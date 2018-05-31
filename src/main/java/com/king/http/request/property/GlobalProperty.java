package com.king.http.request.property;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author 金龙
 * @date 2018/5/31 at 上午11:06
 */
public class GlobalProperty {
    /**
     * Set property to given value.
     * Specifying a null value will cause the property to be cleared
     */
    private static String setProperty(final String name, final String value) {
        final PrivilegedAction<String> action;
        if (value != null) {
            action = new PrivilegedAction<String>() {

                public String run() {
                    return System.setProperty(name, value);
                }
            };
        } else {
            action = new PrivilegedAction<String>() {

                public String run() {
                    return System.clearProperty(name);
                }
            };
        }
        return AccessController.doPrivileged(action);
    }

    /**
     * This setting will apply to all requests.
     */
    public static void keepAlive(final boolean keepAlive) {
        setProperty("http.keepAlive", Boolean.toString(keepAlive));
    }

    /**
     * This setting will apply to all requests.
     */
    public static void maxConnections(final int maxConnections) {
        setProperty("http.maxConnections", Integer.toString(maxConnections));
    }

    /**
     * This setting will apply to all requests.
     */
    public static void proxyHost(final String host) {
        setProperty("http.proxyHost", host);
        setProperty("https.proxyHost", host);
    }

    /**
     * This setting will apply to all requests.
     */
    public static void proxyPort(final int port) {
        final String portValue = Integer.toString(port);
        setProperty("http.proxyPort", portValue);
        setProperty("https.proxyPort", portValue);
    }

    /**
     * Set the 'http.nonProxyHosts' property to the given host values.
     * <p>
     * Hosts will be separated by a '|' character.
     * <p>
     * This setting will apply to all requests.
     *
     * @param hosts
     */
    public static void nonProxyHosts(final String... hosts) {
        if (hosts != null && hosts.length > 0) {
            StringBuilder separated = new StringBuilder();
            int last = hosts.length - 1;
            for (int i = 0; i < last; i++)
                separated.append(hosts[i]).append('|');
            separated.append(hosts[last]);
            setProperty("http.nonProxyHosts", separated.toString());
        } else
            setProperty("http.nonProxyHosts", null);
    }

}
