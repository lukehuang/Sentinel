package com.alibaba.csp.sentinel.dashboard.rule.zookeeper;

/**
 * @author rodbate
 * @since 2019/04/20 15:22
 */
public final class ZookeeperConfigUtils {

    public static final String GROUP_ID = "SENTINEL_GROUP";
    private static final String ZK_PATH_SEPARATOR = "/";
    private static final String FLOW_RULE_DATA_ID_POSTFIX = "-flow-rules";
    private static final String AUTHORITY_RULE_DATA_ID_POSTFIX = "-authority-rules";

    private ZookeeperConfigUtils() {
    }

    /**
     * /groupId/dataId
     *
     * @param app app name
     * @return zk path
     */
    public static String getAuthorityRuleZkPath(String app) {
        return ZK_PATH_SEPARATOR + GROUP_ID + ZK_PATH_SEPARATOR + app + AUTHORITY_RULE_DATA_ID_POSTFIX;
    }

    /**
     * /groupId/dataId
     *
     * @param app app name
     * @return zk path
     */
    public static String getFlowRuleZkPath(String app) {
        return ZK_PATH_SEPARATOR + GROUP_ID + ZK_PATH_SEPARATOR + app + FLOW_RULE_DATA_ID_POSTFIX;
    }
}
