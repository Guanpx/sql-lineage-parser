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

    /**
     * 去除字段的反引号
     * TODO 参考simplify()方法
     * @param raw 入参
     * @return 去除字段的反引号
     */
    public static String stripQuotes(String raw) {
        if (raw == null || raw.length() < 2) {
            return raw;
        }
        char first = raw.charAt(0);
        char last = raw.charAt(raw.length() - 1);
        if ((first == '`' && last == '`') || (first == '"' && last == '"')) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

}
