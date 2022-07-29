import java.io.*;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class WSOutputStream extends java.io.OutputStream implements Runnable {
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
        int lenBit = len;
        int numLenBits = 0;
        /*
         * If len <= 125 the message length can be encoded in a single byte.
         * The most significant bit (128) implies whether the message is encoded or not,
         * this is not mandatory for server-to-client traffic but is for client-to-server communication.
         */
        if (lenBit > 125) {
            if (lenBit > 65535) {
                lenBit = 127;
                numLenBits = 8; // The following 8 bytes contain the message length
            } else {
                lenBit = 126;
                numLenBits = 2; // The following 2 bytes contain the message length
            }
        }
        byte[] encoded = new byte[2 + numLenBits + input.length()];
        if (lenBit > 125) {
            // Create a set of bytes from an integer and copy said bytes to the encoded byte array
            ByteBuffer bb = ByteBuffer.allocate(numLenBits);
            if (lenBit == 126) bb.putShort((short) len); else bb.putLong(len);
            System.arraycopy(bb.array(), 0, encoded, 2, numLenBits);
        }
        encoded[0] = (byte) 129; // 10000001 for a text frame
        encoded[1] = (byte) lenBit; // 0 - 125 for actual message length, 126 for 2 bytes and 127 for 8 bytes
        // Add the bytes of the actual message to the encoded byte array
        System.arraycopy(input.getBytes(), 0, encoded, 2 + numLenBits, len);
        return encoded;
    }

    @Override
    public void run() {
        try (var sc = new Scanner(pipedIn)) {
            String data;
            // If receiving HTTP/1.1 101 Switching Protocols, pass complete request
            if ((data = sc.nextLine()).startsWith("HTTP/1.1 101 Switching Protocols"))
                out.write((data + '\n' + sc.useDelimiter("\\r\\n\\r\\n").next() + "\r\n\r\n").getBytes());
            else out.write((data + '\n').getBytes());
            while (!socket.isClosed()) out.write(socket.isWebSocketConnection() ? encodeOutgoingTraffic(sc.nextLine()) : (sc.nextLine() + '\n').getBytes());
        } catch (NoSuchElementException | IOException ignored) { /* Client disconnected */ }
    }
}
