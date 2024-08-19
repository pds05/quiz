package ru.otus.java.basic.project.server.providers.db.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.java.basic.project.server.AppException;
import ru.otus.java.basic.project.server.logic.operation.OperationException;
import ru.otus.java.basic.project.server.logic.quiz.QuizService;
import ru.otus.java.basic.project.server.providers.db.AppDatabasePool;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.Question;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.QuestionsUser;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.Quiz;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.QuizUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QuizDAO implements QuizService {
    public static final Logger logger = LoggerFactory.getLogger(QuizDAO.class.getName());

    public static final String FIND_ACTIVE_QUIZZES_QUERY = "select * from quiz where IS_ACTIVE = true";
    public static final String FIND_QUIZ_BY_ID_QUERY = "select * from quiz where ID = ?";
    public static final String FIND_QUIZ_BY_TITLE_QUERY = "select * from quiz where TITLE = ?";
    public static final String FIND_USER_QUIZ_QUERY = "select * from quiz where CREATOR_ID = ?";
    public static final String FIND_ACTIVE_QUIZ_QUERY = "select * from quiz where IS_ACTIVE = true order by TITLE";
    public static final String ADD_QUIZ_QUERY = "INSERT INTO quiz (TITLE, CREATOR_ID, CREATED_DATE, IS_ACTIVE) VALUES (?, ?, ?, ?)";
    public static final String UPDATE_QUIZ_QUERY = """
            UPDATE quiz
            SET TITLE = ?, GRADE = ?, CREATOR_ID = ?, CREATED_DATE = ?, HIDE_DATE = ?, IS_ACTIVE = ?
            WHERE ID = ?
            """;
    public static final String ADD_QUESTION_QUERY = "INSERT INTO questions (QUIZ_ID, QUESTION_TEXT, ANSWER_TEXT, ORDER_F, DIFFICULTY_LEVEL) VALUES (?, ?, ?, ?, ?)";
    public static final String UPDATE_QUESTION_QUERY = """
            UPDATE questions
            SET QUIZ_ID = ?, QUESTION_TEXT = ?, ANSWER_TEXT = ?, ORDER_F = ?, DIFFICULTY_LEVEL = ?
            WHERE ID = ?
            """;
    public static final String FIND_QUESTIONS_QUERY = "select * from questions where QUIZ_ID = ? ORDER BY ORDER_F";
    public static final String FIND_QUIZ_USERS_QUERY = "select * from quiz_users_rel where QUIZ_ID = ? AND USER_ID = ?";
    public static final String FIND_COUNT_QUIZ_USERS_QUERY = "select COUNT(*) from quiz_users_rel where QUIZ_ID = ?";
    public static final String ADD_QUIZ_USERS_QUERY = """
            INSERT INTO quiz_users_rel (QUIZ_ID, USER_ID, START_DATE)
            VALUES (?, ?, ?)
            """;
    public static final String UPDATE_QUIZ_USERS_QUERY = """
            UPDATE quiz_users_rel
            SET END_DATE = ?, SCORE_RESULT = ?, QUIZ_GRADE = ?
            WHERE QUIZ_ID = ? AND USER_ID = ?
            """;
    public static final String FIND_QUESTIONS_USERS_QUERY = "select * from questions_users_rel where QUESTION_ID = ? AND USER_ID = ?";
    public static final String ADD_QUESTIONS_USERS_QUERY = """
            INSERT INTO questions_users_rel (QUESTION_ID, USER_ID, IS_CORRECT)
            VALUES (?, ?, ?)
            """;
    public static final String UPDATE_QUESTIONS_USERS_QUERY = """
            UPDATE questions_users_rel
            SET IS_CORRECT = ?
            WHERE QUESTION_ID = ?, AND USER_ID = ?
            """;

    private AppDatabasePool pool;

    public QuizDAO(AppDatabasePool pool) {
        this.pool = pool;
    }

    @Override
    public List<Quiz> getQuizzes() {
        List<Quiz> quizzes = new ArrayList<>();
        try (Connection connection = pool.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(FIND_ACTIVE_QUIZ_QUERY)) {
                quizzes.addAll(readQuiz(resultSet, false));
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return quizzes;
    }

    @Override
    public Quiz getQuiz(int quizId, boolean withQuestions) {
        Quiz quiz = null;
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_QUIZ_BY_ID_QUERY)) {
            statement.setInt(1, quizId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    List<Quiz> quizzes = readQuiz(resultSet, withQuestions);
                    if (quizzes.size() == 1) {
                        quiz = quizzes.get(0);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return quiz;
    }

    @Override
    public Quiz getQuiz(String title, boolean withQuestions) {
        Quiz quiz = null;
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_QUIZ_BY_TITLE_QUERY)) {
            statement.setString(1, title);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Quiz> quizzes = readQuiz(resultSet, withQuestions);
                if (quizzes.size() == 1) {
                    quiz = quizzes.get(0);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return quiz;
    }

    @Override
    public Quiz addQuiz(Quiz quiz) {
        checkQuizObject(quiz, false);
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_QUIZ_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, quiz.getTitle());
            preparedStatement.setInt(2, quiz.getCreatorId());
            preparedStatement.setTimestamp(3, new Timestamp(new Date().getTime()));
            preparedStatement.setBoolean(4, quiz.isActive());
            preparedStatement.executeQuery();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                quiz.setId(resultSet.getInt(1));
            } else {
                logger.warn("Ошибка добавления объекта в БД - не присвоен id, {}", quiz);
                throw new OperationException("Ошибка добавления объекта quiz");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return quiz;
    }

    @Override
    public List<Quiz> getAvailable() {
        List<Quiz> result = new ArrayList<>();
        try (Connection connection = pool.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(FIND_ACTIVE_QUIZ_QUERY)) {
                result.addAll(readQuiz(resultSet, false));
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return result;
    }

    private List<Quiz> readQuiz(ResultSet rs, boolean withQuestions) throws SQLException {
        List<Quiz> result = new ArrayList<>();
        while (rs.next()) {
            int quizId = rs.getInt("ID");
            String title = rs.getString("TITLE");
            String grade = rs.getString("GRADE");
            int userId = rs.getInt("CREATOR_ID");
            Date createdDate = rs.getTimestamp("CREATED_DATE");
            Date hideDate = rs.getTimestamp("HIDE_DATE");
            boolean active = rs.getBoolean("IS_ACTIVE");
            Quiz quiz = new Quiz(quizId, title, grade, userId, createdDate, hideDate, active);
            if (withQuestions) {
                List<Question> questions = getQuestions(quiz.getId());
                quiz.setQuestions(questions);
            }
            result.add(quiz);
        }
        return result;
    }

    @Override
    public List<Quiz> getUserQuizzes(int userId) {
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_USER_QUIZ_QUERY)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return readQuiz(resultSet, false);
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
    }

    @Override
    public QuizUser getQuizUser(int quizId, int userId) {
        QuizUser quizUser = null;
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_QUIZ_USERS_QUERY)) {
            statement.setInt(1, quizId);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int id = resultSet.getInt("ID");
                    Date startDate = resultSet.getTimestamp("START_DATE");
                    Date endDate = resultSet.getTimestamp("END_DATE");
                    String scoreResult = resultSet.getString("SCORE_RESULT");
                    int quizGrade = resultSet.getInt("QUIZ_GRADE");
                    quizUser = new QuizUser(id, quizId, userId, startDate, endDate, scoreResult, quizGrade);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return quizUser;
    }

    @Override
    public QuizUser addQuizUser(QuizUser quizUser) {
        checkQuizUser(quizUser, false);
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(ADD_QUIZ_USERS_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, quizUser.getQuizId());
            statement.setInt(2, quizUser.getUserId());
            statement.setTimestamp(3, new Timestamp(new Date().getTime()));
            statement.executeUpdate();
            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                quizUser.setId(resultSet.getInt(1));
            } else {
                logger.warn("Ошибка добавления объекта в БД - не присвоен id, {}", quizUser);
                throw new OperationException("Ошибка добавления объекта quizUser");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return quizUser;
    }

    @Override
    public QuizUser updateQuizUser(QuizUser quizUser) {
        checkQuizUser(quizUser, true);
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_QUIZ_USERS_QUERY)) {
            statement.setTimestamp(1, new Timestamp(new Date().getTime()));
            statement.setString(2, quizUser.getScoreResult());
            statement.setInt(3, quizUser.getQuizGrade());
            statement.setInt(4, quizUser.getQuizId());
            statement.setInt(5, quizUser.getUserId());
            int flag = statement.executeUpdate();
            if (flag != 1) {
                logger.warn("Ошибка обновления объекта в БД, {}", quizUser);
                throw new OperationException("Ошибка обновления quizUser");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return quizUser;
    }

    @Override
    public QuestionsUser getQuestionsUser(int questionId, int userId) {
        QuestionsUser questionsUser = null;
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_QUESTIONS_USERS_QUERY)) {
            statement.setInt(1, questionId);
            statement.setInt(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int id = resultSet.getInt("ID");
                    boolean isCorrect = resultSet.getBoolean("IS_CORRECT");
                    questionsUser = new QuestionsUser(id, questionId, userId, isCorrect);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return questionsUser;
    }

    @Override
    public QuestionsUser addQuestionsUser(QuestionsUser questionsUser) {
        checkQuestionsUser(questionsUser, false);
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(ADD_QUESTIONS_USERS_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, questionsUser.getQuestionId());
            statement.setInt(2, questionsUser.getUserId());
            statement.setBoolean(3, questionsUser.isCorrect());
            statement.executeUpdate();
            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                questionsUser.setId(resultSet.getInt(1));
            } else {
                logger.warn("Ошибка добавления объекта в БД - не присвоен id, {}", questionsUser);
                throw new OperationException("Ошибка добавления объекта questionsUser");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return questionsUser;
    }

    @Override
    public QuestionsUser updateQuestionsUser(QuestionsUser questionsUser) {
        checkQuestionsUser(questionsUser, true);
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_QUESTIONS_USERS_QUERY)) {
            statement.setBoolean(1, questionsUser.isCorrect());
            statement.setInt(2, questionsUser.getQuestionId());
            statement.setInt(3, questionsUser.getUserId());
            int flag = statement.executeUpdate();
            if (flag != 1) {
                logger.warn("Ошибка обновления объекта в БД, {}", questionsUser);
                throw new OperationException("Ошибка обновления questionsUser");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return questionsUser;
    }

    @Override
    public Quiz updateQuiz(Quiz quiz) {
        checkQuizObject(quiz, true);
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_QUIZ_QUERY)) {
            statement.setString(1, quiz.getTitle());
            statement.setString(2, quiz.getGrade());
            statement.setInt(3, quiz.getCreatorId());
            statement.setTimestamp(4, new Timestamp(quiz.getCreatedDate().getTime()));
            statement.setTimestamp(5, quiz.getHideDate() != null ? new Timestamp(quiz.getHideDate().getTime()) : null);
            statement.setBoolean(6, quiz.isActive());
            statement.setInt(7, quiz.getId());
            int flag = statement.executeUpdate();
            if (flag != 1) {
                logger.warn("Ошибка обновления объекта в БД, {}", quiz);
                throw new OperationException("Ошибка обновления quiz");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return quiz;
    }

    @Override
    public Question updateQuestion(Question question) {
        checkQuestion(question, true);
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_QUESTION_QUERY)) {
            statement.setInt(1, question.getQuizId());
            statement.setString(2, question.getQuestionText());
            statement.setString(3, question.getAnswerText());
            statement.setInt(4, question.getOrderF());
            statement.setInt(5, question.getDifficultyLevel());
            statement.setInt(6, question.getId());
            int flag = statement.executeUpdate();
            if (flag != 1) {
                logger.warn("Ошибка обновления объекта в БД, {}", question);
                throw new OperationException("Ошибка обновления question");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return question;
    }


    @Override
    public Question addQuestion(Question question) {
        checkQuestion(question, false);
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(ADD_QUESTION_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, question.getQuizId());
            statement.setString(2, question.getQuestionText());
            statement.setString(3, question.getAnswerText());
            statement.setInt(4, question.getOrderF());
            statement.setInt(5, question.getDifficultyLevel());
            statement.executeUpdate();
            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                question.setId(resultSet.getInt(1));
            } else {
                logger.warn("Ошибка добавления объекта в БД - не присвоен id, {}", question);
                throw new OperationException("Ошибка добавления объекта question");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return question;
    }

    @Override
    public List<Question> getQuestions(int quizId) {
        List<Question> questions = new ArrayList<>();
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_QUESTIONS_QUERY)) {
            statement.setInt(1, quizId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int questionId = resultSet.getInt("ID");
                    String questionText = resultSet.getString("QUESTION_TEXT");
                    String answerText = resultSet.getString("ANSWER_TEXT");
                    int orderF = resultSet.getInt("ORDER_F");
                    int difficultyLevel = resultSet.getInt("DIFFICULTY_LEVEL");
                    Question question = new Question(questionId, quizId, questionText, answerText, orderF, difficultyLevel);
                    questions.add(question);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return questions;
    }

    public int getCountQuizUsers(int quizId) {
        int count = 0;
        try (Connection connection = pool.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_COUNT_QUIZ_USERS_QUERY)) {
            statement.setInt(1, quizId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    count = resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return count;
    }

    private void checkQuizObject(Quiz quiz, boolean sync) {
        if (quiz == null) {
            logger.warn("Объект quiz не инициализирован, quiz is null");
            throw new AppException("quiz is null");
        }
        if (sync && quiz.getId() == null) {
            logger.warn("Объект quiz не синхронизирован c БД, quiz id is null, {}", quiz);
            throw new AppException("quiz id is null");
        }
    }

    private void checkQuestion(Question question, boolean sync) {
        if (question == null) {
            logger.warn("Объект question не инициализирован, question is null");
            throw new AppException("question is null");
        }
        if (sync && question.getId() == null) {
            logger.warn("Объект question не синхронизирован c БД, question id is null, {}", question);
            throw new AppException("question id is null");
        }
    }

    private void checkQuestionsUser(QuestionsUser questionsUser, boolean sync) {
        if (questionsUser == null) {
            logger.warn("Объект questionsUser не инициализирован, questionsUser is null");
            throw new AppException("questionsUser is null");
        }
        if (sync && questionsUser.getId() == null) {
            logger.warn("Объект questionsUser не синхронизирован c БД, questionsUser id is null, {}", questionsUser);
            throw new AppException("questionsUser id is null");
        }
    }

    private void checkQuizUser(QuizUser quizUser, boolean sync) {
        if (quizUser == null) {
            logger.warn("Объект quizUser не инициализирован, quizUser is null");
            throw new AppException("quizUser is null");
        }
        if (sync && quizUser.getId() == null) {
            logger.warn("Объект quizUser не синхронизирован c БД, quizUser id is null, {}", quizUser);
            throw new AppException("quizUser id is null");
        }
    }

    private void checkQuestionList(List<Question> questions) {
        if (questions == null) {
            logger.warn("Список questions не инициализирован, questions list is null");
            throw new AppException("questions list is null");
        }
        if (questions.isEmpty()) {
            logger.warn("Список questions пустой, questions list is empty");
            throw new AppException("questions list is empty");
        }
    }
}
