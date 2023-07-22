public class Participant {

    public int Points; // Public field to hold the participant's points
    private final String name; // Private field to hold the participant's name
    private final String surname; // Private field to hold the participant's surname
    private final String age; // Private field to hold the participant's age

    public Participant(String name , String surname , String age) {
        // Constructor to initialize the participant's name, surname, and age
        this.name = name;
        this.surname = surname;
        this.age = age;
        this.Points = 0; // Initialize the participant's points to 0
    }

    public int points() {
        // Getter method to retrieve the participant's points
        return Points;
    }

    public void setPoints(int points) {
        // Setter method to update the participant's points
        this.Points = points;
    }

    public String name() {
        // Getter method to retrieve the participant's name
        return name;
    }

    public String surname() {
        // Getter method to retrieve the participant's surname
        return surname;
    }

    public String age() {
        // Getter method to retrieve the participant's age
        return age;
    }

    public void sendMessage(String message) {
        // Placeholder method for sending a message to the participant (implementation not provided)
    }

    public int getPoints() {
        // Getter method to retrieve the participant's points
        return Points;
    }
}
