package com.alibaba.csp.sentinel.dashboard.rule.nacos;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;


public final class NacosConfigUtils {

    public static final long DEFAULT_GET_CONFIG_TIMEOUT_MILLIS = 5000;
    public static final String GROUP_ID = "SENTINEL_GROUP";

    private static final String FLOW_RULE_DATA_ID_POSTFIX = "-flow-rules";
    private static final String AUTHORITY_RULE_DATA_ID_POSTFIX = "-authority-rules";


    private NacosConfigUtils() {
    }

    /**
     * get flow rule nacos dataId
     *
     * @param appName app name
     * @return data id
     */
    public static String getFlowRuleDataId(String appName) {
        Validate.isTrue(StringUtils.isNotBlank(appName), "appName require not empty");
        return appName + FLOW_RULE_DATA_ID_POSTFIX;
    }

    /**
     * get authority rule nacos dataId
     *
     * @param appName app name
     * @return data id
     */
    public static String getAuthorityRuleDataId(String appName) {
        Validate.isTrue(StringUtils.isNotBlank(appName), "appName require not empty");
        return appName + AUTHORITY_RULE_DATA_ID_POSTFIX;
    }
}
