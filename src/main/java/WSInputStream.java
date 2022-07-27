import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WSInputStream extends java.io.InputStream implements Runnable {
    private final Socket socket;
    private final java.io.InputStream in;
    private final OutputStream out;
    private final PipedInputStream pipedIn;
    private final PipedOutputStream pipedOut;
    public WSInputStream(Socket s, java.io.InputStream in, OutputStream out) throws IOException {
        this.socket = s;
        this.in = in;
        this.out = out;
        this.pipedIn = new PipedInputStream();
        this.pipedOut = new PipedOutputStream(pipedIn);
        new Thread(this).start();
    }

    @Override
    public int read() throws IOException {
        return pipedIn.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return pipedIn.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return pipedIn.read(b, off, len);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return pipedIn.readAllBytes();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        return pipedIn.readNBytes(len);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        return pipedIn.readNBytes(b, off, len);
    }

    /*
     * Receive and decode messages sent by client
     * Documentation: https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_a_WebSocket_server_in_Java#decoding_messages
     */
    public String decodeMessage() throws IOException {
        byte[] message;
        int head = in.read(); // First byte gives information on the message itself
        int FIN = (head >> 7) & 1; // Get most significant bit, FIN 1 means this is the whole message
        int opCode = head & (0xf); // Get first 4 bits, Opcode 0x1 means this is a text
        if (FIN != 1 || opCode != 1) throw new IOException(); // TODO: Implement various Opcodes
        int len = in.read() - 128; // Second byte gives information on the length of the message
        if (len > 125) {
            // If the second byte minus 128 is between 0 and 125, this is the length of the message
            byte[] arr;
            int scan = len == 126 ? 2 : 8;
            // If it is 126, the following 2 bytes (16-bit unsigned integer)
            // If 127, the following 8 bytes (64-bit unsigned integer, the most significant bit MUST be 0)
            arr = new byte[scan];
            for (int i = 0; i < scan; i++) arr[i] = (byte) in.read();
            len = ByteBuffer.wrap(arr).getInt();
        }
        byte[] key = new byte[4];
        for (int i = 0; i < 4; i++) key[i] = (byte) in.read();
        message = new byte[len];
        for (int i = 0; i < len; i++) message[i] = (byte) (in.read() ^ key[i & 0x3]);
        System.out.println(new String(message));
        return new String(message);
    }

    @Override
    public void run() {
        try (var sc = new Scanner(in); var pw = new PrintWriter(pipedOut, true)) {
            String input;
            if ((input = sc.next()).startsWith("GET")) {
                // Upon receiving a GET request, attempt to upgrade the connection to type websocket
                String data = sc.useDelimiter("\\r\\n\\r\\n").next();
                Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
                match.find();
                byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Sec-WebSocket-Accept: "
                        + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)))
                        + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
                System.out.println("Upgrading socket connection for " + socket + " -- switching protocols to websocket");
                out.write(response, 0, response.length);
                while (!socket.isClosed()) {
                    pw.println(decodeMessage());
                }
            } else {
                // Regular socket connection, replace piped input stream with regular input and stop thread
                System.out.println("Websocket handshake did not occur -- not upgrading " + socket + " to websocket connection");
                pw.println(input);
                while (!socket.isClosed()) {
                    if ((input = sc.next()) == null) break;
                    pw.println(input);
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("Socket connection closed for " + socket + " -- stopping websocket stream pre-processor thread");
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
