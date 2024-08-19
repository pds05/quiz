package ru.otus.java.basic.project.server;

public class AppException extends RuntimeException {

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppException(String message) {
        super(message);
    }
}
