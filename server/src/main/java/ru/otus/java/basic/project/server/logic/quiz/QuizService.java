package ru.otus.java.basic.project.server.logic.quiz;

import ru.otus.java.basic.project.server.providers.db.jdbc.entity.Question;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.QuestionsUser;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.Quiz;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.QuizUser;

import java.util.List;

public interface QuizService {

    List<Quiz> getQuizzes();

    Quiz getQuiz(int quizId, boolean withQuestions);

    Quiz getQuiz(String title, boolean withQuestions);

    Quiz addQuiz(Quiz quiz);

    Quiz updateQuiz(Quiz quiz);

    Question addQuestion(Question question);

    Question updateQuestion(Question question);

    List<Question> getQuestions(int quizId);

    List<Quiz> getAvailable();

    List<Quiz> getUserQuizzes(int userId);

    QuizUser getQuizUser(int quizId, int userId);

    QuizUser addQuizUser(QuizUser quizUser);

    QuizUser updateQuizUser(QuizUser quizUser);

    QuestionsUser getQuestionsUser(int questionId, int userId);

    QuestionsUser addQuestionsUser(QuestionsUser questionsUser);

    QuestionsUser updateQuestionsUser(QuestionsUser questionsUser);
}
