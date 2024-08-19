package ru.otus.java.basic.project.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.java.basic.project.server.logic.quiz.QuizLogic;
import ru.otus.java.basic.project.server.logic.quiz.QuizService;
import ru.otus.java.basic.project.server.logic.operation.UserService;
import ru.otus.java.basic.project.server.providers.db.AppDatabasePool;
import ru.otus.java.basic.project.server.providers.db.jdbc.DriverManagerConnectionPool;
import ru.otus.java.basic.project.server.providers.db.jdbc.HikariConnectionPool;
import ru.otus.java.basic.project.server.providers.db.jdbc.QuizDAO;
import ru.otus.java.basic.project.server.providers.db.jdbc.UserDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final Logger logger = LoggerFactory.getLogger(Server.class.getName());

    public static final int HANDLER_POOL_SIZE = 10;
    public static final String DB_URL = "jdbc:mariadb://localhost:3306/chat_db";

    private int port;
    private boolean isStopped;

    private Map<String, ClientHandler> clients;
    private UserService userService;
    private AppDatabasePool pool;
    private QuizService quizService;
    private QuizLogic quizLogic;

    private ExecutorService executorService;

    public Server(int port) {
        this.port = port;
        this.clients = new HashMap<>();
        this.pool = new HikariConnectionPool(DB_URL);
        this.quizService = new QuizDAO(pool);
        this.quizLogic = new QuizLogic(quizService);
        this.userService = new UserDAO(this);
    }

    public UserService getUserService() {
        return userService;
    }

    public QuizService getQuizService() {
        return quizService;
    }

    public QuizLogic getQuizLogic() {
        return quizLogic;
    }

    public AppDatabasePool getPool() {
        return pool;
    }

    public void start() {
        logger.info("Запуск сервера...");
        executorService = Executors.newFixedThreadPool(HANDLER_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            pool.initialize();
            userService.initialize();
            quizLogic.initialize();
            logger.info("Сервер запущен на порту: " + port);
            while (!isStopped) {
                Socket socket = serverSocket.accept();
                executorService.execute(new ClientHandler(this, socket));
            }
        } catch (IOException ioe) {
            logger.error("Сетевая ошибка сервера: {}", ioe.getMessage(), ioe);
        } catch (AppException ae) {
            logger.error("Системная ошибка сервера: {}", ae.getMessage(), ae);
        } catch (Exception e) {
            logger.error("Непредвиденная ошибка сервера: {}", e.getMessage(), e);
        }
        stop();
    }

    public void shutdown() {
        isStopped = true;
    }

    private void stop() {
        logger.info("Завершается работа сервера...");
        clients.forEach((k, v) -> kickUsername(k));
        executorService.shutdownNow();
        quizLogic.shutdown();
        pool.shutdown();
        logger.info("Сервер остановлен");
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        if (userService != null) {
            userService.connectUser(clientHandler.getUser());
        }
        broadcastMessage("В чат зашел: " + clientHandler.getUsername());
        clients.put(clientHandler.getUsername(), clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        if (userService != null) {
            userService.disconnectUser(clientHandler.getUser(), false);
        }
        clients.remove(clientHandler.getUsername());
        broadcastMessage("Из чата вышел: " + clientHandler.getUsername());
    }

    public synchronized void broadcastMessage(String message) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            ClientHandler clientHandler = entry.getValue();
            if (quizLogic.isClientQuizBusy(clientHandler)) {
                return;
            }
            entry.getValue().sendMessage(message);
        }
    }

    public ClientHandler getClientHandler(String username) {
        return clients.get(username);
    }

    public boolean isUsernameBusy(String username) {
        for (ClientHandler c : clients.values()) {
            if (c.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean kickUsername(String username) {
        for (ClientHandler clientHandler : clients.values()) {
            if (clientHandler.getUsername().equals(username)) {
                clientHandler.sendMessage("Вы отключены от чата");
                if (userService != null) {
                    userService.disconnectUser(clientHandler.getUser(), true);
                }
                clientHandler.disconnect();
                return true;
            }
        }
        return false;
    }
}
