import java.io.*;
import java.net.*;

public class ProxyClient {
    private static final String PROXY_HOST = "localhost";
    private static final int PROXY_PORT = 8084;

    public static void sendRequest(String request) {
        try (
            Socket socket = new Socket(PROXY_HOST, PROXY_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))
        ) {
            // Send HTTP request
            out.println(request);
            out.println("Host: example.com");
            out.println("request 1");
            out.println("request 2");
            out.println("request 3");
            out.println("Connection: close");
            out.println(); // Empty line to indicate end of headers

            // Read and print response
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            System.err.println("Error in client: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Example requests to be processed one by one
        String[] requests = {
            "GET / HTTP/1.1",
            "GET /about HTTP/1.1",
            "GET /contact HTTP/1.1"
        };

        // Send requests sequentially
        for (String request : requests) {
            System.out.println("\nSending request: " + request);
            sendRequest(request);
            try {
                Thread.sleep(1000); // Wait between requests
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}