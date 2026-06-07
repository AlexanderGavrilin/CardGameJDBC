package com.cardgame.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Менеджер подключений к PostgreSQL на базе HikariCP.
 * Настройки берутся из application.properties через {@link AppConfig}.
 * Каждое полученное соединение использует схему card_game (search_path).
 */
public final class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
    private static volatile HikariDataSource dataSource;

    private ConnectionManager() {}

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (ConnectionManager.class) {
                if (dataSource == null) {
                    dataSource = createDataSource();
                    log.info("HikariCP connection pool инициализирован");
                }
            }
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static String schema() {
        return AppConfig.get("db.schema", "card_game");
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("HikariCP connection pool закрыт");
        }
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(AppConfig.get("db.url"));
        config.setUsername(AppConfig.get("db.username"));
        config.setPassword(AppConfig.get("db.password"));
        config.setMaximumPoolSize(AppConfig.getInt("db.pool.maximumPoolSize", 10));
        config.setMinimumIdle(AppConfig.getInt("db.pool.minimumIdle", 2));
        config.setConnectionTimeout(AppConfig.getLong("db.pool.connectionTimeout", 30000));
        config.setIdleTimeout(AppConfig.getLong("db.pool.idleTimeout", 600000));
        config.setMaxLifetime(AppConfig.getLong("db.pool.maxLifetime", 1800000));
        // Все соединения сразу видят таблицы схемы card_game
        config.setConnectionInitSql("SET search_path TO " + schema());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.setPoolName("CardGamePool");
        return new HikariDataSource(config);
    }
}
