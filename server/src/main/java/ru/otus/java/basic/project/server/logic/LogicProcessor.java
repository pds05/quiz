package ru.otus.java.basic.project.server.logic;

import ru.otus.java.basic.project.server.ClientHandler;
import ru.otus.java.basic.project.server.Server;

public interface LogicProcessor {
    void process(Server server, ClientHandler clientHandler, String inputMessage);
}
