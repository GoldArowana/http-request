package com.king.http.request.tools;

import java.net.InetSocketAddress;
import java.net.Proxy;

import static java.net.Proxy.Type.HTTP;

/**
 * @author 金龙
 * @date 2018/5/31 at 上午11:40
 */
public class ProxyUtils {
    public static Proxy getNewProxy(String httpProxyHost, int httpProxyPort) {
        return new Proxy(HTTP, new InetSocketAddress(httpProxyHost, httpProxyPort));
    }
}
