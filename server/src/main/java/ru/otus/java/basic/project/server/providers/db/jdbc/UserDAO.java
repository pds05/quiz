package ru.otus.java.basic.project.server.providers.db.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.java.basic.project.server.ClientHandler;
import ru.otus.java.basic.project.server.logic.operation.OperationException;
import ru.otus.java.basic.project.server.Server;
import ru.otus.java.basic.project.server.AppException;
import ru.otus.java.basic.project.server.logic.operation.UserService;
import ru.otus.java.basic.project.server.providers.AuthException;
import ru.otus.java.basic.project.server.providers.db.AppDatabasePool;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.Action;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.User;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.UserActivity;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.UserRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserDAO implements UserService {
    public static final Logger logger = LoggerFactory.getLogger(UserDAO.class.getName());

    public static final String AUTH_USER_QUERY = "SELECT * FROM users WHERE login = ? AND password = ?";
    public static final String FIND_USERS_QUERY = "SELECT * FROM users";
    public static final String FIND_USER_BY_USERNAME_QUERY = "SELECT * FROM users WHERE username = ?";
    public static final String FIND_USER_BY_LOGIN_QUERY = "SELECT * FROM users WHERE login = ?";
    public static final String FIND_USER_BY_ID_QUERY = "SELECT * FROM users WHERE ID = ?";
    public static final String FIND_USER_ROLES_OF_USER_QUERY = """
            SELECT ur.* FROM user_roles ur
            JOIN users_user_roles_rel uurr ON ur.ID = uurr.USER_ROLE_ID
            WHERE uurr.USER_ID = ?
            ORDER BY ur.PRIORITY ASC
            """;
    public static final String FIND_USER_ROLE_QUERY = "SELECT * FROM user_roles WHERE auth_role = ?";
    public static final String FIND_USER_ROLES_QUERY = "SELECT * FROM user_roles ORDER BY PRIORITY DESC";
    public static final String ADD_USER_QUERY = """
            INSERT INTO users (USERNAME, LOGIN, PASSWORD, IS_ACTIVE, REGISTRATION_DATE) 
            VALUES (?, ?, ?, ?, ?)
            """;
    public static final String ADD_USER_ROLE_QUERY = "INSERT INTO user_roles (AUTH_ROLE, DESCRIPTION) VALUES (?, ?)";
    public static final String ADD_USER_ROLE_TO_USER_QUERY = "INSERT INTO users_user_roles_rel (USER_ID, USER_ROLE_ID) VALUES (?, ?)";
    public static final String IS_USER_ROLE_OF_USER_QUERY = """
            SELECT COUNT(*) FROM user_roles ur
            JOIN users_user_roles_rel uurr ON ur.ID = uurr.USER_ROLE_ID
            WHERE uurr.USER_ID = ? AND ur.ID = ?
            """;
    public static final String DELETE_USER_QUERY = "DELETE FROM users WHERE ID = ?";
    public static final String DELETE_USER_ROLE_FROM_USER_QUERY = """
            DELETE FROM users_user_roles_rel
            WHERE USER_ID = ? AND USER_ROLE_ID = ?
            """;
    public static final String UPDATE_USER_QUERY = """
            UPDATE users
            SET USERNAME = ?, LOGIN = ?, PASSWORD = ?, IS_ACTIVE = ?, DEACTIVATION_DATE = ?
            WHERE ID = ?
            """;
    public static final String USERS_ACTIVITY_QUERY = "SELECT * FROM users_activity WHERE USER_ID = ?";
    public static final String ADD_USERS_ACTIVITY_QUERY = "INSERT INTO users_activity (USER_ID, LAST_CONNECT_DATE, IS_ONLINE) VALUES (?, ?, ?)";
    public static final String UPDATE_USERS_ACTIVITY_QUERY = """
            UPDATE users_activity
            SET LAST_CONNECT_DATE = ?, LAST_DISCONNECT_DATE = ?, KICK_DATE = ?, IS_ONLINE = ?
            WHERE USER_ID = ?
            """;
    public static final String AUTHORIZATION_QUERY = """
            SELECT COUNT(*) FROM user_roles ur,
                (SELECT ur2.PRIORITY FROM actions a2
                JOIN user_roles_actions_rel urar2 ON urar2.ACTION_ID = a2.ID
                JOIN user_roles ur2 ON urar2.USER_ROLE_ID = ur2.ID
                WHERE a2.COMMAND = ?) s2
            WHERE ur.ID = ? and ur.PRIORITY <= s2.PRIORITY;
            """;
    public static final String FIND_USER_ACTIONS_QUERY = """
            SELECT a.* FROM actions a
            JOIN user_roles_actions_rel urar ON urar.ACTION_ID = a.ID
            JOIN user_roles ur ON ur.ID = urar.USER_ROLE_ID
            WHERE ur.PRIORITY >=
                (SELECT ur2.PRIORITY FROM user_roles ur2
                JOIN users_user_roles_rel uurr2 ON uurr2.USER_ROLE_ID = ur2.ID
                JOIN users u2 ON u2.ID = uurr2.USER_ID
                WHERE u2.ID = ?) 
            ORDER BY a.COMMAND
            """;

    private Server server;
    private AppDatabasePool pool;

    public UserDAO(Server server) {
        this.server = server;
        initialize();
    }

    @Override
    public void initialize() {
        pool = server.getPool();
        if (pool == null) {
            throw new AppException("Пул подключений не инициализирован");
        }
    }

    @Override
    public void authenticate(ClientHandler clientHandler, String login, String password) throws AuthException {
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(AUTH_USER_QUERY)) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                List<User> users = readUsers(rs);
                if (users.isEmpty()) {
                    throw new AuthException("некорретный логин/пароль");
                }
                User user = users.get(0);
                if (!user.isActive()) {
                    throw new AuthException("учетная запись отключена");
                }
                if (server.isUsernameBusy(user.getUsername())) {
                    throw new AuthException("учетная запись занята");
                }
                clientHandler.setUser(user);
                server.subscribe(clientHandler);
            }
        } catch (SQLException e) {
            logger.error("Ошибка доступа к базе данных", e);
            throw new OperationException("Ошибка доступа к базе данных");
        }
    }

    @Override
    public void registration(ClientHandler clientHandler, String login, String password, String username, String email, String phoneNumber) throws AuthException {
        if (login.trim().length() < 3 || password.trim().length() < 6 || username.trim().length() < 1) {
            throw new AuthException("Логин 3+ символа, Пароль 6+ символов, Имя пользователя 1+ символ");
        }
        if (getUser(username, false) != null) {
            throw new OperationException("имя пользователя занято");
        }
        if (getUser(login, true) != null) {
            throw new OperationException("логин занят");
        }
        User user = new User(username, login, password, true, new Date());
        try {
            addUser(user);
        } catch (OperationException bre) {
            throw new AuthException("ошибка создания профиля");
        }
        if (user.getId() != null) {
            clientHandler.setUser(user);
            server.subscribe(clientHandler);
        } else {
            throw new AuthException("ошибка создания профиля");
        }
    }

    @Override
    public boolean authorization(ClientHandler clientHandler, String command) {
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(AUTHORIZATION_QUERY)) {
            preparedStatement.setString(1, command);
            User user = clientHandler.getUser();
            if (user != null) {
                List<UserRole> userRoles = user.getUserRoles();
                //если у пользователя есть группы, то извлекаем с наивысшем приоритетом, иначе используем группу с наименьшим доступом
                preparedStatement.setInt(2, user.getUserRoles() != null ? userRoles.get(0).getId() : getUserRoles().get(0).getId());
            } else {
                preparedStatement.setInt(2, getUserRole("user").getId());
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int flag = resultSet.getInt(1);
                    if (flag >= 1) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка доступа к базе данных", e);
            throw new OperationException("Ошибка доступа к базе данных");
        }
        return false;
    }

    private List<User> readUsers(ResultSet resultSet) throws SQLException {
        List<User> users = new ArrayList<>();
        while (resultSet.next()) {
            int id = resultSet.getInt("id");
            String username = resultSet.getString("username");
            String login = resultSet.getString("login");
            String password = resultSet.getString("password");
            boolean isActive = resultSet.getBoolean("is_active");
            Date registrationDate = resultSet.getTimestamp("registration_date");
            Date deactivationDate = resultSet.getTimestamp("deactivation_date");

            User user = new User(id, username, login, password, isActive, registrationDate, deactivationDate);
            List<UserRole> userRoles = getUserRolesOfUser(user);
            user.setUserRoles(userRoles);

            UserActivity userActivity = getUserActivity(user);
            user.setActivity(userActivity);

            users.add(user);
        }
        return users;
    }

    @Override
    public List<Action> getUserActions(User user) {
        List<Action> actions = new ArrayList<>();
        try(Connection connection = pool.getConnection();
            PreparedStatement statement = connection.prepareStatement(FIND_USER_ACTIONS_QUERY)) {
            statement.setInt(1, user.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String command = resultSet.getString("command");
                    String description = resultSet.getString("description");
                    Action action = new Action(id, command, description);
                    actions.add(action);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return actions;
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try (Connection connection = pool.getConnection();
             Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(FIND_USERS_QUERY)) {
                users.addAll(readUsers(rs));
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        for (User user : users) {
            List<UserRole> userRoles = getUserRolesOfUser(user);
            user.setUserRoles(userRoles);

            UserActivity userActivity = getUserActivity(user);
            user.setActivity(userActivity);
        }
        return users;
    }

    @Override
    public User getUser(String name, boolean byLogin) {
        User user = null;
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(byLogin ? FIND_USER_BY_LOGIN_QUERY : FIND_USER_BY_USERNAME_QUERY)) {
            preparedStatement.setString(1, name);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                List<User> users = readUsers(rs);
                if (users.size() == 1) {
                    user = users.get(0);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return user;
    }

    @Override
    public User getUser(int userId) {
        User user = null;
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(FIND_USER_BY_ID_QUERY)) {
            preparedStatement.setInt(1, userId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                List<User> users = readUsers(rs);
                if (users.size() == 1) {
                    user = users.get(0);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return user;
    }

    @Override
    public User addUser(User user) {
        checkUserObject(user, false);
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_USER_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getLogin());
            preparedStatement.setString(3, user.getPassword());
            preparedStatement.setBoolean(4, user.isActive());
            preparedStatement.setTimestamp(5, new Timestamp(new Date().getTime()));
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            } else {
                logger.warn("Ошибка добавления объекта в БД - не присвоен id, {}", user);
                throw new OperationException("Ошибка добавления объекта user");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return user;
    }

    @Override
    public User updateUser(User user, boolean updateUserRoles) {
        checkUserObject(user, true);
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USER_QUERY)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getLogin());
            preparedStatement.setString(3, user.getPassword());
            preparedStatement.setBoolean(4, user.isActive());
            preparedStatement.setTimestamp(5, user.getDeactivationDate() != null ? new Timestamp(user.getDeactivationDate().getTime()) : null);
            preparedStatement.setInt(6, user.getId());
            int flag = preparedStatement.executeUpdate();
            if (flag != 1) {
                logger.warn("Ошибка обновления объекта в БД, {}", user);
                throw new OperationException("Ошибка обновления user");
            }
            if (updateUserRoles) {
                List<UserRole> localUserRoles = user.getUserRoles();
                if (localUserRoles != null || localUserRoles.isEmpty()) {
                    List<UserRole> dbUserRoles = getUserRolesOfUser(user);
                    for (UserRole userRole : localUserRoles) {
                        if (!dbUserRoles.contains(userRole)) {
                            addUserRoleToUser(user, userRole);
                        }
                    }
                    for (UserRole userRole : dbUserRoles) {
                        if (!localUserRoles.contains(userRole)) {
                            removeUserRoleFromUser(user, userRole);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return user;
    }

    public void deactivateUser(User user) {
        checkUserObject(user, true);
        user.setActive(false);
        updateUser(user, false);
    }

    public void activateUser(User user) {
        checkUserObject(user, true);
        user.setActive(true);
        updateUser(user, false);
    }

    @Override
    public boolean deleteUser(User user) {
        checkUserObject(user, true);
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_USER_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            int flag = preparedStatement.executeUpdate();
            return flag == 1;
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
    }

    @Override
    public UserRole getUserRole(String userRoleName) {
        UserRole userRole = null;
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(FIND_USER_ROLE_QUERY)) {
            preparedStatement.setString(1, userRoleName);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String authRole = rs.getString("auth_role");
                    String description = rs.getString("description");
                    int priority = rs.getInt("priority");
                    userRole = new UserRole(id, authRole, description, priority);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return userRole;
    }

    @Override
    public List<UserRole> getUserRoles() {
        List<UserRole> userRoles = new ArrayList<>();
        try (Connection connection = pool.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(FIND_USER_ROLES_QUERY)) {
                userRoles.addAll(readUserRoles(rs));
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return userRoles;
    }

    @Override
    public List<UserRole> getUserRolesOfUser(User user) {
        checkUserObject(user, true);
        List<UserRole> userRoles = new ArrayList<>();
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(FIND_USER_ROLES_OF_USER_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            try (ResultSet rs = preparedStatement.executeQuery()) {
                userRoles.addAll(readUserRoles(rs));
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return userRoles;
    }

    private List<UserRole> readUserRoles(ResultSet resultSet) throws SQLException {
        List<UserRole> userRoles = new ArrayList<>();
        while (resultSet.next()) {
            int id = resultSet.getInt("id");
            String authRole = resultSet.getString("auth_role");
            String description = resultSet.getString("description");
            int priority = resultSet.getInt("priority");
            UserRole userRole = new UserRole(id, authRole, description, priority);
            userRoles.add(userRole);
        }
        return userRoles;
    }

    @Override
    public UserRole addUserRole(UserRole userRole) {
        checkUserRoleObject(userRole, false);
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_USER_ROLE_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, userRole.getAuthRole());
            preparedStatement.setString(2, userRole.getDescription());
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) {
                userRole.setId(rs.getInt(1));
            } else {
                logger.warn("Ошибка добавления объекта в БД - не присвоен id, {}", userRole);
                throw new OperationException("Ошибка добавления userRole");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return userRole;
    }

    @Override
    public boolean isUserRoleOfUser(User user, UserRole userRole) {
        checkUserObject(user, true);
        checkUserRoleObject(userRole, true);
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(IS_USER_ROLE_OF_USER_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            preparedStatement.setInt(2, userRole.getId());
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    int flag = rs.getInt(1);
                    return flag == 1;
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return false;
    }

    @Override
    public UserActivity getUserActivity(User user) {
        checkUserObject(user, true);
        UserActivity userActivity = null;
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(USERS_ACTIVITY_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    int userId = rs.getInt("user_id");
                    Date lastConnectDate = rs.getTimestamp("last_connect_date");
                    Date lastDisconnectDate = rs.getTimestamp("last_disconnect_date");
                    Date kickDate = rs.getTimestamp("kick_date");
                    boolean isOnline = rs.getBoolean("is_online");
                    userActivity = new UserActivity(id, userId, lastConnectDate, lastDisconnectDate, kickDate, isOnline);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return userActivity;
    }

    @Override
    public UserActivity addUserActivity(UserActivity userActivity) {
        checkUserActivityObject(userActivity, false);
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_USERS_ACTIVITY_QUERY, PreparedStatement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, userActivity.getUserId());
            preparedStatement.setTimestamp(2, new Timestamp(userActivity.getLastConnectDate().getTime()));
            preparedStatement.setBoolean(3, userActivity.isOnline());
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                userActivity.setId(resultSet.getInt(1));
            } else {
                logger.warn("Ошибка добавления объекта в БД - не присвоен id, {}", userActivity);
                throw new OperationException("Ошибка добавления userActivity");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return userActivity;
    }

    @Override
    public UserActivity updateUserActivity(UserActivity userActivity) {
        checkUserActivityObject(userActivity, true);
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USERS_ACTIVITY_QUERY)) {
            preparedStatement.setTimestamp(1, userActivity.getLastConnectDate() != null ? new Timestamp(userActivity.getLastConnectDate().getTime()) : null);
            preparedStatement.setTimestamp(2, userActivity.getLastDisconnectDate() != null ? new Timestamp(userActivity.getLastDisconnectDate().getTime()) : null);
            preparedStatement.setTimestamp(3, userActivity.getKickDate() != null ? new Timestamp(userActivity.getKickDate().getTime()) : null);
            preparedStatement.setBoolean(4, userActivity.isOnline());
            preparedStatement.setInt(5, userActivity.getUserId());
            int flag = preparedStatement.executeUpdate();
            if (flag != 1) {
                logger.warn("Ошибка обновления объекта в БД, {}", userActivity);
                throw new OperationException("Ошибка обновления userActivity");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return userActivity;
    }

    @Override
    public User addUserRoleToUser(User user, UserRole userRole) {
        checkUserObject(user, true);
        checkUserRoleObject(userRole, true);
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_USER_ROLE_TO_USER_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            preparedStatement.setInt(2, userRole.getId());
            int flag = preparedStatement.executeUpdate();
            if (flag == 1) {
                user.setUserRoles(getUserRolesOfUser(user));
            } else {
                logger.warn("Ошибка добавления роли пользователю, user={}, userRole={}", user, userRole);
                throw new OperationException("Роль " + userRole.getAuthRole() + " не обнаружена");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return user;
    }

    @Override
    public User removeUserRoleFromUser(User user, UserRole userRole) {
        checkUserObject(user, true);
        checkUserRoleObject(userRole, true);
        try (Connection connection = pool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_USER_ROLE_FROM_USER_QUERY)) {
            preparedStatement.setInt(1, user.getId());
            preparedStatement.setInt(2, userRole.getId());
            int flag = preparedStatement.executeUpdate();
            if (flag == 1) {
                user.setUserRoles(getUserRolesOfUser(user));
            } else {
                logger.warn("Ошибка удаления роли пользователя, user={}, userRole={}", user, userRole);
                throw new OperationException("Роль " + userRole.getAuthRole() + " не обнаружена");
            }
        } catch (SQLException e) {
            logger.error("Ошибка запроса к БД", e);
            throw new OperationException("Ошибка запроса к БД");
        }
        return user;
    }

    @Override
    public User connectUser(User user) {
        checkUserObject(user, true);
        if (user.getActivity() == null) {
            UserActivity userActivity = new UserActivity(user.getId(), new Date(), null, null, true);
            addUserActivity(userActivity);
            user.setActivity(userActivity);
        }
        UserActivity userActivity = user.getActivity();
        userActivity.setLastConnectDate(new Date());
        userActivity.setOnline(true);
        updateUserActivity(userActivity);
        user.setActivity(userActivity);
        return user;
    }

    @Override
    public User disconnectUser(User user, boolean isKicked) {
        checkUserObject(user, true);
        UserActivity userActivity = user.getActivity();
        if (userActivity == null) {
            logger.warn("Ошибка обвноления журнала активности, userActivity is null {}, ", user);
            userActivity = new UserActivity(user.getId(), null, null, null, false);
        }
        userActivity.setLastDisconnectDate(!isKicked ?
                new Timestamp(new Date().getTime()) :
                userActivity.getLastDisconnectDate() != null ?
                        new Timestamp(userActivity.getLastDisconnectDate().getTime()) :
                        null);
        userActivity.setKickDate(isKicked ?
                new Timestamp(new Date().getTime()) :
                userActivity.getKickDate() != null ?
                        new Timestamp(userActivity.getKickDate().getTime()) :
                        null);
        userActivity.setOnline(false);
        updateUserActivity(userActivity);
        user.setActivity(userActivity);
        return user;
    }

    private void checkUserObject(User user, boolean sync) {
        if (user == null) {
            logger.warn("Объект user не инициализирован, user is null");
            throw new AppException("user is null");
        }
        if (sync && user.getId() == null) {
            logger.warn("Объект user не синхронизирован c БД, user id is null, {}", user);
            throw new AppException("user id is null");
        }
    }

    private void checkUserRoleObject(UserRole userRole, boolean sync) {
        if (userRole == null) {
            logger.warn("Объект userRole не инициализирован, userRole is null");
            throw new AppException("userRole is null");
        }
        if (sync && userRole.getId() == null) {
            logger.warn("Объект userRole не синхронизирован c БД, userRole id is null, {}", userRole);
            throw new AppException("userRole id is null");
        }
    }

    private void checkUserActivityObject(UserActivity userActivity, boolean sync) {
        if (userActivity == null) {
            logger.warn("Объект userActivity не инициализирован, userActivity is null");
            throw new AppException("userActivity is null");
        }
        if (sync && userActivity.getId() == null) {
            logger.warn("Объект userActivity не синхронизирован c БД, userActivity id is null, {}", userActivity);
            throw new AppException("userActivity id is null");
        }
    }
}
