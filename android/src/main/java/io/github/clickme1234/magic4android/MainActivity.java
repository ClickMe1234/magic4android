package io.github.clickme1234.magic4android;

import android.app.Activity;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.*;
import android.provider.Settings;
import android.view.Display;
import android.widget.*;

public class MainActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView state;
    private Display[] displays;
    private final Runnable refresh=new Runnable(){public void run(){state.setText(RemoteAccessibilityService.status+"\n"+RemoteAccessibilityService.displayStatus);handler.postDelayed(this,1000);}};
    private EditText field(LinearLayout box,String hint,String value) {
        TextView label=new TextView(this);label.setText(hint);box.addView(label);
        EditText edit=new EditText(this);edit.setHint(hint);edit.setText(value);edit.setSingleLine();box.addView(edit);return edit;
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(32,64,32,32);scroll.addView(box);setContentView(scroll);
        TextView title=new TextView(this);title.setText("Magic4Android · experimental");title.setTextSize(24);box.addView(title);
        TextView help=new TextView(this);help.setText("Open Magic4Android TV on the LG TV and select the phone's HDMI input. Enable this service in Accessibility, then connect to your TV's local IP. The receiver runs on the phone; a PC is not required during use.\n\nPointer = cyan dot; OK/red = click; wheel = scroll; arrows = accessible focus; Back/green = Android Back. Some apps and DeX may reject inputs. This is not a game controller. Stop when finished.\n");box.addView(help);
        var prefs=getPreferences(0);
        EditText ip=field(box,"TV IPv4 address",prefs.getString("tv",""));ip.setInputType( android.text.InputType.TYPE_CLASS_PHONE);
        TextView scaling=new TextView(this);scaling.setText("Pointer scaling follows your selected display automatically, including 4K output.");box.addView(scaling);
        displays=((DisplayManager)getSystemService(DISPLAY_SERVICE)).getDisplays();
        String[] choices=new String[displays.length];for(int i=0;i<displays.length;i++)choices[i]="Display "+displays[i].getDisplayId()+" · "+displays[i].getName();
        Spinner display=new Spinner(this);display.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices));box.addView(display);
        Button access=new Button(this);access.setText("Open Accessibility settings");box.addView(access);access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        Button connect=new Button(this);connect.setText("Connect to TV");box.addView(connect);connect.setOnClickListener(v->{
            try {
                var service=RemoteAccessibilityService.current();
                if(service==null)throw new IllegalStateException("Enable Magic4Android in Accessibility first");
                service.connect(ip.getText().toString().trim(),displays[display.getSelectedItemPosition()].getDisplayId());
                prefs.edit().putString("tv",ip.getText().toString().trim()).remove("sw").remove("sh").apply();
            }catch(Exception error){RemoteAccessibilityService.status=error.toString();}
        });
        Button stop=new Button(this);stop.setText("Stop remote control");box.addView(stop);stop.setOnClickListener(v->{var service=RemoteAccessibilityService.current();if(service!=null)service.stop();});
        state=new TextView(this);state.setTextSize(18);box.addView(state);
    }
    @Override public void onResume(){super.onResume();handler.post(refresh);}
    @Override public void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
