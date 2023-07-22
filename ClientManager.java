import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;

public class ClientManager implements Runnable {
    // Static list to hold all quiz instances
    public static final List<Quiz> quiz = new ArrayList<>();

    private static int id = 0; // Static variable to generate unique client IDs

    // Static map to store the connected clients using their IDs
    private static final Map<String, ClientManager> clients = new HashMap<>();

    private String clientId; // The unique ID assigned to this client

    private final Socket socket; // The client's socket

    private PrintWriter writer; // PrintWriter to send messages to the client
    private BufferedReader reader; // BufferedReader to read messages from the client

    private Participant participant; // Participant associated with this client

    private int quizNumber = 0; // Current quiz number the participant is attempting
    private boolean correctAnswer; // Flag to indicate whether the participant answered correctly

    // Private constructor (only called internally)
    private ClientManager(Socket socket) {

        this.socket = socket;
    }

    // Static method to manage a new client connection
    public static void manage(Socket socket) {

        // Execute the ClientManager instance in a new thread using a thread pool
        Executors.newCachedThreadPool().execute(new ClientManager(socket));
    }

    // Entry point for the ClientManager when it runs as a thread
    @Override
    public void run() {
        // Generate a unique client ID for this client
        clientId = String.valueOf(++id);
        // Store this ClientManager instance in the map of connected clients
        clients.put(clientId , this);

        try {
            // Initialize input and output streams for communication with the client
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream() , true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Start listening for incoming messages from the client and handle quiz initialization
        onMessage();
        createQuiz();
    }


    // Method to handle incoming messages from the client
    private void onMessage() {
        new Thread(() -> {
            while (socket.isConnected() && !socket.isClosed()) {
                try {
                    // Read a line of message from the client
                    String line = reader.readLine();
                    if (line != null)  // Read a line of message from the client
                        messageHandler(line);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    // Method to handle incoming messages from the client and process them
    private void messageHandler(String message) {
        System.out.printf("Receive new message from: %s , Message: %s" , clientId , message);

        try {
            // Split the incoming message into two parts using the ";;" separator
            final String[] split = message.split(";;");

            if (split.length != 2) {
                // If the message format is invalid, print an error message and throw an Exception
                System.out.printf("Fail to handler message: %s" , message);
                throw new Exception("Fail to handler message");
            }

            // Extract the event name and message content from the split parts
            String eventName = split[0];
            String msg = split[1];

            // If the participant is not registered yet, prevent handling other events
            if (!eventName.equals("REGISTER") && participant == null) {
                throw new Exception("Please register");
            }

            // Handle the event based on its name
            switch (eventName) {
                case "REGISTER":
                    register(msg);
                    break;
                case "QUIZ":
                    quiz();
                    break;
                case "ANSWER_QUIZ":
                    answerQuestion(msg);
                    break;
                case "NEXT_QUIZ":
                    nextQuiz();
                    break;
                case "QUIT":
                    quit();
                    break;
            }

        } catch (Exception e) {
            // If there's an error while processing the message, send an error message to the client
            sendMessage("ERROR" , String.format("Error message handing: %s" , e.getMessage()));
            e.printStackTrace();
        }

    }

    // Method to handle the client's request to quit and close the connection
    private void quit() {
        sendMessage("QUIT" , "Close connection");
        try {
            socket.close();
            // Remove this client from the map of connected clients
            clients.remove(clientId);
        } catch (IOException e) {
            // If there's an error while closing the socket, throw a RuntimeException
            throw new RuntimeException(e);
        }
    }

    // Method to handle the client's request for the next quiz
    private void nextQuiz() {
        // If the participant answered correctly, proceed to the next quiz
        if (correctAnswer) quizNumber++;
        // Send the next quiz question to the client
        sendMessage("QUIZ" , ClientManager.quiz.get(quizNumber).toString());
    }

    // Method to handle the client's response to a quiz question
    private void answerQuestion(String msg) {

        try {
            // Check if the quiz has ended
            if (quizNumber >= quiz.size()) {
                throw new Exception("Game ended");
            }
            // Check if the participant's answer is correct
            correctAnswer = isCorrectAnswer(quiz.get(quizNumber) , msg);

            if (correctAnswer) {
                // If the answer is correct, update the participant's points and send the result to the client
                participant.setPoints(participant.Points + 1);
                sendMessage("RESULT_ANSWER" , String.format("YES,%d" , participant.Points));
                return;
            }
            // If the answer is incorrect, send the result to the client
            sendMessage("RESULT_ANSWER" , String.format("NO,%d" , participant.Points));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to handle the client's request to start the quiz
    private void quiz() {
        // Reset the quiz number and send the first quiz question to the client
        quizNumber = 0;
        sendMessage("QUIZ" , ClientManager.quiz.get(quizNumber).toString());
    }

    // Method to handle the client's registration request
    private void register(String message) {
        try {
            // Split the registration message into name, surname, and age using the "," separator
            final String[] split = message.split(",");
            if (split.length != 3) {
                throw new Exception("Invalid register info");
            }

            // Extract name, surname, and age from the split parts
            String name = split[0];
            String surname = split[1];
            String age = split[2];

            // Create a new participant instance with the provided details
            participant = new Participant(name , surname , age);

            // Send the client's unique ID as the registration confirmation
            sendMessage("REGISTER" , clientId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to create the quiz questions and answers
    private void createQuiz() {
        // Add quiz questions to the quiz list
        quiz.add(new Quiz("Which of the following is NOT a Social Media Platform?" ,
                new String[]{
                        "A) Facebook" ,
                        "B) Twitter" ,
                        "C) Instagram" ,
                        "D) Google"} ,
                4));

        // Quiz 2
        quiz.add(new Quiz("Which planet is known as the Red Planet?" ,
                new String[]{
                        "A) Mars" ,
                        "B) Venus" ,
                        "C) Jupiter" ,
                        "D) Saturn"} ,
                1));

        // Quiz 3
        quiz.add(new Quiz("What is the chemical symbol for gold?" ,
                new String[]{
                        "A) Au" ,
                        "B) Ag" ,
                        "C) Fe" ,
                        "D) Hg"} ,
                3));

        // Quiz 4
        quiz.add(new Quiz("Who painted the Mona Lisa?" ,
                new String[]{
                        "A) Leonardo da Vinci" ,
                        "B) Vincent van Gogh" ,
                        "C) Pablo Picasso" ,
                        "D) Michelangelo"} ,
                1));

        // Quiz 5
        quiz.add(new Quiz("What is the capital of France?" ,
                new String[]{
                        "A) London" ,
                        "B) Berlin" ,
                        "C) Rome" ,
                        "D) Paris"} ,
                4));

        // Quiz 6
        quiz.add(new Quiz("Which country is the largest producer of coffee in the world?" ,
                new String[]{
                        "A) Brazil" ,
                        "B) Colombia" ,
                        "C) Ethiopia" ,
                        "D) Vietnam"} ,
                1));

        // Quiz 7
        quiz.add(new Quiz("What is the largest ocean on Earth?" ,
                new String[]{
                        "A) Pacific Ocean" ,
                        "B) Atlantic Ocean" ,
                        "C) Indian Ocean" ,
                        "D) Arctic Ocean"} ,
                3));

        // Quiz 8
        quiz.add(new Quiz("Who is the author of the Harry Potter book series?" ,
                new String[]{
                        "A) J.K. Rowling" ,
                        "B) Stephen King" ,
                        "C) George R.R. Martin" ,
                        "D) Suzanne Collins"} ,
                1));

        // Quiz 9
        quiz.add(new Quiz("What is the tallest mountain in the world?" ,
                new String[]{
                        "A) K2" ,
                        "B) Mount Everest" ,
                        "C) Mount Kilimanjaro" ,
                        "D) Mount McKinley"} ,
                2));

        // Quiz 10
        quiz.add(new Quiz("Which of the following is NOT a programming language?" ,
                new String[]{
                        "A) Java" ,
                        "B) Python" ,
                        "C) HTML" ,
                        "D) Java Script"} ,
                3));
    }

    // Method to send a message to the client
    private void sendMessage(String eventName , String message) {
        System.out.printf("Sending message: Client: %s , Message: %s\n" , clientId , message);
        // Replace newline characters in the message with a special identifier to preserve formatting
        writer.println(String.format("%s;;%s" , eventName , message.replace("\n" , ";n;")));
        System.out.printf("Sent message: Client: %s , Message: %s\n" , clientId , message);
    }

    // Method to check if the participant's response matches the correct answer
    private boolean isCorrectAnswer(Quiz quiz , String response) {
        // Answer checking logic goes here
        String correctAnswer;
        switch (quiz.CorrectAnswer) {
            case 1:
                correctAnswer = "A";
                break;
            case 2:
                correctAnswer = "B";
                break;
            case 3:
                correctAnswer = "C";
                break;
            default:
                correctAnswer = "D";
                break;
        }
        return response.equalsIgnoreCase(correctAnswer);

    }
}
