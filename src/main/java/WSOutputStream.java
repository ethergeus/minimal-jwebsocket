import java.io.*;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class WSOutputStream extends java.io.OutputStream implements Runnable {
    private boolean upgraded;
    private final Socket socket;
    private final java.io.OutputStream out;
    private final PipedInputStream pipedIn;
    private final PipedOutputStream pipedOut;

    public WSOutputStream(Socket s, OutputStream out) throws IOException {
        this.socket = s;
        this.out = out;
        this.pipedIn = new PipedInputStream();
        this.pipedOut = new PipedOutputStream(pipedIn);
        new Thread(this).start();
    }

    @Override
    public void write(int b) throws IOException {
        pipedOut.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        pipedOut.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        pipedOut.write(b, off, len);
    }

    /*
     * Encoding is done according to the RFC 6455 specification, i.e. the WebSocket Protocol
     * Documentation: https://www.rfc-editor.org/rfc/rfc6455#section-6.1
     * Example: https://stackoverflow.com/questions/8125507/how-can-i-send-and-receive-websocket-messages-on-the-server-side
     */
    private byte[] encodeOutgoingTraffic(String input) {
        int len = input.length();
        int scan = 0;
        /*
         * If len <= 125 the message length can be encoded in a single byte.
         * The most significant bit (128) implies whether the message is encoded or not,
         * this is not mandatory for server-to-client traffic but is for client-to-server communication.
         */
        if (len > 125) {
            if (len > 65535) {
                len = 127;
                scan = 8; // The following 8 bytes contain the message length
            } else {
                len = 126;
                scan = 2; // The following 2 bytes contain the message length
            }
        }
        byte[] encoded = new byte[2 + scan + input.length()];
        encoded[0] = (byte) 129; // 10000001 for a text frame
        encoded[1] = (byte) len; // 0 - 125 for actual message length, 126 for 2 bytes and 127 for 8 bytes
        // Create a set of bytes from an integer and copy said bytes to the encoded byte array
        if (scan > 0) System.arraycopy(ByteBuffer.allocate(scan).putInt(input.length()).array(), 0, encoded, 2, scan);
        // Add the bytes of the actual message to the encoded byte array
        System.arraycopy(input.getBytes(), 0, encoded, 2 + scan, input.length());
        return encoded;
    }

    @Override
    public void run() {
        try (var sc = new Scanner(pipedIn)) {
            String data;
            if ((data = sc.nextLine()).startsWith("HTTP/1.1 101 Switching Protocols")) {
                // Pass complete HTTP/1.1 101 Switching Protocols response, from now on treat connection as websocket
                out.write((data + '\n' + sc.useDelimiter("\\r\\n\\r\\n").next() + "\r\n\r\n").getBytes());
                upgraded = true;
            } else out.write((data + '\n').getBytes());
            while (!socket.isClosed()) {
                data = sc.nextLine();
                out.write(upgraded ? encodeOutgoingTraffic(data) : (data + '\n').getBytes());
            }
        } catch (NoSuchElementException | IOException e) {
            System.out.println("Socket connection closed for " + socket + " -- stopping websocket output stream pre-processor thread");
        }
    }
}
