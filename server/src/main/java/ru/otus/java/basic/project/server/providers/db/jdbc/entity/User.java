package ru.otus.java.basic.project.server.providers.db.jdbc.entity;

import java.util.Date;
import java.util.List;

public class User {
    private Integer id;
    private String username;
    private String login;
    private String password;
    private boolean isActive;
    private Date registrationDate;
    private Date deactivationDate;
    private List<UserRole> userRoles;
    private UserActivity activity;
    private List<Quiz> quizzes;

    public User(Integer id, String username, String login, String password, boolean isActive, Date registrationDate, Date deactivationDate) {
        this.id = id;
        this.username = username;
        this.login = login;
        this.password = password;
        this.isActive = isActive;
        this.registrationDate = registrationDate;
        this.deactivationDate = deactivationDate;
    }

    public User(String username, String login, String password, boolean isActive, Date registrationDate) {
        this.username = username;
        this.login = login;
        this.password = password;
        this.isActive = isActive;
        this.registrationDate = registrationDate;
    }

    public User(){};

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Date getDeactivationDate() {
        return deactivationDate;
    }

    public void setDeactivationDate(Date deactivationDate) {
        this.deactivationDate = deactivationDate;
    }

    public List<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<UserRole> userRoles) {
        this.userRoles = userRoles;
    }

    public UserActivity getActivity() {
        return activity;
    }

    public void setActivity(UserActivity activity) {
        this.activity = activity;
    }

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Quiz> quizzes) {
        this.quizzes = quizzes;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", isActive=" + isActive +
                ", registrationDate=" + registrationDate +
                ", deactivationDate=" + deactivationDate +
                ", userRoles=" + userRoles +
                ", activity=" + activity +
                ", quizzes=" + quizzes +
                '}';
    }
}
