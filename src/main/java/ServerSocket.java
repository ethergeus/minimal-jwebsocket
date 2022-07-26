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

    @Override
    public Socket accept() throws IOException {
        /*
         * Source material from java.net.ServerSocket, adapted to use the local Socket class instead of java.net.Socket
         */
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (!isBound())
            throw new SocketException("Socket is not bound yet");
        Socket s = new Socket((SocketImpl) null);
        implAccept(s);
        return s;
    }
}
