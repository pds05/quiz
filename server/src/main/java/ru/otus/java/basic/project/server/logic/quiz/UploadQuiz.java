package ru.otus.java.basic.project.server.logic.quiz;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

public class UploadQuiz {
    private String title;

    @JsonDeserialize(as = ArrayList.class)
    private List<UploadQuestion> questions;

    public UploadQuiz() {
    }

    public UploadQuiz(String title, List<UploadQuestion> questions) {
        this.title = title;
        this.questions = questions;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<UploadQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<UploadQuestion> questions) {
        this.questions = questions;
    }

    @Override
    public String toString() {
        return "UploadQuiz{" +
                "title='" + title + '\'' +
                ", questions=" + questions +
                '}';
    }
}
