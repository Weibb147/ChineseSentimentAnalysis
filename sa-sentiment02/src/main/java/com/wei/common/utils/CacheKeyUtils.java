package com.wei.common.utils;

public class CacheKeyUtils {

    private static final String PREFIX_TASK_RESULT = "sentiment:task:result:";
    private static final String PREFIX_USER_SESSION = "sentiment:user:session:";

    /**
     * 生成任务结果缓存 Key
     */
    public static String taskResultKey(Long taskId) {
        return PREFIX_TASK_RESULT + taskId;
    }

    /**
     * 生成用户会话缓存 Key
     */
    public static String userSessionKey(Long userId) {
        return PREFIX_USER_SESSION + userId;
    }
}