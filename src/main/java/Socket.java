import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketImpl;

public class Socket extends java.net.Socket {
    private WSInputStream WSInputStream;
    public Socket(SocketImpl socket) throws IOException {
        super(socket);
    }

    public WSInputStream getInputStream() throws IOException {
        return WSInputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        return super.getOutputStream();
    }

    /*
     * InputStream and OutputStream pre-processors are created in this method instead of the constructor because upon
     * creation of the object a connection has not yet been established, this happens during the ImplAccept() call.
     */
    public void createPreProcessors() throws IOException {
        WSInputStream = new WSInputStream(super.getInputStream(), super.getOutputStream());
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
