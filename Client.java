import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Executors;

public class Client implements Runnable {

    // Attributes
    private final Socket socket; // The socket used to communicate with the server
    private PrintWriter writer; // PrintWriter to send messages to the server
    private BufferedReader reader; // BufferedReader to read messages from the server
    private final BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    private boolean first = true; // Flag to track if this is the first quiz
    private boolean waitForAnswerQuiz = true; // Flag to indicate whether to wait for quiz answers
    private String clientId; // The unique identifier assigned to this client


    // Constructor (private to prevent instantiation without a Socket)
    private Client(Socket socket) {
        this.socket = socket;
    }


    // Static method to create and manage a new Client instance
    public static void manage(Socket socket) {
        // Use a thread pool to execute the Client instance in a separate thread
        Executors.newCachedThreadPool().execute(new Client(socket));
    }


    @Override
    // Entry point for the Client when it runs as a thread
    public void run() {
        try {
            // Initialize input and output streams for communication
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream() , true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Start listening for incoming messages and process user input
        onMessage();
        sendMessage();
    }

    // Method to handle incoming messages from the Server
    private void onMessage() {
        new Thread(() -> {
            // Continuously listen for messages from the server as long as the socket is connected and not closed
            while (socket.isConnected() && !socket.isClosed()) {
                try {
                    // Read the message from the server
                    String line = reader.readLine();
                    if (line != null)
                        // Pass the message to the messageHandler method for further processing
                        messageHandler(line);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    // Method to send messages from the Client to the Server
    private void sendMessage() {
        new Thread(() -> {

            // Get client's name, surname, and age from the console input
            String name, surname, age;
            name = getInput("Please enter name");
            surname = getInput("Please enter surname");
            age = getInput("Please enter age");

            // Send a registration message to the Server with client details
            sendMessage("REGISTER" , String.format("%s,%s,%s" , name , surname , age));
            while (socket.isConnected() && !socket.isClosed()) {
                // Continue until the Client is ready to answer a quiz
                if (waitForAnswerQuiz) continue;

                String number;
                // Get the user's choice for quiz or quit from the console input
                number = getInput("1. Quiz\n2. QUIT\nPlease enter number");

                if (number.equals("1")) {
                    // If it's the first quiz, request a new quiz from the Server
                    if (first) {
                        sendMessage("QUIZ" , "true");
                    } else {
                        // For subsequent quizzes, ask for the next quiz
                        sendMessage("NEXT_QUIZ" , "true");
                    }
                    // Set waitForAnswerQuiz to true to wait for the quiz response
                    waitForAnswerQuiz = true;

                } else if (number.equals("2")) {
                    // If the user chooses to quit, send a quit message to the Server and close the socket
                    sendMessage("QUIT" , "true");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

            }

        }).start();
    }

    // Method to get user input from the console
    private String getInput(String message) {
        try {
            while (true) {
                System.out.print(message + ": ");
                // Read a line of input from the console
                String line = input.readLine();
                if (line != null) {
                    // Return the input line as the user's response
                    return line;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // Return null in case of an error
    }

    // Method to handle incoming messages from the Server
    private void messageHandler(String message) {
        try {
            // Split the incoming message into two parts using the ";;" separator
            final String[] split = message.split(";;");

            if (split.length != 2) {
                // If the message format is invalid, print an error message and throw an Exception
                System.out.printf("Fail to handler message: %s\n" , message);
                throw new Exception("Fail to handler message");
            }

            // Extract the event name and message content from the split parts
            String eventName = split[0];
            String msg = split[1].replace(";n;" , "\n");

            // Handle the event based on its name
            switch (eventName) {
                case "REGISTER":
                    // Handle the registration response from the Server and set clientId
                    clientId = msg;
                    System.out.printf("Successfully register , Your client id: %s\n" , clientId);
                    waitForAnswerQuiz = false; // Set the flag to false to proceed with quiz answering
                    break;
                case "QUIZ":
                    // Handle a new quiz received from the Server
                    newQuiz(msg);
                    break;
                case "RESULT_ANSWER":
                    // Handle the response to a quiz answer from the Server
                    resultAnswer(msg);
                    break;
                case "QUIT":
                    // Handle the quit message from the Server
                    socket.close(); // Close the socket to terminate the connection
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to handle the response to a quiz answer from the Server
    private void resultAnswer(String msg) {

        // Split the quiz result and points from the message using the "," separator
        String[] split = msg.split(",");

        if (split.length != 2) {
            // If the message format is invalid, print the result answer
            System.out.printf("Result answer: %s\n" , msg);
            return;
        }

        String result = split[0];
        String points = split[1];

        if (result.equalsIgnoreCase("yes")) {
            System.out.println("Bravo, You have 1 point, Points: " + points);
        } else if (result.equalsIgnoreCase("no")) {
            System.out.println("Wrong answer, Points: " + points);
        } else {
            System.out.printf("Result answer: %s\n" , msg);
        }
        // Set first and waitForAnswerQuiz to false to allow the Client to answer the next quiz
        first = false;
        waitForAnswerQuiz = false;
    }

    // Method to handle a new quiz received from the Server
    private void newQuiz(final String quiz) {
        // Display the new quiz received from the server
        System.out.println(quiz);
        // Get the user's answer to the quiz and send it to the Server
        String answer = getInput("Please enter answer");
        sendMessage("ANSWER_QUIZ" , answer);
    }

    // Method to send a message to the Server
    private void sendMessage(String eventName , String message) {
        // Print the message being sent to the server for debugging purposes
        System.out.printf("Sending message: Message: %s\n" , message);
        // Send the formatted message to the server through the writer
        writer.println(String.format("%s;;%s" , eventName , message));
        // Print the sent message for debugging purposes
        System.out.printf("Sent message: Message: %s\n" , message);
    }

    // Entry point of the Client application
    public static void main(String[] args) {
        try {
            System.out.println("Connecting to localhost:8888");
            // Create a socket to connect to the server running on localhost at port 8888
            Socket socket = new Socket("localhost" , 8888);
            System.out.println("Connected to localhost:8888");
            // Create and manage a new Client instance
            Client.manage(socket);
        } catch (IOException e) {
            // If there's an error while connecting to the server, print the stack trace
            e.printStackTrace();
        }
    }
}
