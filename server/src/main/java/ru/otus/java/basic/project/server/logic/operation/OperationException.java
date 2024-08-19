package ru.otus.java.basic.project.server.logic.operation;

import ru.otus.java.basic.project.server.AppException;

public class OperationException extends AppException {

    public OperationException(String messsage) {
        super(messsage);
    }
}
