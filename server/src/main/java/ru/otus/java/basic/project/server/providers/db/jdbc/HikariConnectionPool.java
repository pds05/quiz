package ru.otus.java.basic.project.server.providers.db.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.java.basic.project.server.AppException;
import ru.otus.java.basic.project.server.providers.db.AppDatabasePool;

import java.sql.Connection;
import java.sql.SQLException;

public class HikariConnectionPool implements AppDatabasePool {
    public static final Logger logger = LoggerFactory.getLogger(HikariConnectionPool.class.getName());
    public static final int CONNECTION_POOL_SIZE = 10;

    private static HikariDataSource dataSource;
    private static HikariConfig config;
    private String url;

    public HikariConnectionPool(String url) {
        this.url = url;
    }

    @Override
    public Connection getConnection() {
        Connection connection;
        try {
            connection = dataSource.getConnection();

        } catch (SQLException e) {
            throw new AppException("Отсутсвует подключение к базе данных", e);
        }
        return connection;
    }

    @Override
    public void initialize() {
        logger.debug("Инициализация пула подключений к базе данных");
        config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername("chat_admin");
        config.setPassword("12345678");
        config.setMaximumPoolSize(CONNECTION_POOL_SIZE);
        config.setConnectionTimeout(10000);
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        dataSource = new HikariDataSource(config);
        logger.debug("Пул подключений запущен, количество активных подключений = " + dataSource.getMaximumPoolSize());
    }

    @Override
    public void shutdown() {
        logger.debug("Остановка пула подключений...");
        if (dataSource != null) {
            dataSource.close();
            logger.debug("Пул подключений остановлен");
        }
    }
}
