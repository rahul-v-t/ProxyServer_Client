import java.io.*;
import java.net.*;
// import java.util.concurrent.LinkedBlockingQueue;


public class ProxyServer {
    private ServerSocket serverSocket;
    private static final int PORT = 8084;

    public ProxyServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Proxy Server started on port " + PORT);
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + PORT);
            System.exit(1);
        }
    }

    public void start() {
        while (true) {
            try {
                // Accept client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
                // Handle request in a new thread
                new Thread(new ClientHandler(clientSocket)).start();
                
            } catch (IOException e) {
                System.err.println("Accept failed: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        ProxyServer proxy = new ProxyServer();
        proxy.start();
    }
}



// public class ProxyServer {
//     private LinkedBlockingQueue<Socket> requestQueue = new LinkedBlockingQueue<>();
//     private ServerSocket serverSocket ; // Declared as class field
//     private static final int PORT = 8082;
//     public void start() {
//         // Accept thread
//         new Thread(() -> {
//             while (true) {
//                 try {
//                     serverSocket = new ServerSocket(PORT);
//                     requestQueue.put(serverSocket.accept());
//                 } catch (Exception e) {
//                     System.err.println("Error accepting: " + e.getMessage());
//                 }
//             }
//         }).start();
        
//         // Processing thread
//         while (true) {
//             try {
//                 Socket clientSocket = requestQueue.take();
//                 new ClientHandler(clientSocket).run(); // Process synchronously
//             } catch (InterruptedException e) {
//                 Thread.currentThread().interrupt();
//             }
//         }
//     }
//     public static void main(String[] args) {
//         ProxyServer proxy = new ProxyServer();
//         proxy.start();
//     }
// }



class ClientHandler implements Runnable {
    private Socket clientSocket;
    private static final String TARGET_HOST = "example.com";
    private static final int TARGET_PORT = 80;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
            // Client streams
            BufferedReader clientIn = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
            
            // Target server socket and streams
            Socket targetSocket = new Socket(TARGET_HOST, TARGET_PORT);
            PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true);
            BufferedReader targetIn = new BufferedReader(
                new InputStreamReader(targetSocket.getInputStream()))
        ) {
            // Read client request
            String requestLine;
            while ((requestLine = clientIn.readLine()) != null) {
                if (requestLine.isEmpty()) {
                    break;
                }
                System.out.println("Request: " + requestLine);
                targetOut.println(requestLine);
            }

            // Send response back to client
            String responseLine;
            while ((responseLine = targetIn.readLine()) != null) {
                clientOut.println(responseLine);
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