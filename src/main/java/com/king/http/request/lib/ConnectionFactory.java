package com.king.http.request.lib;

import com.king.jdk.net.HttpURLConnection;
import com.king.jdk.net.Proxy;
import com.king.jdk.net.URL;

import java.io.IOException;

public interface ConnectionFactory {

    ConnectionFactory DEFAULT = new ConnectionFactory() {
        public HttpURLConnection create(URL url) throws IOException {
            return (HttpURLConnection) url.openConnection();
        }

        public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
            return (HttpURLConnection) url.openConnection(proxy);
        }
    };

    HttpURLConnection create(URL url) throws IOException;

    HttpURLConnection create(URL url, Proxy proxy) throws IOException;
}
