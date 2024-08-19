package ru.otus.java.basic.project.server.providers.db.jdbc.entity;

public class Action {
    private Integer id;
    public String command;
    public String description;

    public Action(String command, String description) {
        this.command = command;
        this.description = description;
    }

    public Action(Integer id, String command, String description) {
        this.id = id;
        this.command = command;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
