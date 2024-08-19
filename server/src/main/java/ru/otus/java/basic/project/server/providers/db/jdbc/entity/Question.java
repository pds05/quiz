package ru.otus.java.basic.project.server.providers.db.jdbc.entity;

public class Question {
    private Integer id;
    private Integer quizId;
    private String questionText;
    private String answerText;
    private int orderF;
    private int difficultyLevel;

    public Question(int id, Integer quizId, String questionText, String answerText, int orderF, int difficultyLevel) {
        this.id = id;
        this.quizId = quizId;
        this.questionText = questionText;
        this.answerText = answerText;
        this.orderF = orderF;
        this.difficultyLevel = difficultyLevel;
    }

    public Question(Integer quizId, String questionText, String answerText, int orderF, int difficultyLevel) {
        this.quizId = quizId;
        this.questionText = questionText;
        this.answerText = answerText;
        this.orderF = orderF;
        this.difficultyLevel = difficultyLevel;
    }

    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public int getOrderF() {
        return orderF;
    }

    public void setOrderF(int orderF) {
        this.orderF = orderF;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", quizId='" + quizId + '\'' +
                ", questionText='" + questionText + '\'' +
                ", answerText='" + answerText + '\'' +
                ", orderF=" + orderF +
                ", difficultyLevel=" + difficultyLevel +
                '}';
    }
}
