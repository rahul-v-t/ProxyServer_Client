import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.ArrayList;
import java.util.List;

public class ProxyServer {
    private ServerSocket serverSocket;
    private LinkedBlockingQueue<Socket> requestQueue = new LinkedBlockingQueue<>();
    private static final int PORT = 8092;

    public ProxyServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Proxy Server started on port " + PORT);
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + PORT + ": " + e.getMessage());
            System.exit(1);
        }
    }

    public void start() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Accepted connection from: " + clientSocket.getInetAddress());
                    requestQueue.put(clientSocket);
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                    if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                }
            }
        }).start();

        new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = requestQueue.take();
                    new Thread(new ClientHandler(clientSocket)).start();
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while taking from queue: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        ProxyServer proxy = new ProxyServer();
        proxy.start();
    }

    class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                // Read and store full request
                String requestLine = clientIn.readLine();
                if (requestLine == null || requestLine.isEmpty()) {
                    return;
                }
                System.out.println("Request: " + requestLine);

                List<String> headers = new ArrayList<>();
                String host = null;
                String line;
                while ((line = clientIn.readLine()) != null && !line.isEmpty()) {
                    headers.add(line);
                    System.out.println("Header: " + line);
                    if (line.toLowerCase().startsWith("host:")) {
                        host = line.split(":", 2)[1].trim();
                    }
                }

                if (host == null) {
                    String[] requestParts = requestLine.split(" ");
                    if (requestParts.length < 2) return;
                    URL targetUrl = new URL(requestParts[1]);
                    host = targetUrl.getHost();
                }

                // Connect to target server
                try (
                    Socket targetSocket = new Socket(host, 80);
                    PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true);
                    BufferedReader targetIn = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()))
                ) {
                    // Forward full request
                    targetOut.println(requestLine);
                    for (String header : headers) {
                        targetOut.println(header);
                    }
                    targetOut.println(); // End headers

                    // Forward response
                    String responseLine;
                    while ((responseLine = targetIn.readLine()) != null) {
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