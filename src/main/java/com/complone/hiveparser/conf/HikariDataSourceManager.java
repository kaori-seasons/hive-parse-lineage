package com.complone.hiveparser.conf;

import com.complone.hiveparser.conf.hikari.HikariDataConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "spring.datasources.hikari")
@Component
@Data
public class HikariDataSourceManager extends DataSourceConfig{
    private String type;
    
    private List<HikariDataConfig> hikariDataConfigs;
    
}
