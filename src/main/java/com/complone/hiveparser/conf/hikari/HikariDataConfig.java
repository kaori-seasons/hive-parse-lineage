package com.complone.hiveparser.conf.hikari;

import com.complone.hiveparser.conf.DataSourceConfig;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class HikariDataConfig extends DataSourceConfig {
    
    /**
     * 池中的最大连接数。（4 个分区，每个分区有 20 个连接），所以默认最大值没有改变
     */
    private Integer maximumPoolSize;
    
    /**
     * 连接池保持打开和空闲的连接数
     */
    private Integer minimumIdle;
    
    /**
     * 池等待连接可用的秒数
     */
    private Long connectionTimeout;
    
    /**
     * 池允许连接在关闭之前保持空闲的秒数
     */
    private Long idleTimeout;
}
