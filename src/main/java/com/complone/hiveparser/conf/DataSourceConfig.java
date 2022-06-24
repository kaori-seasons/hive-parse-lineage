package com.complone.hiveparser.conf;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public abstract class DataSourceConfig {
    private String id;
    
    private String url;
    
    private String username;
    
    private String password;
    
}
