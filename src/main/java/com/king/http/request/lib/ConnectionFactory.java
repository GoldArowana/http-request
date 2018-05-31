package com.king.http.request.lib;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

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
