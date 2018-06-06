package com.king.jdk.net.exception;

import java.io.IOException;

public class MalformedURLException extends IOException {
    private static final long serialVersionUID = -182787522200415866L;

    public MalformedURLException() {
    }

    public MalformedURLException(String msg) {
        super(msg);
    }
}
