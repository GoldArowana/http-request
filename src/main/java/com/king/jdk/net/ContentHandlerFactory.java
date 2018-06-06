package com.king.jdk.net;

public interface ContentHandlerFactory {
    /**
     * Creates a new {@code ContentHandler} to read an object from
     * a {@code URLStreamHandler}.
     *
     * @param mimetype the MIME type for which a content handler is desired.
     * @return a new {@code ContentHandler} to read an object from a
     * {@code URLStreamHandler}.
     */
    ContentHandler createContentHandler(String mimetype);
}

