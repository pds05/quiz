package ru.otus.java.basic.project.server.providers.db.jdbc.entity;

import java.util.Objects;

public class UserRole {
    private Integer id;
    private String authRole;
    private String description;
    private int priority;

    public UserRole(Integer id, String authRole, String description, int priority) {
        this.id = id;
        this.authRole = authRole;
        this.description = description;
        this.priority = priority;
    }

    public UserRole(String authRole, String description, int priority) {
        this.authRole = authRole;
        this.description = description;
        this.priority = priority;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAuthRole() {
        return authRole;
    }

    public void setAuthRole(String authRole) {
        this.authRole = authRole;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "id=" + id +
                ", authRole='" + authRole + '\'' +
                ", description='" + description + '\'' +
                ", priority=" + priority +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRole userRole = (UserRole) o;
        return priority == userRole.priority && Objects.equals(id, userRole.id) && Objects.equals(authRole, userRole.authRole) && Objects.equals(description, userRole.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, authRole, description, priority);
    }
}
