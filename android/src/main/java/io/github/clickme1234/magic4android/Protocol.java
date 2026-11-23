package io.github.clickme1234.magic4android;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import org.json.JSONObject;

/** Wire-compatible with upstream's explicit coordinate-only subscription. */
public final class Protocol {
    public record Event(String type, int x, int y, int key, boolean down, int delta) {}
    public static String subscribe() { return "{\"t\":\"sub_sensor\",\"updateFreq\":30,\"filter\":[\"coordinate\"]}"; }
    public static Event decode(String wire) throws Exception {
        if (wire.length() > 8192) throw new IllegalArgumentException("Packet too large");
        JSONObject object = new JSONObject(wire);
        String type = object.getString("t");
        switch (type) {
            case "keepalive": return new Event(type,0,0,0,false,0);
            case "remote_update": {
                byte[] data = Base64.getDecoder().decode(object.getString("payload"));
                if (data.length != 8) throw new IllegalArgumentException("Expected coordinate-only payload");
                ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                return new Event(type,b.getInt(),b.getInt(),0,false,0);
            }
            case "input": {
                JSONObject p = object.getJSONObject("parameters");
                return new Event(type,0,0,p.getInt("keyCode"),p.getBoolean("isDown"),0);
            }
            case "mouse": {
                JSONObject p = object.getJSONObject("mouse");
                String action = p.getString("type");
                if (!action.equals("mousedown") && !action.equals("mouseup")) throw new IllegalArgumentException("Unknown mouse action");
                return new Event(action,p.getInt("x"),p.getInt("y"),0,false,0);
            }
            case "wheel": {
                JSONObject p = object.getJSONObject("wheel");
                return new Event(type,p.getInt("x"),p.getInt("y"),0,false,p.getInt("delta"));
            }
            default: throw new IllegalArgumentException("Unknown message type");
        }
    }
    public static float scale(int coordinate, int source, int destination) {
        if(source < 2 || destination < 2) throw new IllegalArgumentException("Invalid dimensions");
        return (float)Math.max(0,Math.min(destination-1, (double)coordinate*(destination-1)/(source-1)));
    }
}
