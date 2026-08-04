package com.gdou.util;
public class UserHolder{
    /**
     * 定义一个ThreadLocal类存储用户id
     */
    private static final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    public static void setUserId(String userId) {
        threadLocal.set(userId);
    }

    public static String getUserId() {
        return threadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
    }
}
