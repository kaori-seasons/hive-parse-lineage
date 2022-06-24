package com.complone.hiveparser.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "spring.datasource")
@Component
@Data
public class UserConfiguarion {
    
    private String type;
    
    private DruidDataSourceManager druid;
    
    private HikariDataSourceManager hikari;
    
    
}
