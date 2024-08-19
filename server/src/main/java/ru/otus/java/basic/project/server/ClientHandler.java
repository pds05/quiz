package ru.otus.java.basic.project.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.java.basic.project.server.logic.operation.OperationLogic;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.User;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public class ClientHandler implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(ClientHandler.class.getName());

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private User user;
    private String username;

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.username = user.getUsername();
    }

    @Override
    public void run() {
        try {
            username = socket.getRemoteSocketAddress().toString();
            logger.debug("Подключился новый клиент " + username);
            sendMessage("Пройдите аутентификацию в чате /auth или зарегистрируйтесь /register");
            while (true) {
                String message = in.readUTF().trim();
                if (message.startsWith("/")) {
                    if (message.equals("/exit")) {
                        sendMessage("/exit-ok");
                        return;
                    }
                    String command = message.split(" ", 2)[0].substring(1);
                    try {
                        if (authorizeCommand(command)) {
                            OperationLogic.Authentication aa;
                            try {
                                aa = OperationLogic.Authentication.valueOf(command);
                            } catch (IllegalArgumentException eae) {
                                sendMessage("/" + command + "-nok команда не доступна");
                                continue;
                            }
                            if (aa.process(server, this, message)) {
                                break;
                            }
                            continue;
                        }
                        continue;
                    } catch (AppException ae) {
                        sendMessage("/" + command + "-nok сервис временно не доступен");
                    }
                }
                sendMessage("Перед работой с чатом необходимо выполнить аутентификацию '/auth login password' или регистрацию '/register login password username'");
            }
            sendMessage("Вы подключены к чату!");
            while (true) {
                String message = in.readUTF().trim();
                if (message.startsWith("/")) {
                    if (message.equals("/exit")) {
                        sendMessage("/exit-ok");
                        break;
                    }
                    String command = message.split(" ", 2)[0].substring(1);
                    OperationLogic.Operations operations;
                    try {
                        operations = OperationLogic.Operations.valueOf(command);
                    } catch (IllegalArgumentException eae) {
                        sendMessage("/" + command + "-nok команда не доступна");
                        continue;
                    }
                    try {
                        if (authorizeCommand(command)) {
                            operations.process(server, this, message);
                        }
                    } catch (Exception ae) {
                        sendMessage("/" + command + "-nok: команда не выполнена");
                        logger.error(ae.getMessage(), ae);
                    }
                    continue;
                }
                server.broadcastMessage(username + ": " + message);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработчика сообщений", e);
        } finally {
            disconnect();
        }
    }

    private boolean authorizeCommand(String command) {
        if (server.getUserService().authorization(this, command)) {
            return true;
        } else {
            sendMessage("/" + command + "-nok: комманда не доступна");
            return false;
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            logger.error("Не доступен исходящий поток, client={}", socket.getRemoteSocketAddress().toString(), e);
            throw new AppException("Ошибка отправки сообщения");
        }
    }

    public String readMessage() {
        String input;
        try {
            input = in.readUTF();
        } catch (IOException e) {
            logger.error("Не доступен входящий поток, client={}", socket.getRemoteSocketAddress().toString(), e);
            throw new AppException("Ошибка чтения сообщения");
        }
        return input;
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка закрытия потока ввода/вывода", e);
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка закрытия сокета", e);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ClientHandler that = (ClientHandler) object;
        return Objects.equals(socket, that.socket) && Objects.equals(user, that.user) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(socket, user, username);
    }
}
