package ru.otus.java.basic.project.server.providers.db.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.java.basic.project.server.AppException;
import ru.otus.java.basic.project.server.providers.db.AppDatabasePool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DriverManagerConnectionPool implements AppDatabasePool {
    public static final Logger logger = LoggerFactory.getLogger(DriverManagerConnectionPool.class.getName());
    private String url;

    private Connection connection;

    public DriverManagerConnectionPool(String url) {
        logger.debug("Инициализация модуля {}...", this.getClass().getName());
        this.url = url;
        initialize();
        logger.debug("Инициализация модуля {} завершена", this.getClass().getName());
    }

    @Override
    public void initialize() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(url, "chat_admin", "12345678");
            } catch (SQLException e) {
                throw new AppException("Ошибка подключения к базе данных", e);
            }
        }
    }

    @Override
    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new AppException("Ошибка закрытия подключения к БД");
            }
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }
}
