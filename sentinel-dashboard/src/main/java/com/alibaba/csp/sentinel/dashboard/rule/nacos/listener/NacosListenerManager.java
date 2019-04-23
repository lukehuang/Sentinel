package com.alibaba.csp.sentinel.dashboard.rule.nacos.listener;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.AuthorityRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.repository.rule.RuleRepository;
import com.alibaba.csp.sentinel.dashboard.rule.nacos.NacosConfigUtils;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.*;

import static com.alibaba.csp.sentinel.dashboard.rule.nacos.NacosConfigUtils.DEFAULT_GET_CONFIG_TIMEOUT_MILLIS;

/**
 * @author rodbate
 * @since 2019/04/20 09:11
 */
@Slf4j
public class NacosListenerManager {

    private static final Object OBJ = new Object();

    /**
     * app name -> #OBJ
     */
    private final ConcurrentHashMap<String, Object> registeredApps = new ConcurrentHashMap<>(16);

    @Autowired
    private ConfigService configService;

    @Autowired
    private RuleRepository<AuthorityRuleEntity, Long> authorityRuleRepository;

    @Autowired
    private RuleRepository<FlowRuleEntity, Long> flowRuleRepository;

    @Autowired
    private Converter<String, List<AuthorityRuleEntity>> authorityRuleConverter;

    @Autowired
    private Converter<String, List<FlowRuleEntity>> flowRuleConverter;


    /**
     * add nacos listener if specified not registered
     *
     * @param app app name
     * @return true or false
     */
    public boolean addListenerIfNeed(String app) {
        if (StringUtils.isBlank(app)) {
            return false;
        }

        Object obj = registeredApps.putIfAbsent(app, NacosListenerManager.OBJ);
        if (obj == null) {
            try {
                addAppListeners(app);
            } catch (NacosException e) {
                log.error("Add nacos listener exception", e);
                return false;
            }
            return true;
        }
        return false;
    }

    private void addAppListeners(String app) throws NacosException {
        initNacosConfig(app);
        registerNacosListeners(app);
    }


    private void initNacosConfig(String app) throws NacosException {
        List<AuthorityRuleEntity> authorityRuleEntityList = null;
        String authorityRules = configService.getConfig(NacosConfigUtils.getAuthorityRuleDataId(app),
            NacosConfigUtils.GROUP_ID, DEFAULT_GET_CONFIG_TIMEOUT_MILLIS);
        if (StringUtil.isNotBlank(authorityRules)) {
            authorityRuleEntityList = authorityRuleConverter.convert(authorityRules);
        }
        authorityRuleRepository.saveAll(authorityRuleEntityList);


        List<FlowRuleEntity> flowRuleEntityList = null;
        String flowRules = configService.getConfig(NacosConfigUtils.getFlowRuleDataId(app),
            NacosConfigUtils.GROUP_ID, DEFAULT_GET_CONFIG_TIMEOUT_MILLIS);
        if (StringUtil.isNotBlank(flowRules)) {
            flowRuleEntityList = flowRuleConverter.convert(flowRules);
        }
        flowRuleRepository.saveAll(flowRuleEntityList);
    }


    private void registerNacosListeners(String app) throws NacosException {
        //authority rule config listener
        configService.addListener(NacosConfigUtils.getAuthorityRuleDataId(app), NacosConfigUtils.GROUP_ID, new Listener() {
            @Override
            public Executor getExecutor() {
                return buildExecutor(app, "authority");
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("GroupId={},DataId={}  receive config info: {}", NacosConfigUtils.GROUP_ID, NacosConfigUtils.getAuthorityRuleDataId(app), configInfo);
                List<AuthorityRuleEntity> rules = null;
                if (StringUtils.isNotBlank(configInfo)) {
                    rules = authorityRuleConverter.convert(configInfo);
                }
                authorityRuleRepository.saveAll(rules);
            }
        });

        //flow rule config listener
        configService.addListener(NacosConfigUtils.getFlowRuleDataId(app), NacosConfigUtils.GROUP_ID, new Listener() {
            @Override
            public Executor getExecutor() {
                return buildExecutor(app, "flow");
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("GroupId={},DataId={} receive config info: {}", NacosConfigUtils.GROUP_ID, NacosConfigUtils.getFlowRuleDataId(app), configInfo);
                List<FlowRuleEntity> rules = null;
                if (StringUtils.isNotBlank(configInfo)) {
                    rules = flowRuleConverter.convert(configInfo);
                }
                flowRuleRepository.saveAll(rules);
            }
        });
    }


    private Executor buildExecutor(String app, String ruleType) {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1), new NamedThreadFactory(String.format("%s-%s-rule-listener", app, ruleType)),
            new ThreadPoolExecutor.DiscardOldestPolicy());
    }

}
