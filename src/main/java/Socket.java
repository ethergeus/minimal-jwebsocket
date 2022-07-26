import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketImpl;

public class Socket extends java.net.Socket {
    public Socket(SocketImpl socket) throws IOException {
        super(socket);
    }

    public InputStream getInputStream() throws IOException {
        return super.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return super.getOutputStream();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
