package com.king.http.request.protocol;

/**
 * @author 金龙
 * @date 2018/5/30 at 下午5:53
 */
public enum Method {
    METHOD_DELETE("DELETE"),

    METHOD_GET("GET"),

    METHOD_HEAD("HEAD"),

    METHOD_OPTIONS("OPTIONS"),

    METHOD_POST("POST"),

    METHOD_PUT("PUT"),

    METHOD_TRACE("TRACE");

    private String method;

    Method(String method) {
        this.method = method;
    }

    public String value() {
        return this.method;
    }
}
