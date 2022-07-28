import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class ServerTest {
    private static final int SERVER_PORT = 8080;
    private TestServer server;
    private static final String WS_HTML_CLIENT = Paths.get("src", "test", "resources", "TestWSClient.html").toUri().toString();

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
        try (var client = new java.net.Socket(InetAddress.getLocalHost(), server.getPort());
             var br = new BufferedReader(new InputStreamReader(client.getInputStream()));
             var pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream()), true)) {
            pw.println("ping");
            assertEquals("pong", br.readLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void websocketResponseTest() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless");
        WebDriver driver = new FirefoxDriver(options);
        driver.get(WS_HTML_CLIENT);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("ping"))).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("response"), "pong"));
        driver.quit();
    }
}
