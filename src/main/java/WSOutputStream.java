import java.io.*;
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

    private byte[] encodeMessage() {
        byte[] encoded = new byte[] {};
        return encoded;
    }

    @Override
    public void run() {
        try (var sc = new Scanner(pipedIn); var pw = new PrintWriter(out, true)) {
            String output;
            if ((output = sc.nextLine()).startsWith("HTTP/1.1 101 Switching Protocols")) {
                // Pass complete HTTP/1.1 101 Switching Protocols response, from now on treat connection as websocket
                out.write((output + '\n' + sc.useDelimiter("\\r\\n\\r\\n").next() + "\r\n\r\n").getBytes());
                while (!socket.isClosed()) out.write(encodeMessage());
            } else {
                pw.println(output);
                while (!socket.isClosed()) pw.println(sc.nextLine());
            }
        } catch (NoSuchElementException e) {
            System.out.println("Socket connection closed for " + socket + " -- stopping websocket stream pre-processor thread");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
