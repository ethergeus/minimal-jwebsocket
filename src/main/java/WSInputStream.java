import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
