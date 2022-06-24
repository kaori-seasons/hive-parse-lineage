package com.complone.hiveparser.utils;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.complone.hiveparser.conf.DatabaseConfInfo;
import com.complone.hiveparser.conf.DruidDataSourceManager;
import com.complone.hiveparser.conf.HikariDataSourceManager;
import com.complone.hiveparser.conf.UserConfiguarion;
import com.complone.hiveparser.conf.druid.DruidDataConfig;
import com.complone.hiveparser.conf.hikari.HikariDataConfig;
import com.complone.hiveparser.datasource.AbstractRoutingDataSource;
import com.complone.hiveparser.datasource.mock.MockedDataSource;
import com.complone.hiveparser.exeption.DataSourceNotFoundException;
import com.complone.hiveparser.type.DatabaseType;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.collections.CollectionUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;

public class DataSourceUtils {
    
    public static DataSource build(final Class<? extends DataSource> dataSourceClass, final DatabaseType databaseType, final String databaseName, final DatabaseConfInfo databaseConfInfo, final UserConfiguarion userConfiguarion) {
            try {
                return createMultiDataSource(dataSourceClass, databaseType, databaseName, databaseConfInfo, userConfiguarion);
            }catch (UnsupportedOperationException ex){
                throw new UnsupportedOperationException(dataSourceClass.getName());
            }
    }
    
    
    private static DataSource createMultiDataSource(final Class<? extends DataSource> dataSourceClass, final DatabaseType databaseType, final String databaseName, final DatabaseConfInfo databaseConfInfo, final UserConfiguarion userConf){
        DynamicRoutingDataSource dynamicRoutingDataSource = new AbstractRoutingDataSource();
        if (HikariDataSource.class ==  dataSourceClass){
            HikariDataSourceManager hikariManager = userConf.getHikari();
            List<HikariDataConfig> hikariDataConfigs = hikariManager.getHikariDataConfigs();
            for (HikariDataConfig config: hikariDataConfigs){
                HikariDataSource hikariDataSource = createHikariDataSource(databaseType,databaseName,config);
                if (Objects.nonNull(dynamicRoutingDataSource.getDataSource(config.getId()))) {
                    throw new DataSourceNotFoundException(404,"创建数据源失败");
                }
                dynamicRoutingDataSource.addDataSource(config.getId(), hikariDataSource);
            }
            fetchMultiDataSource(databaseConfInfo);
            
            return dynamicRoutingDataSource.getDataSource(databaseConfInfo.getSrcDataSource());
        }else if (DruidDataSource.class == dataSourceClass) {
            DruidDataSourceManager druidDataSourceManager = userConf.getDruid();
            List<DruidDataConfig> druidDataConfigs = druidDataSourceManager.getDruidDataConfigs();
            for (DruidDataConfig config: druidDataConfigs){
                DruidDataSource druidDataSource = createDruidDataSource(databaseType,databaseName,config);
                dynamicRoutingDataSource.addDataSource(config.getId(), druidDataSource);
            }
            fetchMultiDataSource(databaseConfInfo);
            return dynamicRoutingDataSource.getDataSource(databaseConfInfo.getSrcDataSource());
        }else {
            return new MockedDataSource();
        }
        
    }
    
    public static DataSource  fetchMultiDataSource(DatabaseConfInfo databaseConfInfo){
        if ( databaseConfInfo.isPrimary() && CollectionUtils.isNotEmpty(databaseConfInfo.getSrcDataSourceList())) {
            return fetchDataSyncConf(databaseConfInfo);
        }
        return null;
    }
    
    /**
     * 在多个数据源合并导入一个目标数据源的时候 需要解析正确库的schema格式
     * @param databaseConfInfo
     * @return
     */
    private static DataSource fetchDataSyncConf(DatabaseConfInfo databaseConfInfo){
        return new HikariDataSource();
    }
    
    private static DruidDataSource createDruidDataSource(final DatabaseType databaseType, final String databaseName, final DruidDataConfig dataSourceConfig) {
        DruidDataSource result = new DruidDataSource();
        result.setUrl(getURL(databaseType, databaseName));
        result.setDriverClassName(getDriverClassName(databaseType));
        result.setUsername(dataSourceConfig.getUsername());
        result.setPassword(dataSourceConfig.getPassword());
        result.setValidationQuery(dataSourceConfig.getValidationQuery());
        result.setTestOnBorrow(dataSourceConfig.isTestOnBorrow());
        result.setTestOnReturn(dataSourceConfig.isTestOnReturn());
        result.setTestWhileIdle(dataSourceConfig.isTestWhileIdle());
        result.setTimeBetweenEvictionRunsMillis(dataSourceConfig.getTimeBetweenEvictionRunsMillis());
        result.setMaxActive(dataSourceConfig.getMaxActive());
        result.setInitialSize(dataSourceConfig.getInitialSize());
        return result;
    }
    
    
    private static HikariDataSource createHikariDataSource(final DatabaseType databaseType, final String databaseName, final HikariDataConfig hikariDataConfig) {
        HikariDataSource result = new HikariDataSource();
        result.setJdbcUrl(getURL(databaseType, databaseName));
        result.setDriverClassName(getDriverClassName(databaseType));
        result.setUsername(hikariDataConfig.getUsername());
        result.setPassword(hikariDataConfig.getPassword());
        result.setMaximumPoolSize(hikariDataConfig.getMaximumPoolSize());
        result.setMinimumIdle(hikariDataConfig.getMinimumIdle());
        result.setConnectionTimeout(hikariDataConfig.getConnectionTimeout());
        result.setIdleTimeout(hikariDataConfig.getIdleTimeout());
        return result;
    }
    
    public static String getDriverClassName(final DatabaseType databaseType){
        switch (databaseType.getType()){
            case "MySQL":
                return "com.mysql.jdbc.Driver";
            case "PostgreSQL":
                return "org.postgresql.Driver";
            case "Oracle":
                return "oracle.jdbc.driver.OracleDriver";
            default:
                throw new UnsupportedOperationException(databaseType.getType());
        }
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
