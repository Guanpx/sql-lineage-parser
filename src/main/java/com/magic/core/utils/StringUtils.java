package com.magic.core.utils;

/**
 * 字符串工具类
 *
 * @author Guan Peixiang
 * @date 2023/12/19
 */
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * 判断字符串是否为空
     *
     * @param str 入参
     * @return true如果字符串为null或空字符串
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断字符串是否非空
     *
     * @param str 入参
     * @return true如果字符串不为null且不为空字符串
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}
