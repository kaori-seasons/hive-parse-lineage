package com.complone.hiveparser.datasource;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class AbstractRoutingDataSource extends DynamicRoutingDataSource {
    
    
    @Override
    public DataSource determineDataSource() {
        
        //TODO 如果是跨库查询的情况 需要通过线程上下文来决定
        // 此时数据同步的目标库和源库
        return super.determineDataSource();
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return determineDataSource().getConnection();
    }
    
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return determineDataSource().getConnection();
    }
}
