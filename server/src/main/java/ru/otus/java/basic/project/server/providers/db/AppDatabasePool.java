package ru.otus.java.basic.project.server.providers.db;

import ru.otus.java.basic.project.server.AppModule;

import java.sql.Connection;

public interface AppDatabasePool extends AppModule {

    Connection getConnection();
}
