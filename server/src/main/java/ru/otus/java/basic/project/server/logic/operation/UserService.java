package ru.otus.java.basic.project.server.logic.operation;

import ru.otus.java.basic.project.server.providers.AuthenticationProvider;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.Action;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.User;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.UserActivity;
import ru.otus.java.basic.project.server.providers.db.jdbc.entity.UserRole;

import java.util.List;

public interface UserService extends AuthenticationProvider {

    List<Action> getUserActions(User user);

    List<User> getAllUsers();

    User getUser(String name, boolean byLogin);

    User getUser(int userId);

    User addUser(User user);

    User updateUser(User user, boolean updateUserRoles);

    boolean deleteUser(User user);

    UserRole getUserRole(String userRoleName);

    List<UserRole> getUserRoles();

    UserRole addUserRole(UserRole userRole);

    List<UserRole> getUserRolesOfUser(User user);

    boolean isUserRoleOfUser(User user, UserRole userRole);

    User addUserRoleToUser(User user, UserRole userRole);

    User removeUserRoleFromUser(User user, UserRole userRole);

    UserActivity getUserActivity(User user);

    UserActivity addUserActivity(UserActivity userActivity);

    UserActivity updateUserActivity(UserActivity userActivity);

    User connectUser(User user);

    User disconnectUser(User user, boolean isKicked);
}
