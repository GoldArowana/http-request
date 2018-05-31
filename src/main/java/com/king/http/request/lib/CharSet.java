package com.king.http.request.lib;

/**
 * @author 金龙
 * @date 2018/5/30 at 下午4:23
 */
public enum CharSet {
    UTF8("UTF-8");

    private String charset;

    CharSet(String charset) {
        this.charset = charset;
    }

    public String get() {
        return this.charset;
    }
}
