import java.io.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class TestServer implements Runnable {
    private static final int SERVER_PORT = 8080;
    private ServerSocket socket;

    public static void main(String[] args) {
        TestServer server = new TestServer();
        server.connect(SERVER_PORT);
        server.start();
    }

    public void connect(int port) {
        try {
            this.socket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        new Thread(this).start();
    }

    public void stop() throws IOException {
        socket.close();
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    @Override
    public void run() {
        try (Socket client = socket.accept();
             var br = new BufferedReader(new InputStreamReader(client.getInputStream()));
             var pw = new PrintWriter(client.getOutputStream(), true);
             var sc = new Scanner(br)) {
            String input;
            while (!socket.isClosed()) {
                if ((input = sc.next()) == null) break;
                System.out.println("Server received: " + input);
                switch (input) {
                    case "ping" -> pw.println("pong");
                }
            }
            this.stop();
        } catch (NoSuchElementException e) {
            System.out.println("Client disconnected");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            this.stop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
