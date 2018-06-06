package com.king.http.request.protocol;

import com.king.http.request.tools.Commons;

/**
 * @author 金龙
 * @date 2018/5/30 at 下午5:25
 */
public enum Headers {
    JSON("application/json"),
    FORM_URL_ENCODED("application/x-www-form-urlencoded"),
    GZIP("gzip"),
    ACCEPT("Accept"),
    ACCEPT_CHARSET("Accept-Charset"),
    ACCEPT_ENCODING("Accept-Encoding"),
    AUTHORIZATION("Authorization"),
    CACHE_CONTROL("Cache-Control"),
    CONTENT_ENCODING("Content-Encoding"),
    CONTENT_LENGTH("Content-Length"),
    CONTENT_TYPE("Content-Type"),
    DATE("Date"),
    ETAG("ETag"),
    EXPIRES("Expires"),
    IF_NONE_MATCH("If-None-Match"),
    LAST_MODIFIED("Last-Modified"),
    LOCATION("Location"),
    PROXY_AUTHORIZATION("Proxy-Authorization"),
    REFERER("Referer"),
    SERVER("Server"),
    USER_AGENT("User-Agent"),
    PARAM_CHARSET("getCharset"),
    CONTENT_TYPE_MULTIPART("multipart/form-data; boundary=" + Commons.BOUNDARY);


    private String header;

    Headers(String header) {
        this.header = header;
    }

    public String value() {
        return this.header;
    }
}
