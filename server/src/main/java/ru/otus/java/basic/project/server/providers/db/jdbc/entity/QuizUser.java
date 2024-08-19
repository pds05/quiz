package ru.otus.java.basic.project.server.providers.db.jdbc.entity;

import java.util.Date;

public class QuizUser {
    private Integer id;
    private int quizId;
    private int userId;
    private Date startDate;
    private Date endDate;
    private String scoreResult;
    private int quizGrade;

    public QuizUser(int quizId, int userId) {
        this.quizId = quizId;
        this.userId = userId;
    }

    public QuizUser(Integer id, int quizId, int userId, Date startDate, Date endDate, String scoreResult, int quizGrade) {
        this.id = id;
        this.quizId = quizId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.scoreResult = scoreResult;
        this.quizGrade = quizGrade;
    }

    public QuizUser(Integer quizId, int userId, Date startDate, Date endDate, String scoreResult, int quizGrade) {
        this.quizId = quizId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.scoreResult = scoreResult;
        this.quizGrade = quizGrade;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getScoreResult() {
        return scoreResult;
    }

    public void setScoreResult(String scoreResult) {
        this.scoreResult = scoreResult;
    }

    public int getQuizGrade() {
        return quizGrade;
    }

    public void setQuizGrade(int quizGrade) {
        this.quizGrade = quizGrade;
    }
}
