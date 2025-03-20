import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ProxyClient {
    private ServerSocket serverSocket;
    private final String proxyHost; // Proxy server host
    private final int proxyPort;    // Proxy server port
    private static final int CLIENT_PORT = 8094; // Client listens on this port
    private static final String DEFAULT_PROXY_HOST = "localhost";
    private static final int DEFAULT_PROXY_PORT = 8092;

    // Constructor with proxy host and port parameters
    public ProxyClient(String proxyHost, int proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        try {
            serverSocket = new ServerSocket(CLIENT_PORT);
            System.out.println("Proxy Client started on port " + CLIENT_PORT + 
                              ", forwarding to proxy " + proxyHost + ":" + proxyPort);
        } catch (IOException e) {
            System.err.println("Could not listen on port " + CLIENT_PORT + ": " + e.getMessage());
            System.exit(1);
        }
    }

    // Default constructor for backward compatibility
    public ProxyClient() {
        this(DEFAULT_PROXY_HOST, DEFAULT_PROXY_PORT);
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from: " + clientSocket.getInetAddress());
                new Thread(new RequestHandler(clientSocket)).start();
            } catch (IOException e) {
                System.err.println("Error accepting connection: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        // Use command-line args or defaults
        String proxyHost = args.length > 0 ? args[0] : DEFAULT_PROXY_HOST;
        int proxyPort = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PROXY_PORT;
        ProxyClient client = new ProxyClient(proxyHost, proxyPort);
        client.start();
    }

    class RequestHandler implements Runnable {
        private Socket clientSocket;

        public RequestHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                // Read incoming request
                String requestLine = clientIn.readLine();
                if (requestLine == null || requestLine.isEmpty()) {
                    return;
                }
                System.out.println("Client Request: " + requestLine);

                List<String> headers = new ArrayList<>();
                String line;
                while ((line = clientIn.readLine()) != null && !line.isEmpty()) {
                    headers.add(line);
                    System.out.println("Client Header: " + line);
                }

                // Connect to proxy server
                try (
                    Socket proxySocket = new Socket(proxyHost, proxyPort);
                    PrintWriter proxyOut = new PrintWriter(proxySocket.getOutputStream(), true);
                    BufferedReader proxyIn = new BufferedReader(new InputStreamReader(proxySocket.getInputStream()))
                ) {
                    // Forward request to proxy server
                    proxyOut.println(requestLine);
                    for (String header : headers) {
                        proxyOut.println(header);
                    }
                    proxyOut.println(); // End headers

                    // Forward proxy response back to client
                    String responseLine;
                    while ((responseLine = proxyIn.readLine()) != null) {
                        clientOut.println(responseLine);
                    }
                }

            } catch (IOException e) {
                System.err.println("Error handling request: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }
}