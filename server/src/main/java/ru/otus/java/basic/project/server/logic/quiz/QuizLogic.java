package ru.otus.java.basic.project.server.logic.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.otus.java.basic.project.server.AppModule;
import ru.otus.java.basic.project.server.ClientHandler;
import ru.otus.java.basic.project.server.Server;
import ru.otus.java.basic.project.server.logic.LogicProcessor;
import ru.otus.java.basic.project.server.logic.operation.OperationException;
import ru.otus.java.basic.project.server.providers.db.jdbc.QuizDAO;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.*;

import java.util.*;
import java.util.concurrent.*;

public class QuizLogic implements AppModule {
    public static final Logger logger = LoggerFactory.getLogger(QuizLogic.class.getName());

    public static final int WAITING_RESPONSE_TIMEOUT = 20;
    public static final int MAX_UPLOAD_QUESTIONS = 20;
    public static final int QUIZ_EXECUTOR_POOL = 10;
    public static final int PERIOD_DEACTIVATION_EXECUTE = 30;
    public static final int ACTIVE_QUIZ_PERIOD = 1;

    private static ExecutorService executor;
    private static ScheduledExecutorService scheduledExecutorService;
    private static Map<ClientHandler, Quiz> activeQuizzes = new ConcurrentHashMap<>();

    private QuizService quizService;

    public QuizLogic(QuizService quizService) {
        this.quizService = quizService;
        initialize();
    }

    @Override
    public void initialize() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(QUIZ_EXECUTOR_POOL);
        }
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                List<Quiz> quizzes = quizService.getQuizzes();
                for (Quiz quiz : quizzes) {
                    Calendar now = Calendar.getInstance();
                    Calendar quizDate = Calendar.getInstance();
                    quizDate.setTime(quiz.getCreatedDate());
                    if ((now.get(Calendar.DAY_OF_MONTH) - quizDate.get(Calendar.DAY_OF_MONTH)) > ACTIVE_QUIZ_PERIOD) {
                        quiz.setActive(false);
                        quiz.setHideDate(new Date());
                        quizService.updateQuiz(quiz);
                        logger.info("Викторина отключена по расписанию " + quiz);
                    }
                }
            }, PERIOD_DEACTIVATION_EXECUTE, PERIOD_DEACTIVATION_EXECUTE, TimeUnit.SECONDS);
        }
    }

    @Override
    public void shutdown() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public boolean isClientQuizBusy(ClientHandler clientHandler) {
        return activeQuizzes.containsKey(clientHandler);
    }

    private static String waitingResponse(ClientHandler clientHandler, int waitTimeout, String errorMessage) {
        Future<String> future = executor.submit(new ClientListener(clientHandler));
        String response = null;
        try {
            response = future.get(waitTimeout, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                clientHandler.sendMessage(errorMessage);
            }
            future.cancel(true);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    static class ClientListener implements Callable<String> {
        private ClientHandler clientHandler;

        public ClientListener(ClientHandler clientHandler) {
            this.clientHandler = clientHandler;
        }

        @Override
        public String call() throws Exception {
            return clientHandler.readMessage();
        }
    }

    public enum Operations implements LogicProcessor {
        upload {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                String[] elements = inputMessage.split(" ", 2);
                if (elements.length != 2) {
                    clientHandler.sendMessage("Неверный формат команды. Формат команды: /upload jsonFile");
                    return;
                }
                ObjectMapper mapper = new ObjectMapper();
                UploadQuiz uploadQuiz;
                try {
                    uploadQuiz = mapper.readValue(elements[1], UploadQuiz.class);

                    Quiz quiz = new Quiz(uploadQuiz.getTitle(), clientHandler.getUser().getId(), true);
                    server.getQuizService().addQuiz(quiz);
                    List<Question> uploadedQuestions = new ArrayList<>();

                    int count = 0;
                    int overCount = 0;
                    for (UploadQuestion uploadQuestion : uploadQuiz.getQuestions()) {
                        if (count < MAX_UPLOAD_QUESTIONS) {
                            Question question = new Question(quiz.getId(),
                                    uploadQuestion.getQuestion(),
                                    uploadQuestion.getAnswer(),
                                    uploadQuestion.getOrder(),
                                    uploadQuestion.getDifficulty());
                            question = server.getQuizService().addQuestion(question);
                            uploadedQuestions.add(question);
                            count++;
                        } else {
                            overCount++;
                        }
                    }
                    if (uploadQuiz.getQuestions().size() > MAX_UPLOAD_QUESTIONS) {
                        clientHandler.sendMessage("Загружено " + MAX_UPLOAD_QUESTIONS +
                                " вопросов, последние " + overCount +
                                " вопросов превышают установленный лимит");
                    }
                    quiz.setQuestions(uploadedQuestions);
                    clientHandler.sendMessage("/upload-ok викторина '" + quiz.getTitle() + "' запущена");
                    server.broadcastMessage("Запущенна новая викторина '" + quiz.getTitle() + "'");
                } catch (JsonProcessingException e) {
                    logger.warn("От клиента {} получен некорректный json: {}", clientHandler.getUser().getUsername(), e.getMessage());
                    clientHandler.sendMessage("/upload-nok некорректный формат загрузки викторины: " + e.getMessage());
                }
            }
        },

        quizes {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                List<Quiz> quizzes = server.getQuizService().getAvailable();
                String[] title = quizzes.stream().map(Quiz::getTitle).toArray(String[]::new);
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < title.length; i++) {
                    builder.append("'").append(title[i]).append("'");
                    if (i != title.length - 1) {
                        builder.append("\r\n");
                    }
                }
                clientHandler.sendMessage("/quizes-ok\r\nCписок викторин:\r\n" + builder);
            }
        },

        join {
            @Override
            public void process(Server server, ClientHandler clientHandler, String inputMessage) {
                String[] elements = inputMessage.split(" ", 2);
                User user = clientHandler.getUser();
                QuizService service = server.getQuizService();
                if (elements.length != 2) {
                    clientHandler.sendMessage("Неверный формат команды. Формат команды: /join quizTitle");
                    return;
                }
                String quizName = elements[1];
                Quiz quiz = server.getQuizService().getQuiz(quizName, true);
                if (quiz == null) {
                    throw new OperationException("Викторина " + quizName + " не доступна");
                }
                if (service.getQuizUser(quiz.getId(), user.getId()) != null) {
                    QuizUser quizUser = service.getQuizUser(quiz.getId(), user.getId());
                    clientHandler.sendMessage("Вы уже участвовали в этой викторине, ваш результат " + quizUser.getScoreResult() + " баллов");
                    return;
                }
                try {
                    activeQuizzes.put(clientHandler, quiz);
                    clientHandler.sendMessage("Викторина " + quiz.getTitle() + " началась!\r\nНа каждый ответ дается " + WAITING_RESPONSE_TIMEOUT + " секунд");

                    //добавляем запись о запуске викторины
                    QuizUser quizUser = new QuizUser(quiz.getId(), clientHandler.getUser().getId());
                    service.addQuizUser(quizUser);
                    QuizUserWrapper cacheQuizUser = new QuizUserWrapper(quiz, quizUser);

                    int count = 1;
                    for (Question question : quiz.getQuestions()) {
                        clientHandler.sendMessage("Вопрос " + count + ":\r\n" + question.getQuestionText());
                        String answer = waitingResponse(clientHandler, WAITING_RESPONSE_TIMEOUT, "Время ответа истекло. Следующий вопрос:");

                        QuestionUserWrapper cacheAnswer = new QuestionUserWrapper(question, answer);
                        cacheQuizUser.addQuestionUserWrapper(cacheAnswer);

                        //сохраняем результат ответа на вопрос
                        QuestionsUser questionsUser = new QuestionsUser(question.getId(), clientHandler.getUser().getId(), cacheAnswer.isCorrect());
                        service.addQuestionsUser(questionsUser);
                        count++;
                    }
                    clientHandler.sendMessage(printResult(cacheQuizUser));
                    clientHandler.sendMessage("Оцените на сколько вам понравилась викторина от 1 до 10");
                    //Если пользователь поставит свою оценку викторине, тогда обновим общую оценку викторины
                    String userQuizGrade = waitingResponse(clientHandler, 30, "");
                    if (userQuizGrade != null) {
                        quiz.setGrade(calculateQuizGrade(service, quiz, userQuizGrade));
                        service.updateQuiz(quiz);
                    }
                    clientHandler.sendMessage("Спасибо за участие в викторине!");

                    //сохраняем результат викторины
                    quizUser.setScoreResult(cacheQuizUser.getScore());
                    quizUser.setEndDate(new Date());
                    quizUser.setQuizGrade(Integer.valueOf(userQuizGrade));
                    service.updateQuizUser(quizUser);
                } finally {
                    activeQuizzes.remove(clientHandler);
                }
            }
        }
    }

    private static String calculateQuizGrade(QuizService service, Quiz quiz, String usersGrade) {
        int passesUsers = ((QuizDAO) service).getCountQuizUsers(quiz.getId());
        float baseGrade = Float.valueOf(quiz.getGrade().replace(',', '.'));
        float newGrade = (baseGrade + Integer.valueOf(usersGrade)) / passesUsers;
        return String.format("%.1f", newGrade);
    }

    private static String printResult(QuizUserWrapper wrapper) {
        StringBuilder builder = new StringBuilder("Результат викторины ");
        builder.append(wrapper.getQuiz().getTitle())
                .append(":\r\n")
                .append("Вы набрали ").append(wrapper.getScore()).append(" баллов\r\n");
        int count = 1;
        for (QuestionUserWrapper question : wrapper.getAnswers()) {
            builder.append("Вопрос ").append(count).append(" - ");
            if (question.isCorrect()) {
                builder.append("верно!\r\n");
            } else {
                builder.append("ошибка, правильный ответ - ").append(question.getQuestion().getAnswerText()).append("\r\n");
            }
            count++;
        }
        return builder.toString();
    }
}
