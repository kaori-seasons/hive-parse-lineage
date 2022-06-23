package com.complone.hiveparser.entity;

import lombok.Data;

import java.util.Set;


/**
 * 字段级别血缘
 */
public class ColLineParse {
    
    private String toTable; //处理结果主表
    
    private String toNameParse; //目标表
    
    private String fromName; //源表
    
    private Set<String> conditionSet; //过滤条件
    
    public ColLineParse(String toTable, String toNameParse, String fromName, Set<String> conditionSet) {
        this.toTable = toTable;
        this.toNameParse = toNameParse;
        this.fromName = fromName;
        this.conditionSet = conditionSet;
    }
    
    public String getToTable() {
        return toTable;
    }
    
    public void setToTable(String table) {
        this.toTable = table;
    }
    
    public String getToNameParse() {
        return toNameParse;
    }
    
    public void setToNameParse(String toNameParse) {
        this.toNameParse = toNameParse;
    }
    
    public String getFromName() {
        return fromName;
    }
    
    public void addFromName(String fromName) {
        this.fromName = fromName;
    }
    
    public Set<String> getConditionSet() {
        return conditionSet;
    }
    
    public void addConditionSet(Set<String> conditionSet) {
        this.conditionSet = conditionSet;
    }
}
