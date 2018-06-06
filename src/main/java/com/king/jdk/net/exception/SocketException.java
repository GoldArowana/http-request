package com.king.jdk.net.exception;

import java.io.IOException;

public class SocketException extends IOException {
    private static final long serialVersionUID = -5935874303556886934L;

    public SocketException(String msg) {
        super(msg);
    }

    public SocketException() {
    }
}
