package io.github.clickme1234.magic4android;

import android.accessibilityservice.*;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.display.DisplayManager;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.accessibility.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class RemoteAccessibilityService extends AccessibilityService {
    private static volatile WeakReference<RemoteAccessibilityService> current=new WeakReference<>(null);
    public static RemoteAccessibilityService current() {return current.get();}
    public static volatile String status="Enable Accessibility access to connect";
    public static volatile String displayStatus="";
    private final Handler main=new Handler(Looper.getMainLooper());
    private final RemoteClient client=new RemoteClient();
    private int displayId,sourceWidth=1920,sourceHeight=1080,width,height;
    private float x,y;
    private WindowManager manager;
    private View pointer;
    private WindowManager.LayoutParams layout;
    private boolean gestureBusy;
    private long lastClick;
    private volatile int generation;
    private boolean directionalMode;
    private AccessibilityNodeInfo navigationFocus;
    private DisplayManager displayManager;
    private String targetDisplayName;
    private boolean trackingDisplay,externalTarget,hasSourcePosition;
    private int lastSourceX,lastSourceY;
    private int lastActivityX,lastActivityY;
    private boolean hasActivityPosition;
    private final Runnable hideIdlePointer=()->{if(pointer!=null)pointer.setVisibility(View.INVISIBLE);};
    private void pointerActivity() {
        if(pointer==null)return;
        pointer.setVisibility(View.VISIBLE);
        main.removeCallbacks(hideIdlePointer);
        main.postDelayed(hideIdlePointer,5000);
    }
    private final DisplayManager.DisplayListener displayListener=new DisplayManager.DisplayListener() {
        public void onDisplayAdded(int id){refreshTargetDisplay();}
        public void onDisplayRemoved(int id){if(id==displayId){removePointer();refreshTargetDisplay();}}
        public void onDisplayChanged(int id){if(id==displayId || pointer==null)refreshTargetDisplay();}
    };

    @Override public void onServiceConnected() { current=new WeakReference<>(this);status="Ready — choose TV and display, then Connect"; }
    public void connect(String tv,int targetDisplay) throws Exception {
        stop();
        displayManager=(DisplayManager)getSystemService(DISPLAY_SERVICE);
        Display display=displayManager.getDisplay(targetDisplay);
        if(display==null) throw new IllegalArgumentException("Selected display is disconnected");
        // webOS Magic4PC uses a 1920x1080 UI/sensor space, independent of HDMI output.
        sourceWidth=1920;sourceHeight=1080;
        targetDisplayName=display.getName();externalTarget=targetDisplay!=Display.DEFAULT_DISPLAY;
        attachDisplay(display);
        trackingDisplay=true;displayManager.registerDisplayListener(displayListener,main);
        final int session=generation;
        try {
            client.start(tv,new RemoteClient.Listener() {
                public void event(Protocol.Event e) {main.post(() -> {if(session==generation)handle(e);});}
                public void status(String s) {if(session==generation){status=s;android.util.Log.i("Magic4Android",s);}}
            });
        } catch(Exception error) {stop();throw error;}
    }
    public void stop() {
        trackingDisplay=false;
        if(displayManager!=null)displayManager.unregisterDisplayListener(displayListener);
        generation++;client.close();main.removeCallbacksAndMessages(null);gestureBusy=false;pointerMode();
        removePointer();hasSourcePosition=false;hasActivityPosition=false;displayStatus="";status="Stopped";
    }
    private void removePointer() {
        main.removeCallbacks(hideIdlePointer);
        if(pointer!=null){try{manager.removeView(pointer);}catch(Exception ignored){}pointer=null;}
        navigationFocus=null;gestureBusy=false;
    }
    private void attachDisplay(Display display) {
        removePointer();displayId=display.getDisplayId();width=0;height=0;
        Context context=createDisplayContext(display);
        manager=(WindowManager)context.getSystemService(WINDOW_SERVICE);
        pointer=new View(context) {
            private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);paint.setColor(Color.CYAN);
                canvas.drawCircle(getWidth()/2f,getHeight()/2f,Math.min(getWidth(),getHeight())/2f-4,paint);
            }
        };
        layout=new WindowManager.LayoutParams(24,24,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,android.graphics.PixelFormat.TRANSLUCENT);
        layout.gravity=Gravity.TOP|Gravity.LEFT;manager.addView(pointer,layout);
        updateOutputSize(display);
        pointerActivity();
    }
    private void updateOutputSize(Display display) {
        DisplayMetrics metrics=new DisplayMetrics();display.getRealMetrics(metrics);
        if(metrics.widthPixels<2 || metrics.heightPixels<2)return;
        if(width==metrics.widthPixels && height==metrics.heightPixels)return;
        width=metrics.widthPixels;height=metrics.heightPixels;pointerMode();
        displayStatus="Output: "+width+" × "+height;
        android.util.Log.i("Magic4Android","Coordinate mapping: "+sourceWidth+"x"+sourceHeight+" -> "+width+"x"+height+" on display "+displayId);
        if(hasSourcePosition)position(lastSourceX,lastSourceY);
    }
    private void refreshTargetDisplay() {
        if(!trackingDisplay)return;
        Display display=displayManager.getDisplay(displayId);
        if(display!=null && pointer!=null){updateOutputSize(display);return;}
        // DeX may replace the logical display when changing HDMI modes. Rebind
        // only if exactly one display matches the previously selected target.
        Display replacement=null;
        for(Display candidate:displayManager.getDisplays()) {
            if((candidate.getDisplayId()!=Display.DEFAULT_DISPLAY)==externalTarget && candidate.getName().equals(targetDisplayName)) {
                if(replacement!=null){displayStatus="Multiple matching displays — reconnect to choose";return;}
                replacement=candidate;
            }
        }
        if(replacement==null){displayStatus="Waiting for TV display";return;}
        try{attachDisplay(replacement);}catch(Exception error){removePointer();displayStatus="Display reconnect failed: "+error.getClass().getSimpleName();}
    }
    private void position(int sx,int sy) {
        lastSourceX=sx;lastSourceY=sy;hasSourcePosition=true;
        x=Protocol.scale(sx,sourceWidth,width);y=Protocol.scale(sy,sourceHeight,height);
        updatePointerLayout();
    }
    private void updatePointerLayout() {
        if(pointer==null)return;
        // Centre the indicator on the touch position and keep its full canvas
        // inside the screen at the edges. Click coordinates remain unchanged.
        layout.x=Math.max(0,Math.min(width-layout.width,Math.round(x)-layout.width/2));
        layout.y=Math.max(0,Math.min(height-layout.height,Math.round(y)-layout.height/2));
        manager.updateViewLayout(pointer,layout);
    }
    private void gesture(float toY,long duration) {
        if(gestureBusy) return;
        Path path=new Path();path.moveTo(x,y);
        if(toY!=y) path.lineTo(x,Math.max(0,Math.min(height-1,toY)));
        GestureDescription g=new GestureDescription.Builder().setDisplayId(displayId)
            .addStroke(new GestureDescription.StrokeDescription(path,0,duration)).build();
        gestureBusy=true;
        boolean accepted=dispatchGesture(g,new GestureResultCallback(){
            public void onCompleted(GestureDescription g){gestureBusy=false;}
            public void onCancelled(GestureDescription g){gestureBusy=false;}
        },main);
        if(!accepted) {gestureBusy=false;status="Android rejected gesture on display "+displayId;}
    }
    private void click() {
        long now=SystemClock.uptimeMillis();if(now-lastClick<250)return;lastClick=now;gesture(y,70);
    }
    private AccessibilityNodeInfo root() {
        var windows=getWindowsOnAllDisplays().get(displayId);
        if(windows==null)return null;
        AccessibilityWindowInfo best=null;
        for(AccessibilityWindowInfo window:windows) {
            if(window.getDisplayId()!=displayId || window.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION || window.getRoot()==null)continue;
            // DeX exposes a separate, unnamed caption-bar window above app content.
            boolean named=window.getTitle()!=null && window.getTitle().length()>0;
            boolean bestNamed=best!=null && best.getTitle()!=null && best.getTitle().length()>0;
            if(best==null || (window.isFocused() && !best.isFocused()) ||
                (window.isFocused()==best.isFocused() && (named && !bestNamed || named==bestNamed && window.getLayer()>best.getLayer())))best=window;
        }
        return best==null?null:best.getRoot();
    }
    private Rect bounds(AccessibilityNodeInfo node) {Rect r=new Rect();node.getBoundsInScreen(r);return r;}
    private FocusNavigator.Box box(AccessibilityNodeInfo node) {Rect r=bounds(node);return new FocusNavigator.Box(r.left,r.top,r.right,r.bottom);}
    private boolean usable(AccessibilityNodeInfo node) {return node!=null && node.isVisibleToUser() && node.isEnabled() && (node.isFocusable() || node.isClickable()) && !bounds(node).isEmpty();}
    private void collect(AccessibilityNodeInfo node,ArrayList<AccessibilityNodeInfo> choices,int[] budget) {
        if(node==null || --budget[0]<0)return;
        int before=choices.size();
        for(int i=0;i<node.getChildCount() && budget[0]>0;i++)collect(node.getChild(i),choices,budget);
        if(choices.size()==before && usable(node))choices.add(node);
    }
    private boolean focus(AccessibilityNodeInfo node) {
        if(!usable(node))return false;
        boolean accepted=node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        if(!accepted)accepted=node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
        if(accepted) {
            navigationFocus=node;
            Rect r=bounds(node);x=Math.max(0,Math.min(width-1,r.exactCenterX()));y=Math.max(0,Math.min(height-1,r.exactCenterY()));
            updatePointerLayout();
            android.util.Log.i("Magic4Android","Directional focus on display "+displayId+": "+r);
        }
        return accepted;
    }
    private void navigate(int key) {
        AccessibilityNodeInfo root=root();if(root==null){status="No accessible app on selected display";return;}
        // Some virtual controls leave input focus on their enclosing list. Retain
        // the selected control so subsequent arrows and OK act on that item.
        AccessibilityNodeInfo focused=navigationFocus;
        if(focused!=null && (focused.getWindowId()!=root.getWindowId() || !focused.refresh() || !usable(focused)))focused=null;
        if(focused==null)focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if(focused==null)focused=root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        if(key==13) {
            if(focused==null || !focused.performAction(AccessibilityNodeInfo.ACTION_CLICK))click();
            return;
        }
        int direction=switch(key){case 37->View.FOCUS_LEFT;case 38->View.FOCUS_UP;case 39->View.FOCUS_RIGHT;default->View.FOCUS_DOWN;};
        ArrayList<AccessibilityNodeInfo> choices=new ArrayList<>();collect(root,choices,new int[]{600});
        AccessibilityNodeInfo next=focused==null?null:focused.focusSearch(direction);
        if(next!=null && !next.equals(focused) && choices.contains(next) && focus(next))return;
        ArrayList<FocusNavigator.Box> boxes=new ArrayList<>();for(var node:choices)boxes.add(box(node));
        boolean hasOrigin=usable(focused) && !focused.equals(root) && !bounds(focused).equals(bounds(root));
        int selected=hasOrigin?FocusNavigator.next(boxes,box(focused),key):FocusNavigator.nearest(boxes,x,y);
        android.util.Log.i("Magic4Android","Navigation candidates="+choices.size()+" origin="+(focused==null?"none":bounds(focused))+" selected="+selected);
        if(selected>=0 && focus(choices.get(selected)))return;
        // At a scroll boundary let the app expose the next page of controls.
        int action=(key==37 || key==38)?AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:AccessibilityNodeInfo.ACTION_SCROLL_FORWARD;
        ArrayList<AccessibilityNodeInfo> pending=new ArrayList<>();pending.add(root);
        for(int i=0;i<pending.size() && i<600;i++) {
            var node=pending.get(i);if(node.isScrollable() && node.performAction(action))return;
            for(int c=0;c<node.getChildCount() && pending.size()<600;c++){var child=node.getChild(c);if(child!=null)pending.add(child);}
        }
        status="No further accessible control in that direction";
    }
    private void back() {
        var windows=getWindowsOnAllDisplays().get(displayId);
        boolean focused=false;
        if(windows!=null)for(var window:windows)if(window.isFocused()){focused=true;break;}
        if(!focused){status="Select an app on the TV with the pointer before using Back";return;}
        boolean accepted=performGlobalAction(GLOBAL_ACTION_BACK);
        navigationFocus=null;
        android.util.Log.i("Magic4Android","Back accepted="+accepted+" on display "+displayId);
        if(!accepted)status="Android rejected Back";
    }
    private void pointerMode(){directionalMode=false;navigationFocus=null;}
    private void handle(Protocol.Event e) {
        if(pointer==null){refreshTargetDisplay();if(pointer==null)return;}
        try {
            switch(e.type()) {
                case "remote_update":
                    if(!directionalMode) {
                        // Repeated sensor samples/keepalives are not activity.
                        // Anchor at the last meaningful move to ignore one-pixel jitter.
                        if(!hasActivityPosition || Math.abs(e.x()-lastActivityX)>=2 || Math.abs(e.y()-lastActivityY)>=2) {
                            lastActivityX=e.x();lastActivityY=e.y();hasActivityPosition=true;pointerActivity();
                        }
                        position(e.x(),e.y());
                    }
                    break;
                case "mousedown": pointerMode();position(e.x(),e.y());pointerActivity();break;
                case "mouseup": pointerMode();position(e.x(),e.y());pointerActivity();click();break;
                case "wheel": pointerMode();position(e.x(),e.y());pointerActivity();gesture(y+(e.delta()>0?height*.25f:-height*.25f),200);break;
                case "input":
                    if(!e.down())break;
                    if(e.key()==1536 || e.key()==13 || e.key()==403 || e.key()==404 || e.key()==461 || (e.key()>=37 && e.key()<=40))pointerActivity();
                    android.util.Log.i("Magic4Android","Remote key "+e.key()+" for display "+displayId);
                    if(e.key()==1536)pointerMode();
                    else if(e.key()==461 || e.key()==404)back();
                    else if(e.key()==403){pointerMode();click();}
                    else if(e.key()==13){if(directionalMode)navigate(13);else click();}
                    else if(e.key()>=37 && e.key()<=40){directionalMode=true;navigate(e.key());}
                    break;
            }
        } catch(Exception error){status="Input rejected: "+error.getMessage();}
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {stop();}
    @Override public void onDestroy() {stop();if(current.get()==this)current.clear();super.onDestroy();}
}
