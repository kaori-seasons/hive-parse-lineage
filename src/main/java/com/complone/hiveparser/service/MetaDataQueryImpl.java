package com.complone.hiveparser.service;

import com.complone.hiveparser.conf.DatabaseConfInfo;
import com.complone.hiveparser.conf.UserConfiguarion;
import com.complone.hiveparser.dao.MetaDataQuery;
import com.complone.hiveparser.type.DatabaseType;
import com.complone.hiveparser.type.DatabaseTypeFactory;
import com.complone.hiveparser.utils.DataSourceUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MetaDataQueryImpl implements MetaDataQuery {
    
    @Autowired
    private UserConfiguarion userConfiguarion;
    
    @Override
    public List<String> getColumnByDBAndTable(DataSource dataSource, DatabaseConfInfo databaseConfInfo, String database, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        String databaseType = databaseConfInfo.getDatabaseType();
        DatabaseType type = DatabaseTypeFactory.getInstance(databaseType);
        Class<? extends DataSource> dataSourceClass = dataSource.getClass();
        DataSource source = DataSourceUtils.build(dataSourceClass, type, database, databaseConfInfo, userConfiguarion);
        Connection connection = source.getConnection();
        DatabaseMetaData dbMetaData = connection.getMetaData();
        ResultSet allColumns = dbMetaData.getColumns(null, "%", table, "%");
        
        while (allColumns.next()){
            String colName = allColumns.getString("COLUMN_NAME");
            columns.add(colName);
        }
        return columns;
    }
}
