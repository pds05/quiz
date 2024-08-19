package ru.otus.java.basic.project.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerApplication {
    public static final Logger logger = LoggerFactory.getLogger(ServerApplication.class.getName());
    public static final int PORT = 8189;

    public static void main(String[] args) {
        Server server = new Server(PORT);
        server.start();
    }
}
