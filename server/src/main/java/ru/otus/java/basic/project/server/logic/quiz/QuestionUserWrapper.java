package ru.otus.java.basic.project.server.logic.quiz;

import ru.otus.java.basic.project.server.providers.db.jdbc.entity.Question;

public class QuestionUserWrapper {
    private Question question;
    private String answer;
    private boolean isCorrect;

    public QuestionUserWrapper(Question question, String answer, boolean isCorrect) {
        this.question = question;
        this.answer = answer;
        this.isCorrect = isCorrect;
    }

    public QuestionUserWrapper(Question question, String answer) {
        this.question = question;
        this.answer = answer;
        this.isCorrect = question.getAnswerText().equals(answer);
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }
}
