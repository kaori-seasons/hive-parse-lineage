package com.complone.hiveparser.controller;

import com.complone.hiveparser.LineParser;
import com.complone.hiveparser.conf.DatabaseConfInfo;
import com.complone.hiveparser.entity.ColLine;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Controller
public class ParseController {
    
    @RequestMapping(value = "/fetch")
    public List<ColLine> fetchDataSource(@RequestBody String sql) throws SQLException {
        DataSource dataSource = new HikariDataSource();
        DatabaseConfInfo databaseConfInfo = new DatabaseConfInfo();
        databaseConfInfo.setSrcDataSource("ds1");
        databaseConfInfo.setDatabaseType("MySQL");
        databaseConfInfo.setDestDataSource("ds2");
        LineParser line = new LineParser(dataSource, databaseConfInfo);
        line.parse(sql,true);
        return line.getColLines();
    }
}
