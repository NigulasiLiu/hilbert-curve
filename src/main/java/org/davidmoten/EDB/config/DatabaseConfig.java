package org.davidmoten.EDB.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DatabaseConfig {
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/dsse");
        config.setUsername("root");
        config.setPassword("liubq");
        config.setMaximumPoolSize(20);  // 设置最大连接数
        config.setConnectionTimeout(30000);  // 超时时间
        config.setIdleTimeout(600000);  // 空闲连接最大存活时间
        config.setMaxLifetime(1800000);  // 连接池中的连接最大存活时间

        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
