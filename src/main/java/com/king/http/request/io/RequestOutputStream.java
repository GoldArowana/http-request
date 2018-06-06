package com.king.http.request.io;

import com.king.http.request.tools.ValidateUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class RequestOutputStream extends BufferedOutputStream {

    private final CharsetEncoder encoder;

    public RequestOutputStream(final OutputStream stream, final String charset, final int bufferSize) {
        super(stream, bufferSize);
        encoder = Charset.forName(ValidateUtils.getValidCharset(charset)).newEncoder();
    }

    public CharsetEncoder getEncoder() {
        return encoder;
    }

    public RequestOutputStream write(final String value) throws IOException {
        final ByteBuffer bytes = encoder.encode(CharBuffer.wrap(value));

        super.write(bytes.array(), 0, bytes.limit());

        return this;
    }
}
