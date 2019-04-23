package com.alibaba.csp.sentinel.dashboard.rule.zookeeper.authority;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.AuthorityRuleEntity;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.zookeeper.ZookeeperConfigUtils;
import com.alibaba.csp.sentinel.datasource.Converter;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * @author rodbate
 * @since 2019/04/20 15:17
 */
public class AuthorityRuleZookeeperProvider implements DynamicRuleProvider<List<AuthorityRuleEntity>> {
    @Autowired
    private CuratorFramework zkClient;

    @Autowired
    private Converter<String, List<AuthorityRuleEntity>> converter;

    @Override
    public List<AuthorityRuleEntity> getRules(String appName) throws Exception {
        byte[] data = zkClient.getData().forPath(ZookeeperConfigUtils.getAuthorityRuleZkPath(appName));
        if (data == null || data.length == 0) {
            return new ArrayList<>();
        }
        return converter.convert(new String(data, StandardCharsets.UTF_8));
    }

}
