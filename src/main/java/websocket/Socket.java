package websocket;

import websocket.preprocessors.WSInputStream;
import websocket.preprocessors.WSOutputStream;

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
        in = new WSInputStream(this, super.getInputStream()); // Create pre-processor thread for the input stream
        out = new WSOutputStream(this, super.getOutputStream()); // Create pre-processor thread for the output stream
    }

    @Override
    public String toString() {
        return (isWebSocketConnection ? "Web" : "") + super.toString();
    }

    /*
     * Upgrade the current socket to a websocket connection by sending an HTTP/1.1 101 Switching Protocol response.
     * Documentation: https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_a_WebSocket_server_in_Java#handshaking
     */
    public void upgradeWebsocket(String key) throws NoSuchAlgorithmException, IOException {
        // Build response
        byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                + "Connection: Upgrade\r\n"
                + "Upgrade: websocket\r\n"
                + "Sec-WebSocket-Accept: "
                + Base64.getEncoder().encodeToString(
                        MessageDigest.getInstance("SHA-1").digest(
                                (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)))
                + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        out.write(response); // Send response
        isWebSocketConnection = true; // Treat this connection as upgraded
    }

    /*
     * Returns whether the current socket has been upgraded to a websocket connection,
     * implies the pre-processors should encode and decode the raw streams.
     */
    public boolean isWebSocketConnection() {
        return isWebSocketConnection;
    }
}
