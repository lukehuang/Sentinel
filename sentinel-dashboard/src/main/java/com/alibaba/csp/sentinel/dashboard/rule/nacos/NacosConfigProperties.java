package com.alibaba.csp.sentinel.dashboard.rule.nacos;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;



@Getter
@Setter
@ConfigurationProperties(prefix = "nacos.config")
public class NacosConfigProperties {
    private String serverAddr;
    private String namespace;
}
