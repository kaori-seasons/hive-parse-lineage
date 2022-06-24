package com.complone.hiveparser.conf.druid;

import com.complone.hiveparser.conf.DataSourceConfig;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class DruidDataConfig extends DataSourceConfig {
    
    /**
     * 配置检测连接是否有效
     */
    private String validationQuery;
    
    /**
     * 获取连接时执行validationQuery检测连接是否有效，这个配置会降低性能
     */
    private boolean testOnBorrow;
    
    /**
     * 归还连接时执行validationQuery检测连接是否有效，这个配置会降低性能
     */
    private boolean testOnReturn;
    
    /**
     * 定期检查 如果连接空闲时间大于
     * timeBetweenEvictionRunsMillis指定的毫秒，
     * 就会执行参数validationQuery指定的SQL来检测连接是否有效
     */
    private boolean testWhileIdle;
    
    /**
     * 配置间隔多久一次才进行检测，检测需要关闭的空闲连接.(ms)
     */
    private Integer timeBetweenEvictionRunsMillis;
    
    /**
     * 配置一个连接在池中最小生存的时间 (ms)
     */
    private Integer minEvictableIdleTimeMillis;
    
    /**
     * 配置一个连接在池中最大生存的时间 (ms)
     */
    private Integer maxEvictableIdleTimeMillis;
    
    /**
     * 连接池中最大连接数量
     */
    private Integer maxActive;
    
    /**
     * 初始化连接池中连接数量
     */
    private Integer initialSize;
}
