public class Quiz {
    public String Question; // Public field to hold the quiz question
    public String[] Answers; // Public field to hold the answer choices
    public int CorrectAnswer; // Public field to hold the index of the correct answer

    public Quiz(String question , String[] answers , int correctAnswer) {
        Question = question; // Initialize the quiz question
        Answers = answers; // Initialize the answer choices
        CorrectAnswer = correctAnswer; // Initialize the index of the correct answer
    }

    public String getQuestion() {

        return Question; // Getter method to retrieve the quiz question
    }

    public String[] getAnswers() {

        return Answers; // Getter method to retrieve the answer choices
    }


    // Override the toString() method to provide a custom string representation of the quiz
    @Override
    public String toString() {
        // Create a string representation with the question and answer choices
        String str = Question + "\n";
        for (String answer : Answers) {
            str += answer + "\n";
        }
        return str;
    }

}
