package org.metro.dao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.sqlite.SQLiteConfig;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import javax.sql.DataSource;

/**
 * <p> Created by pengshuolin on 2019/6/8
 */
@Configuration
@EnableTransactionManagement
@MapperScan("org.metro.dao.mapper")
public class DBConfig {

    @Value("${jdbc.url}")
    private String url;

    @Value("${jdbc.busyTimeout}")
    private int busyTimeout;

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        /*
         * cmd: sqlite3 file_name (https://www.sqlite.org/cli.html)
         * gui: SqliteStudio (https://github.com/pawelsalawa/sqlitestudio/wiki/User_Manual)
         */
        SQLiteConfig config = new SQLiteConfig();
        config.setBusyTimeout(busyTimeout);
        config.setEncoding(SQLiteConfig.Encoding.UTF8);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);

        SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
        dataSource.setUrl(url);
        dataSource.setConfig(config);
        return dataSource;
    }

    public JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager annotationDrivenTransactionManager(@Autowired DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
