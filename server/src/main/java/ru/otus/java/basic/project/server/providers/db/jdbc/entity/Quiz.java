package ru.otus.java.basic.project.server.providers.db.jdbc.entity;

import java.util.Date;
import java.util.List;

public class Quiz {
    private Integer id;
    private String title;
    private String grade;
    private int creatorId;
    private Date createdDate;
    private Date hideDate;
    private boolean isActive;
    private List<Question> questions;

    public Quiz(String title, int creatorId, boolean isActive) {
        this.title = title;
        this.creatorId = creatorId;
        this.isActive = isActive;
    }

    public Quiz(Integer id, String title, String grade, int creatorId, Date createdDate, Date hideDate, boolean isActive) {
        this.id = id;
        this.title = title;
        this.grade = grade;
        this.creatorId = creatorId;
        this.createdDate = createdDate;
        this.hideDate = hideDate;
        this.isActive = isActive;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getHideDate() {
        return hideDate;
    }

    public void setHideDate(Date hideDate) {
        this.hideDate = hideDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    @Override
    public String toString() {
        return "Quiz{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", grade=" + grade +
                ", creatorId=" + creatorId +
                ", createdDate=" + createdDate +
                ", hideDate=" + hideDate +
                ", isActive=" + isActive +
                ", questions=" + questions +
                '}';
    }
}
