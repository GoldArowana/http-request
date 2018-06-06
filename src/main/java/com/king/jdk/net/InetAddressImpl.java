package com.king.jdk.net;

import com.king.jdk.net.exception.UnknownHostException;

import java.io.IOException;

interface InetAddressImpl {

    String getLocalHostName() throws UnknownHostException;

    InetAddress[]
    lookupAllHostAddr(String hostname) throws UnknownHostException;

    String getHostByAddr(byte[] addr) throws UnknownHostException;

    InetAddress anyLocalAddress();

    InetAddress loopbackAddress();

    boolean isReachable(InetAddress addr, int timeout, NetworkInterface netif,
                        int ttl) throws IOException;
}