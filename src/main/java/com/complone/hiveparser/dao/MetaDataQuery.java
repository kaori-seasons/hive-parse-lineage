package com.complone.hiveparser.dao;

import com.complone.hiveparser.conf.DatabaseConfInfo;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

/**
 * 根据数据库和表去查所有字段的列名
 */
public interface MetaDataQuery {
    List<String>  getColumnByDBAndTable(DataSource dataSource, DatabaseConfInfo databaseConfInfo, String database, String table) throws SQLException;
}
