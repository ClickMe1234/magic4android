package io.github.clickme1234.magic4android;

import android.accessibilityservice.*;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.accessibility.*;
import android.widget.TextView;
import java.lang.ref.WeakReference;

public class RemoteAccessibilityService extends AccessibilityService {
    private static volatile WeakReference<RemoteAccessibilityService> current=new WeakReference<>(null);
    public static RemoteAccessibilityService current() {return current.get();}
    public static volatile String status="Enable Accessibility access to connect";
    private final Handler main=new Handler(Looper.getMainLooper());
    private final RemoteClient client=new RemoteClient();
    private int displayId,sourceWidth=1920,sourceHeight=1080,width,height;
    private float x,y;
    private WindowManager manager;
    private TextView pointer;
    private WindowManager.LayoutParams layout;
    private boolean gestureBusy;
    private long lastClick;
    private volatile int generation;

    @Override public void onServiceConnected() { current=new WeakReference<>(this);status="Ready — choose TV and display, then Connect"; }
    public void connect(String tv,int targetDisplay,int tvWidth,int tvHeight) throws Exception {
        stop();
        Display display=((DisplayManager)getSystemService(DISPLAY_SERVICE)).getDisplay(targetDisplay);
        if(display==null) throw new IllegalArgumentException("Selected display is disconnected");
        if(tvWidth<2 || tvHeight<2) throw new IllegalArgumentException("Invalid TV coordinate size");
        displayId=targetDisplay;sourceWidth=tvWidth;sourceHeight=tvHeight;
        DisplayMetrics metrics=new DisplayMetrics();display.getRealMetrics(metrics);width=metrics.widthPixels;height=metrics.heightPixels;
        Context context=createDisplayContext(display);
        manager=(WindowManager)context.getSystemService(WINDOW_SERVICE);
        pointer=new TextView(context);pointer.setText("●");pointer.setTextSize(22);pointer.setTextColor(Color.CYAN);
        layout=new WindowManager.LayoutParams(40,40,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,android.graphics.PixelFormat.TRANSLUCENT);
        layout.gravity=Gravity.TOP|Gravity.LEFT;
        manager.addView(pointer,layout);
        final int session=generation;
        try {
            client.start(tv,new RemoteClient.Listener() {
                public void event(Protocol.Event e) {main.post(() -> {if(session==generation)handle(e);});}
                public void status(String s) {if(session==generation){status=s;android.util.Log.i("Magic4Android",s);}}
            });
        } catch(Exception error) {stop();throw error;}
    }
    public void stop() {
        generation++;client.close();main.removeCallbacksAndMessages(null);gestureBusy=false;
        if(pointer!=null) {try{manager.removeView(pointer);}catch(Exception ignored){}pointer=null;}
        status="Stopped";
    }
    private void position(int sx,int sy) {
        x=Protocol.scale(sx,sourceWidth,width);y=Protocol.scale(sy,sourceHeight,height);
        if(pointer!=null) {layout.x=(int)x;layout.y=(int)y;manager.updateViewLayout(pointer,layout);}
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
        for(AccessibilityWindowInfo window:windows) {
            if(window.getDisplayId()==displayId && window.getType()==AccessibilityWindowInfo.TYPE_APPLICATION && window.getRoot()!=null) return window.getRoot();
        }
        return null;
    }
    private void navigate(int key) {
        AccessibilityNodeInfo root=root();if(root==null){status="No accessible app on selected display";return;}
        AccessibilityNodeInfo focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if(focused==null)focused=root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        if(key==13) {
            if(focused==null || !focused.performAction(AccessibilityNodeInfo.ACTION_CLICK))click();
            return;
        }
        int direction=switch(key){case 37->View.FOCUS_LEFT;case 38->View.FOCUS_UP;case 39->View.FOCUS_RIGHT;default->View.FOCUS_DOWN;};
        AccessibilityNodeInfo next=focused==null?null:focused.focusSearch(direction);
        if(next!=null) {if(!next.performAction(AccessibilityNodeInfo.ACTION_FOCUS))next.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);}
        else status="App has no directional focus target; use pointer/wheel";
    }
    private void handle(Protocol.Event e) {
        if(pointer==null)return;
        try {
            switch(e.type()) {
                case "remote_update": position(e.x(),e.y());break;
                case "mouseup": position(e.x(),e.y());click();break;
                case "wheel": position(e.x(),e.y());gesture(y+(e.delta()>0?height*.25f:-height*.25f),200);break;
                case "input":
                    if(!e.down())break;
                    if(e.key()==461 || e.key()==404)performGlobalAction(GLOBAL_ACTION_BACK);
                    else if(e.key()==403)click();
                    else if(e.key()==13 || (e.key()>=37 && e.key()<=40))navigate(e.key());
                    break;
            }
        } catch(Exception error){status="Input rejected: "+error.getMessage();}
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {stop();}
    @Override public void onDestroy() {stop();if(current.get()==this)current.clear();super.onDestroy();}
}
