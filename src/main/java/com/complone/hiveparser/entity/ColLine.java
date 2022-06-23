package com.complone.hiveparser.entity;

import lombok.Data;

import java.util.Set;

/**
 *
 * 字段级别血缘
 */
@Data
public class ColLine {
    
    private String tableName;
    
    private String colName;
    
    private String toName; //源字段
    
    private String fromName; //目标字段
    
    private Set<String> conditionSet;
    
    public ColLine(String tableName, String colName, String toName, String fromName, Set<String> conditionSet) {
        this.tableName = tableName;
        this.colName = colName;
        this.toName = toName;
        this.fromName = fromName;
        this.conditionSet = conditionSet;
    }
}
