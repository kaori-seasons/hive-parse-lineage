package com.complone.hiveparser.utils;

import com.alibaba.druid.pool.DruidDataSource;
import com.complone.hiveparser.type.DatabaseType;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceUtils {
    
    public static DataSource build(final Class<? extends DataSource> dataSourceClass, final DatabaseType databaseType, final String databaseName) {
        if (HikariDataSource.class == dataSourceClass) {
            return createHikariDataSource(databaseType, databaseName);
        }
        if (DruidDataSource.class == dataSourceClass) {
            return createDruidDataSource(databaseType, databaseName);
        }
        throw new UnsupportedOperationException(dataSourceClass.getName());
    }
    
    private static DruidDataSource createDruidDataSource(final DatabaseType databaseType, final String databaseName) {
        DruidDataSource result = new DruidDataSource();
        result.setUrl(getURL(databaseType, databaseName));
        result.setUsername("root");
        result.setPassword("root");
        result.setValidationQuery("SELECT 1");//用来检测连接是否有效
        result.setTestOnBorrow(false);//借用连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能
        result.setTestOnReturn(false);//归还连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能
        //连接空闲时检测，如果连接空闲时间大于timeBetweenEvictionRunsMillis指定的毫秒，执行validationQuery指定的SQL来检测连接是否有效
        result.setTestWhileIdle(true);//如果检测失败，则连接将被从池中去除
        result.setTimeBetweenEvictionRunsMillis(40 * 1000L);//1分钟
        result.setMaxActive(10);
        result.setInitialSize(2);
        return result;
    }
    
    
    private static HikariDataSource createHikariDataSource(final DatabaseType databaseType, final String databaseName) {
        HikariDataSource result = new HikariDataSource();
        result.setJdbcUrl(getURL(databaseType, databaseName));
        result.setUsername("root");
        result.setPassword("root");
        result.setMaximumPoolSize(10);
        result.setMinimumIdle(2);
        result.setConnectionTimeout(15 * 1000L);
        result.setIdleTimeout(40 * 1000L);
        return result;
    }
    
    private static String getURL(final DatabaseType databaseType, final String databaseName) {
        switch (databaseType.getType()) {
            case "MySQL":
                return String.format("jdbc:mysql://localhost:3306/%s", databaseName);
            case "MariaDB":
                return String.format("jdbc:mariadb://localhost:3306/%s", databaseName);
            case "PostgreSQL":
                return String.format("jdbc:postgresql://localhost:5432/%s", databaseName);
            case "openGauss":
                return String.format("jdbc:opengauss://localhost:5431/%s", databaseName);
            case "Oracle":
                return String.format("jdbc:oracle:thin:@//localhost:1521/%s", databaseName);
            case "SQLServer":
                return String.format("jdbc:sqlserver://localhost:1433;DatabaseName=%s", databaseName);
            case "H2":
                return String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MYSQL", databaseName);
            default:
                throw new UnsupportedOperationException(databaseType.getType());
        }
    }
    
}
