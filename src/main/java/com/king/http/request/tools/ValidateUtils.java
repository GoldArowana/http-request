package com.king.http.request.tools;

import com.king.http.request.lib.CharSet;

/**
 * @author 金龙
 * @date 2018/5/30 at 下午7:16
 */
public class ValidateUtils {
    public static String getValidCharset(final String charset) {
        if (charset != null && charset.length() > 0) {
            return CharSet.valueOf(charset).get();
        } else {
            return CharSet.UTF8.get();
        }
    }
}
