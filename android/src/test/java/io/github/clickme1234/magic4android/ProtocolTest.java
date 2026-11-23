package io.github.clickme1234.magic4android;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.*;
import java.util.Base64;
public class ProtocolTest {
    @Test public void coordinatesUseLittleEndianAndExactSubscriptionLayout() throws Exception {
        byte[] bytes=ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(1280).putInt(720).array();
        var event=Protocol.decode("{\"t\":\"remote_update\",\"payload\":\""+Base64.getEncoder().encodeToString(bytes)+"\"}");
        assertEquals(1280,event.x());assertEquals(720,event.y());
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsWrongSensorLayout() throws Exception {Protocol.decode("{\"t\":\"remote_update\",\"payload\":\"AAAAAA==\"}");}
    @Test public void keyReleaseIsNotKeyPress() throws Exception {assertFalse(Protocol.decode("{\"t\":\"input\",\"parameters\":{\"keyCode\":461,\"isDown\":false}}").down());}
    @Test public void scalesEndpointsAndClamps() {assertEquals(2559,Protocol.scale(1919,1920,2560),0);assertEquals(0,Protocol.scale(-9,1920,2560),0);assertEquals(2559,Protocol.scale(9000,1920,2560),0);}
    @Test(expected=IllegalArgumentException.class) public void rejectsUnsupportedPacket() throws Exception {Protocol.decode("{\"t\":\"command\",\"shell\":\"anything\"}");}
    @Test public void wheelPreservesDirection() throws Exception {assertEquals(-120,Protocol.decode("{\"t\":\"wheel\",\"wheel\":{\"x\":12,\"y\":34,\"delta\":-120}}").delta());}
}
