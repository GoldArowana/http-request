package com.king.jdk.net;

public interface URLStreamHandlerFactory {
    URLStreamHandler createURLStreamHandler(String protocol);
}
