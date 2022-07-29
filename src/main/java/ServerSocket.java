import java.io.IOException;
import java.net.SocketException;
import java.net.SocketImpl;

public class ServerSocket extends java.net.ServerSocket {
    /*
     * The ServerSocket class of the minimal-jwebsocket packages provides a method to interact with browser clients
     * whilst being backwards compatible with the java.net ServerSocket implementation.
     */
    public ServerSocket(int port) throws IOException {
        super(port);
    }

    /*
     * Accept a new connection to the current ServerSocket object.
     * Adapted from java.net.ServerSocket to use the local Socket class instead of java.net.Socket
     */
    @Override
    public Socket accept() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (!isBound())
            throw new SocketException("Socket is not bound yet");
        Socket s = new Socket((SocketImpl) null);
        implAccept(s);
        return s;
    }

    /*
     * Call the java.net.Socket.implAccept() method to create input and output stream, then create pre-processors for
     * both streams which will handle the encoding and decoding for when the connection is upgraded to type websocket.
     */
    protected final void implAccept(Socket s) throws IOException {
        super.implAccept(s);
        s.createPreProcessors();
    }
}
