import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket serverSocket; // ServerSocket to listen for incoming connections

    // Entry point of the Server application
    public static void main(String[] args) {
        Server server = new Server();
        server.startServer(); // Start the server to listen for client connections
    }

    // Method to start the server and listen for incoming client connections
    private void startServer() {

        try {
            // Create a ServerSocket and bind it to port 8888
            serverSocket = new ServerSocket(8888);
            System.out.println("Server run on: localhost:8888");
        } catch (IOException e) {
            e.printStackTrace();
        }


        new Thread(() -> {
            while (true) {
                System.out.println("Waiting for client...");
                try {
                    final Socket client = serverSocket.accept();
                    System.out.println("New client connection: " + client);
                    ClientManager.manage(client);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}
