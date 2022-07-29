# minimal-jwebsocket
Minimal Java library for communication between server and web browser, designed as a drop-in replacement for the `java.net` `ServerSocket` and `Socket` classes

## Features
- Drop-in replacement for `java.net` `ServerSocket` and `Socket` classes, able to interact with regular java.net Socket clients and web browsers
- Automatic HTTP Upgrade handling to WebSocket protocol upon web browser handshake
- Automatic encoding and decoding of traffic between server and web browser client after WebSocket handshaking with the help of background processes

## TODO
- Implement various [Opcodes](https://datatracker.ietf.org/doc/html/rfc6455#section-5.2) for frames other than text
- Implement sending data in individual frames instead of all at once
- Encode traffic from server to client (only encoding from client to server is mandatory)

## Usage

### Server
Using the library to communicate with web browsers is designed to be as straight-forward as interacting normal `java.net` `Socket` clients. Below is a simple example server allowing only once consecutive client to connect at a time, to handle more than one client at a time implement a client handler class with a thread for every client.

```java
import com.antonowycz.ServerSocket; // Extension of java.net.ServerSocket
import com.antonowycz.Socket; // Extension of java.net.Socket
...
public class TestServer implements Runnable {
    ...
    @Override
    public void run() {
        while (!socket.isClosed()) {
            // Accept a client
            try (Socket client = socket.accept();
                var br = new BufferedReader(new InputStreamReader(client.getInputStream()));
                var pw = new PrintWriter(client.getOutputStream(), true);
                var sc = new Scanner(br)) {
                // After creating a buffered reader, scanner and printerwriter for the client
                while (!socket.isClosed()) {
                    String input;
                    if ((input = sc.next()) == null) break; // Read input from scanner
                    switch (input) {
                        case "ping":
                            pw.println("pong"); // Write output to printwriter
                            break;
                        default:
                            System.out.println("Received: " + input);
                    }
                }
            } catch (NoSuchElementException e) {
                System.out.println("Client disconnected"); // Scanner is empty -- client disconnected
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    ...
}
```

### Client
The code above works both when the client on the receiving end is a regular `java.net.Socket` class, or a web browser with the [RFC 6455](https://datatracker.ietf.org/doc/html/rfc6455) protocol implemented, e.g. Chrome 16+, Firefox 11+, IE 10+.

#### Java Socket
The following unit test from `src/test/java/ServerTest.class` ensures backwards compatibility between the extended `Socket` class and the `java.net.Socket` class:

```java
@Test
public void backwardsCompatibleResponseTest() {
    try (var client = new java.net.Socket(InetAddress.getLocalHost(), server.getPort());
            var br = new BufferedReader(new InputStreamReader(client.getInputStream()));
            var pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream()), true)) {
        pw.println("ping");
        assertEquals("pong", br.readLine());
        pw.println("a".repeat(126));
        assertEquals("b".repeat(126), br.readLine());
        pw.println("a".repeat(65536));
        assertEquals("b".repeat(65536), br.readLine());
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}
```

#### Web Browser
The web browser communication is tested through the Selenium UI testing toolkit, using the following web page:

```html
<p id="response"></p>
<button class="websocket-communication disabled" id="ping" onclick="ws.send('ping');">ping</button>
<button class="websocket-communication disabled" id="long2b" onclick="ws.send('a'.repeat(126));">long2b</button>
<button class="websocket-communication disabled" id="long8b" onclick="ws.send('a'.repeat(65536));">long8b</button>
<script>
    function displayOutput(output) {
        document.getElementById('response').innerHTML = output;
    }
    const ws = new WebSocket('ws://localhost:8080');
    ws.onmessage = function(message) {
        displayOutput(message.data);
        switch (message.data) {
            case 'ping': ws.send('pong'); break;
        }
    }
    ws.onopen = function() {
        Array.from(document.querySelectorAll('.websocket-communication.disabled')).forEach((el) => el.classList.remove('disabled'));
    }
</script>
```

The following unit test from `src/test/ServerTest.class` ensures the handshake and bidirectional communication between the web server and server are working correctly:

```java
@Test
public void websocketResponseTest() throws InterruptedException {
    Thread.sleep(2000);
    FirefoxOptions options = new FirefoxOptions();
    options.addArguments("--headless");
    WebDriver driver = new FirefoxDriver(options);
    driver.get(WS_HTML_CLIENT);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
    wait.until(ExpectedConditions.elementToBeClickable(By.id("ping"))).click();
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("response"), "pong"));
    driver.findElement(By.id("long2b")).click();
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("response"), "b".repeat(126)));
    driver.findElement(By.id("long8b")).click();
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("response"), "b".repeat(65536)));
    driver.quit();
}
```