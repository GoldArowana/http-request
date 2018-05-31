package com.king.http.request.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import static com.king.http.request.tools.ValidateUtils.getValidCharset;

public class RequestOutputStream extends BufferedOutputStream {

    private final CharsetEncoder encoder;

    public RequestOutputStream(final OutputStream stream, final String charset, final int bufferSize) {
        super(stream, bufferSize);
        encoder = Charset.forName(getValidCharset(charset)).newEncoder();
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
