package com.complone.hiveparser.conf;

import com.complone.hiveparser.conf.druid.DruidDataConfig;
import com.complone.hiveparser.conf.hikari.HikariDataConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "spring.datasources.druid")
@Component
@Data
public class DruidDataSourceManager extends DataSourceConfig{
    
    private String type;
    
    private List<DruidDataConfig> druidDataConfigs;
}
