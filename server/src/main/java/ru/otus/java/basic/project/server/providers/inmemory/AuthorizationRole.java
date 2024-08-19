package ru.otus.java.basic.project.server.providers.inmemory;

public enum AuthorizationRole {
    ADMIN(0, new String[]{"addrole", "delrole"}),
    MANAGER(1, new String[]{"kick", "deluser", "activate"}),
    USER(2, new String[]{"register", "auth", "w", "exit"});

    AuthorizationRole(int priority, String[] commands) {
        this.priority = priority;
        this.commands = commands;
    }

    private String[] commands;
    private int priority;

    public String[] getCommands() {
        return commands;
    }

    public int getPriority() {
        return priority;
    }

    private static boolean isOwnerAuthorizationRole(AuthorizationRole role, String command) {
        String[] commands = role.getCommands();
        for (int i = 0; i < commands.length; i++) {
            if (commands[i].equals(command)) {
                return true;
            }
        }
        return false;
    }

    public static AuthorizationRole getAuthorizationRole(String command) {
        if (isOwnerAuthorizationRole(ADMIN, command)) {
            return ADMIN;
        } else if (isOwnerAuthorizationRole(MANAGER, command)) {
            return MANAGER;
        } else if (isOwnerAuthorizationRole(USER, command)) {
            return USER;
        } else {
            System.out.println("Неизвестная комманда: " + command);
            return null;
        }
    }
}
