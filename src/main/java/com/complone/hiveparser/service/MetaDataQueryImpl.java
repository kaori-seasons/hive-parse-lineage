package com.complone.hiveparser.service;

import com.complone.hiveparser.dao.MetaDataQuery;
import com.complone.hiveparser.type.DatabaseType;
import com.complone.hiveparser.type.DatabaseTypeFactory;
import com.complone.hiveparser.utils.DataSourceUtils;
import com.zaxxer.hikari.HikariDataSource;
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
    @Override
    public List<String> getColumnByDBAndTable(Class<? extends DataSource> dataSource, String databaseType, String database, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        
        DatabaseType type = DatabaseTypeFactory.getInstance(databaseType);
        DataSource source = DataSourceUtils.build(dataSource,type,database);
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
