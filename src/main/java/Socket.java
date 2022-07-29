import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketImpl;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Socket extends java.net.Socket {
    private boolean isWebSocketConnection;
    private WSInputStream in;
    private WSOutputStream out;
    public Socket(SocketImpl socket) throws IOException {
        super(socket);
    }

    public WSInputStream getInputStream() throws IOException {
        return in;
    }

    public OutputStream getOutputStream() throws IOException {
        return out;
    }

    /*
     * InputStream and OutputStream pre-processors are created in this method instead of the constructor because upon
     * creation of the object a connection has not yet been established, this happens during the ImplAccept() call.
     */
    public void createPreProcessors() throws IOException {
        in = new WSInputStream(this, super.getInputStream());
        out = new WSOutputStream(this, super.getOutputStream());
    }

    @Override
    public String toString() {
        return (isWebSocketConnection ? "Web" : "") + super.toString();
    }

    public void upgradeWebsocket(String key) throws NoSuchAlgorithmException, IOException {
        byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                + "Connection: Upgrade\r\n"
                + "Upgrade: websocket\r\n"
                + "Sec-WebSocket-Accept: "
                + Base64.getEncoder().encodeToString(
                        MessageDigest.getInstance("SHA-1").digest(
                                (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)))
                + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        out.write(response);
        isWebSocketConnection = true;
    }

    public boolean isWebSocketConnection() {
        return isWebSocketConnection;
    }
}
