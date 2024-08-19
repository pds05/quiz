package ru.otus.java.basic.project.server.providers.inmemory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.java.basic.project.server.providers.AuthException;
import ru.otus.java.basic.project.server.providers.AuthenticationProvider;
import ru.otus.java.basic.project.server.ClientHandler;
import ru.otus.java.basic.project.server.Server;

import java.util.ArrayList;
import java.util.List;

public class InMemoryAuthenticationProvider implements AuthenticationProvider {
    public static final Logger logger = LoggerFactory.getLogger(InMemoryAuthenticationProvider.class.getName());

    private class User {
        private String login;
        private String password;
        private String username;
        private List<AuthorizationRole> roles = new ArrayList<>();

        public User(String login, String password, String username, AuthorizationRole role) {
            this.login = login;
            this.password = password;
            this.username = username;
            roles.add(role);
        }

        public void addRole(AuthorizationRole role) {
            roles.add(role);
        }

        public void delRole(AuthorizationRole role) {
            roles.remove(role);
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    private Server server;
    private List<User> users;

    public InMemoryAuthenticationProvider(Server server) {
        this.server = server;
        this.users = new ArrayList<>();
        this.users.add(new User("login1", "pass1", "user1", AuthorizationRole.USER));
        this.users.add(new User("login2", "pass2", "user2", AuthorizationRole.USER));
        this.users.add(new User("login3", "pass3", "user3", AuthorizationRole.USER));
        this.users.add(new User("superuser", "superuser", "superuser", AuthorizationRole.ADMIN));
    }

    @Override
    public void initialize() {
        logger.info("Сервис аутентификации запущен: In-Memory режим");
    }

    private String getUsernameByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.username;
            }
        }
        return null;
    }

    private boolean isLoginAlreadyExist(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAuthorizationRole(String username, AuthorizationRole role) {
        if (role == AuthorizationRole.USER) {
            return true;
        }
        for (User user : users) {
            if (user.username.equals(username)) {
                for (AuthorizationRole authRole : user.roles) {
                    if (authRole == role || authRole.getPriority() <= role.getPriority()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isUsernameAlreadyExist(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void authenticate(ClientHandler clientHandler, String login, String password) throws AuthException {
        String authUsername = getUsernameByLoginAndPassword(login, password);
        if (authUsername == null) {
            throw new AuthException("Некорретный логин/пароль");
        }
        if (server.isUsernameBusy(authUsername)) {
            throw new AuthException("Указанная учетная запись уже занята");
        }
        clientHandler.setUsername(authUsername);
        server.subscribe(clientHandler);
    }

    @Override
    public void registration(ClientHandler clientHandler, String login, String password, String username) throws AuthException {
        if (login.trim().length() < 3 || password.trim().length() < 6 || username.trim().length() < 1) {
            throw new AuthException("Логин 3+ символа, Пароль 6+ символов, Имя пользователя 1+ символ");
        }
        if (isLoginAlreadyExist(login)) {
            throw new AuthException("Указанный логин уже занят");
        }
        if (isUsernameAlreadyExist(username)) {
            throw new AuthException("Указанное имя пользователя уже занято");
        }
        users.add(new User(login, password, username, AuthorizationRole.USER));
        clientHandler.setUsername(username);
        server.subscribe(clientHandler);
    }

    @Override
    public boolean authorization(ClientHandler clientHandler, String command) {
        AuthorizationRole ownerRole = AuthorizationRole.getAuthorizationRole(command);
        if (ownerRole == null) {
            return false;
        }
        return isAuthorizationRole(clientHandler.getUsername(), ownerRole);
    }

    public boolean removeUser(User user) {
        return users.remove(user);
    }
}