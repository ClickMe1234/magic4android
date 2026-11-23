package io.github.clickme1234.magic4android;

import java.net.*;
import java.nio.charset.StandardCharsets;

/** No discovery broadcast listener: packets are accepted only from the chosen TV endpoint. */
public final class RemoteClient implements AutoCloseable {
    public interface Listener { void event(Protocol.Event event); void status(String status); }
    private volatile boolean running;
    private volatile DatagramSocket socket;
    private Thread worker;
    public synchronized void start(String address, Listener listener) throws Exception {
        close();
        if(!address.matches("(?:\\d{1,3}\\.){3}\\d{1,3}")) throw new IllegalArgumentException("Enter your TV's numeric IPv4 address");
        InetAddress peer=InetAddress.getByName(address);
        if(!peer.isSiteLocalAddress() || peer.isLoopbackAddress()) throw new IllegalArgumentException("Use your TV's private home-network address");
        DatagramSocket active=new DatagramSocket();
        active.setSoTimeout(500);
        socket=active; running=true;
        worker=new Thread(() -> run(active,peer,listener),"Magic4Android-UDP"); worker.start();
    }
    private void send(DatagramSocket active,String wire) throws Exception {
        byte[] data=wire.getBytes(StandardCharsets.UTF_8);
        active.send(new DatagramPacket(data,data.length));
    }
    private void run(DatagramSocket active,InetAddress peer,Listener listener) {
        long lastReceived=0,lastSent=0;
        boolean subscribed=false;
        byte[] buffer=new byte[8193];
        try {
            active.connect(peer,42831);
            listener.status("Connecting to TV…");
            while(running && socket==active) {
                long now=System.nanoTime()/1_000_000;
                if(now-lastSent>=1000) {
                    boolean stale=now-lastReceived>3500;
                    send(active,stale?Protocol.subscribe():"{\"t\":\"keepalive\"}");
                    if(stale && subscribed) {listener.status("TV connection lost; retrying…");subscribed=false;}
                    lastSent=now;
                }
                DatagramPacket packet=new DatagramPacket(buffer,buffer.length);
                try {
                    active.receive(packet);
                    if(packet.getLength()>8192) continue;
                    Protocol.Event event;
                    try {event=Protocol.decode(new String(packet.getData(),0,packet.getLength(),StandardCharsets.UTF_8));}
                    catch(Exception malformed) {continue;}
                    lastReceived=System.nanoTime()/1_000_000;
                    if(!subscribed) {listener.status("Connected to TV");subscribed=true;}
                    listener.event(event);
                } catch(SocketTimeoutException timeout) { /* keepalive tick */ }
                catch(PortUnreachableException unavailable) { Thread.sleep(500); }
            }
        } catch(Exception error) {
            if(running && socket==active) listener.status("Connection error: "+error.getMessage());
        } finally {active.close();}
    }
    public synchronized void close() {
        running=false;
        if(socket!=null) socket.close();
        socket=null;
    }
}
