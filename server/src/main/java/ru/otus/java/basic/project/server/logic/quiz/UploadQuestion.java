package ru.otus.java.basic.project.server.logic.quiz;

public class UploadQuestion {
    private String question;
    private String answer;
    private int order;
    private int difficulty;

    public UploadQuestion() {
    }

    public UploadQuestion(String question, String answer, int order, int difficulty) {
        this.question = question;
        this.answer = answer;
        this.order = order;
        this.difficulty = difficulty;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public String toString() {
        return "UploadQuestion{" +
                "question='" + question + '\'' +
                ", answer='" + answer + '\'' +
                ", order=" + order +
                ", difficulty=" + difficulty +
                '}';
    }
}
