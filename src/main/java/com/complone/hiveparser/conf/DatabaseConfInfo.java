package com.complone.hiveparser.conf;

import lombok.Data;

import java.util.List;

@Data
public class DatabaseConfInfo {
    
    private String databaseType;
    
    /**
     * 是否是单库查询数据源
     */
    private boolean isPrimary;
    
    /**
     * 如果不是跨库数据同步，则使用单库数据同步
     */
    private String srcDataSource;
    
    /**
     * 如果是多库查询 则需要指定多个数据源
     */
    private List<String> srcDataSourceList;
    
    /**
     * 最终汇聚到的目标数据源
     */
    private String destDataSource;
}
