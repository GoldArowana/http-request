package com.king.http.request.eception;

import java.io.IOException;

public class HttpRequestException extends RuntimeException {

    public HttpRequestException(final IOException cause) {
        super(cause);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
