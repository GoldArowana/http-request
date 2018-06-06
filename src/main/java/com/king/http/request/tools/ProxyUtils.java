package com.king.http.request.tools;

import com.king.jdk.net.InetSocketAddress;
import com.king.jdk.net.Proxy;

/**
 * @author 金龙
 * @date 2018/5/31 at 上午11:40
 */
public class ProxyUtils {
    public static Proxy getNewProxy(String httpProxyHost, int httpProxyPort) {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost, httpProxyPort));
    }
}
