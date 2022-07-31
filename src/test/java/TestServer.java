import websocket.ServerSocket;
import websocket.Socket;

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
                switch (input) {
                    case "ping":
                        System.out.println("Received ping request -- sending pong response");
                        pw.println("pong");
                        break;
                    default:
                        switch (input.length()) {
                            case 126:
                                System.out.println("Received long request with 2 length bits -- sending back response with 2 length bits");
                                pw.println("b".repeat(126));
                                break;
                            case 65536:
                                System.out.println("Received long request with 8 length bits -- sending back response with 8 length bits");
                                pw.println("b".repeat(65536));
                                break;
                            default:
                                System.out.println(input);
                        }
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("Client disconnected -- quitting");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
