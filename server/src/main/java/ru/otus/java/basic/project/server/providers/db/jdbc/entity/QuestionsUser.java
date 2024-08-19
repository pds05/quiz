package ru.otus.java.basic.project.server.providers.db.jdbc.entity;

public class QuestionsUser {
    private Integer id;
    private int questionId;
    private int userId;
    boolean isCorrect;

    public QuestionsUser(int questionId, int userId, boolean isCorrect) {
        this.questionId = questionId;
        this.userId = userId;
        this.isCorrect = isCorrect;
    }

    public QuestionsUser(Integer id, int questionId, int userId, boolean isCorrect) {
        this.id = id;
        this.questionId = questionId;
        this.userId = userId;
        this.isCorrect = isCorrect;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }
}
