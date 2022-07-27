import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class ServerTest {
    private static final int SERVER_PORT = 8080;
    private TestServer server;
    private static final String WS_HTML_CLIENT = Paths.get("src", "test", "java", "TestWSClient.html").toUri().toString();

    public static void main(String[] args) {
        TestServer server = new TestServer();
        server.connect(SERVER_PORT);
        server.start();
        WebDriver driver = new ChromeDriver();
        driver.get(WS_HTML_CLIENT);
    }

    @BeforeEach
    public void init() {
        server = new TestServer();
        server.connect(SERVER_PORT);
        server.start();
    }

    @AfterEach
    public void stop() throws IOException {
        server.stop();
    }

    /*
     * Test basic communication between the server and a mock client through the standard java.net.Socket class.
     */
    @Test
    public void backwardsCompatibleResponseTest() {
        String output;
        try (var client = new java.net.Socket(InetAddress.getLocalHost(), server.getPort());
             var br = new BufferedReader(new InputStreamReader(client.getInputStream()));
             var pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream()), true)) {
            pw.println("ping");
            output = br.readLine();
            System.out.println("Client received: " + output);
            assertEquals("pong", output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void websocketResponseTest() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);
        LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
        driver.get(WS_HTML_CLIENT);
        driver.findElement(By.id("ping")).click();
        assertEquals("pong", driver.findElement(By.id("response")).getText());
        driver.quit();
    }
}
