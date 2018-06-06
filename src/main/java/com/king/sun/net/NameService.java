package com.king.sun.net;


import com.king.jdk.net.InetAddress;
import com.king.jdk.net.exception.UnknownHostException;


public interface NameService {
    public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException;

    public String getHostByAddr(byte[] addr) throws UnknownHostException;
}

