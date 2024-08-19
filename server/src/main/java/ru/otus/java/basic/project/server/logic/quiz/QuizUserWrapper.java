package ru.otus.java.basic.project.server.logic.quiz;

import ru.otus.java.basic.project.server.providers.db.jdbc.entity.Quiz;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.QuizUser;

import java.util.ArrayList;
import java.util.List;

public class QuizUserWrapper {
    private Quiz quiz;
    private QuizUser quizUser;
    private List<QuestionUserWrapper> answers = new ArrayList<>();

    public QuizUserWrapper(Quiz quiz, QuizUser quizUser) {
        this.quiz = quiz;
        this.quizUser = quizUser;
    }

    public String getScore() {
        float score = 0;
        for (QuestionUserWrapper answer : answers) {
            score += (answer.getQuestion().getDifficultyLevel() == 0) ? 1 : (float) (answer.getQuestion().getDifficultyLevel() / 10);
        }
        return String.format("%.1f", score);
    }

    public void addQuestionUserWrapper(QuestionUserWrapper questionUserWrapper) {
        answers.add(questionUserWrapper);
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public QuizUser getQuizUser() {
        return quizUser;
    }

    public void setQuizUser(QuizUser quizUser) {
        this.quizUser = quizUser;
    }

    public List<QuestionUserWrapper> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuestionUserWrapper> answers) {
        this.answers = answers;
    }
}
