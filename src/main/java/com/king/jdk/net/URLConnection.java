/*
 * Copyright (c) 1995, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package com.king.jdk.net;

import com.king.jdk.net.exception.UnknownServiceException;
import com.king.sun.net.MimeTable;
import sun.net.www.MessageHeader;
import sun.security.util.SecurityConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.Permission;
import java.util.*;

public abstract class URLConnection {

    private static final String contentClassPrefix = "sun.net.www.content";
    private static final String contentPathProp = "java.content.handler.pkgs";
    /**
     * The ContentHandler factory.
     */
    static ContentHandlerFactory factory;
    private static boolean defaultAllowUserInteraction = false;
    private static boolean defaultUseCaches = true;
    /**
     * @since JDK1.1
     */
    private static FileNameMap fileNameMap;
    /**
     * @since 1.2.2
     */
    private static boolean fileNameMapLoaded = false;
    private static Hashtable<String, ContentHandler> handlers = new Hashtable<>();
    /**
     * The URL represents the remote object on the World Wide Web to
     * which this connection is opened.
     * <p>
     * The value of this field can be accessed by the
     * {@code getURL} method.
     * <p>
     * The default value of this variable is the value of the URL
     * argument in the {@code URLConnection} constructor.
     *
     * @see URLConnection#getURL()
     * @see URLConnection#url
     */
    protected URL url;
    protected boolean doInput = true;
    /**
     * This variable is set by the {@code setDoOutput} method. Its
     * value is returned by the {@code getDoOutput} method.
     * <p>
     * A URL connection can be used for input and/or output. Setting the
     * {@code doOutput} flag to {@code true} indicates
     * that the application intends to write data to the URL connection.
     * <p>
     * The default value of this field is {@code false}.
     *
     * @see URLConnection#getDoOutput()
     * @see URLConnection#setDoOutput(boolean)
     */
    protected boolean doOutput = false;
    /**
     * If {@code true}, this {@code URL} is being examined in
     * a context in which it makes sense to allow user interactions such
     * as popping up an authentication dialog. If {@code false},
     * then no user interaction is allowed.
     * <p>
     * The value of this field can be set by the
     * {@code setAllowUserInteraction} method.
     * Its value is returned by the
     * {@code getAllowUserInteraction} method.
     * Its default value is the value of the argument in the last invocation
     * of the {@code setDefaultAllowUserInteraction} method.
     *
     * @see URLConnection#getAllowUserInteraction()
     * @see URLConnection#setAllowUserInteraction(boolean)
     * @see URLConnection#setDefaultAllowUserInteraction(boolean)
     */
    protected boolean allowUserInteraction = defaultAllowUserInteraction;
    /**
     * If {@code true}, the protocol is allowed to use caching
     * whenever it can. If {@code false}, the protocol must always
     * try to get a fresh copy of the object.
     * <p>
     * This field is set by the {@code setUseCaches} method. Its
     * value is returned by the {@code getUseCaches} method.
     * <p>
     * Its default value is the value given in the last invocation of the
     * {@code setDefaultUseCaches} method.
     *
     * @see URLConnection#setUseCaches(boolean)
     * @see URLConnection#getUseCaches()
     * @see URLConnection#setDefaultUseCaches(boolean)
     */
    protected boolean useCaches = defaultUseCaches;
    protected long ifModifiedSince = 0;
    /**
     * If {@code false}, this connection object has not created a
     * communications link to the specified URL. If {@code true},
     * the communications link has been established.
     */
    protected boolean connected = false;
    /**
     * @since 1.5
     */
    private int connectTimeout;
    private int readTimeout;
    /**
     * @since 1.6
     */
    private MessageHeader requests;

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     */
    protected URLConnection(URL url) {
        this.url = url;
    }

    /**
     * Loads filename map (a mimetable) from a data file. It will
     * first try to load the user-specific table, defined
     * by &quot;content.types.user.table&quot; property. If that fails,
     * it tries to load the default built-in table.
     *
     * @return the FileNameMap
     * @see #setFileNameMap(FileNameMap)
     * @since 1.2
     */
    public static synchronized FileNameMap getFileNameMap() {
        if ((fileNameMap == null) && !fileNameMapLoaded) {
            fileNameMap = MimeTable.loadTable();
            fileNameMapLoaded = true;
        }

        return new FileNameMap() {
            private FileNameMap map = fileNameMap;

            public String getContentTypeFor(String fileName) {
                return map.getContentTypeFor(fileName);
            }
        };
    }

    public static void setFileNameMap(FileNameMap map) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkSetFactory();
        fileNameMap = map;
    }

    public static boolean getDefaultAllowUserInteraction() {
        return defaultAllowUserInteraction;
    }

    public static void setDefaultAllowUserInteraction(boolean defaultallowuserinteraction) {
        defaultAllowUserInteraction = defaultallowuserinteraction;
    }

    @Deprecated
    public static void setDefaultRequestProperty(String key, String value) {
    }

    @Deprecated
    public static String getDefaultRequestProperty(String key) {
        return null;
    }

    public static synchronized void setContentHandlerFactory(ContentHandlerFactory fac) {
        if (factory != null) {
            throw new Error("factory already defined");
        }
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSetFactory();
        }
        factory = fac;
    }

    public static String guessContentTypeFromName(String fname) {
        return getFileNameMap().getContentTypeFor(fname);
    }

    static public String guessContentTypeFromStream(InputStream is)
            throws IOException {
        // If we can't read ahead safely, just give up on guessing
        if (!is.markSupported())
            return null;

        is.mark(16);
        int c1 = is.read();
        int c2 = is.read();
        int c3 = is.read();
        int c4 = is.read();
        int c5 = is.read();
        int c6 = is.read();
        int c7 = is.read();
        int c8 = is.read();
        int c9 = is.read();
        int c10 = is.read();
        int c11 = is.read();
        int c12 = is.read();
        int c13 = is.read();
        int c14 = is.read();
        int c15 = is.read();
        int c16 = is.read();
        is.reset();

        if (c1 == 0xCA && c2 == 0xFE && c3 == 0xBA && c4 == 0xBE) {
            return "application/java-vm";
        }

        if (c1 == 0xAC && c2 == 0xED) {
            // next two bytes are version number, currently 0x00 0x05
            return "application/x-java-serialized-object";
        }

        if (c1 == '<') {
            if (c2 == '!'
                    || ((c2 == 'h' && (c3 == 't' && c4 == 'm' && c5 == 'l' ||
                    c3 == 'e' && c4 == 'a' && c5 == 'd') ||
                    (c2 == 'b' && c3 == 'o' && c4 == 'd' && c5 == 'y'))) ||
                    ((c2 == 'H' && (c3 == 'T' && c4 == 'M' && c5 == 'L' ||
                            c3 == 'E' && c4 == 'A' && c5 == 'D') ||
                            (c2 == 'B' && c3 == 'O' && c4 == 'D' && c5 == 'Y')))) {
                return "text/html";
            }

            if (c2 == '?' && c3 == 'x' && c4 == 'm' && c5 == 'l' && c6 == ' ') {
                return "application/xml";
            }
        }

        // big and little (identical) endian UTF-8 encodings, with BOM
        if (c1 == 0xef && c2 == 0xbb && c3 == 0xbf) {
            if (c4 == '<' && c5 == '?' && c6 == 'x') {
                return "application/xml";
            }
        }

        // big and little endian UTF-16 encodings, with byte order mark
        if (c1 == 0xfe && c2 == 0xff) {
            if (c3 == 0 && c4 == '<' && c5 == 0 && c6 == '?' &&
                    c7 == 0 && c8 == 'x') {
                return "application/xml";
            }
        }

        if (c1 == 0xff && c2 == 0xfe) {
            if (c3 == '<' && c4 == 0 && c5 == '?' && c6 == 0 &&
                    c7 == 'x' && c8 == 0) {
                return "application/xml";
            }
        }

        // big and little endian UTF-32 encodings, with BOM
        if (c1 == 0x00 && c2 == 0x00 && c3 == 0xfe && c4 == 0xff) {
            if (c5 == 0 && c6 == 0 && c7 == 0 && c8 == '<' &&
                    c9 == 0 && c10 == 0 && c11 == 0 && c12 == '?' &&
                    c13 == 0 && c14 == 0 && c15 == 0 && c16 == 'x') {
                return "application/xml";
            }
        }

        if (c1 == 0xff && c2 == 0xfe && c3 == 0x00 && c4 == 0x00) {
            if (c5 == '<' && c6 == 0 && c7 == 0 && c8 == 0 &&
                    c9 == '?' && c10 == 0 && c11 == 0 && c12 == 0 &&
                    c13 == 'x' && c14 == 0 && c15 == 0 && c16 == 0) {
                return "application/xml";
            }
        }

        if (c1 == 'G' && c2 == 'I' && c3 == 'F' && c4 == '8') {
            return "image/gif";
        }

        if (c1 == '#' && c2 == 'd' && c3 == 'e' && c4 == 'f') {
            return "image/x-bitmap";
        }

        if (c1 == '!' && c2 == ' ' && c3 == 'X' && c4 == 'P' &&
                c5 == 'M' && c6 == '2') {
            return "image/x-pixmap";
        }

        if (c1 == 137 && c2 == 80 && c3 == 78 &&
                c4 == 71 && c5 == 13 && c6 == 10 &&
                c7 == 26 && c8 == 10) {
            return "image/png";
        }

        if (c1 == 0xFF && c2 == 0xD8 && c3 == 0xFF) {
            if (c4 == 0xE0 || c4 == 0xEE) {
                return "image/jpeg";
            }

            /**
             * File format used by digital cameras to store images.
             * Exif Format can be read by any application supporting
             * JPEG. Exif Spec can be found at:
             * http://www.pima.net/standards/it10/PIMA15740/Exif_2-1.PDF
             */
            if ((c4 == 0xE1) &&
                    (c7 == 'E' && c8 == 'x' && c9 == 'i' && c10 == 'f' &&
                            c11 == 0)) {
                return "image/jpeg";
            }
        }

        if (c1 == 0xD0 && c2 == 0xCF && c3 == 0x11 && c4 == 0xE0 &&
                c5 == 0xA1 && c6 == 0xB1 && c7 == 0x1A && c8 == 0xE1) {

            /* Above is signature of Microsoft Structured Storage.
             * Below this, could have tests for various SS entities.
             * For now, just test for FlashPix.
             */
            if (checkfpx(is)) {
                return "image/vnd.fpx";
            }
        }

        if (c1 == 0x2E && c2 == 0x73 && c3 == 0x6E && c4 == 0x64) {
            return "audio/basic";  // .au format, big endian
        }

        if (c1 == 0x64 && c2 == 0x6E && c3 == 0x73 && c4 == 0x2E) {
            return "audio/basic";  // .au format, little endian
        }

        if (c1 == 'R' && c2 == 'I' && c3 == 'F' && c4 == 'F') {
            /* I don't know if this is official but evidence
             * suggests that .wav files start with "RIFF" - brown
             */
            return "audio/x-wav";
        }
        return null;
    }


    static private boolean checkfpx(InputStream is) throws IOException {


        is.mark(0x100);

        // Get the byte ordering located at 0x1E. 0xFE is Intel,
        // 0xFF is other
        long toSkip = (long) 0x1C;
        long posn;

        if ((posn = skipForward(is, toSkip)) < toSkip) {
            is.reset();
            return false;
        }

        int c[] = new int[16];
        if (readBytes(c, 2, is) < 0) {
            is.reset();
            return false;
        }

        int byteOrder = c[0];

        posn += 2;
        int uSectorShift;
        if (readBytes(c, 2, is) < 0) {
            is.reset();
            return false;
        }

        if (byteOrder == 0xFE) {
            uSectorShift = c[0];
            uSectorShift += c[1] << 8;
        } else {
            uSectorShift = c[0] << 8;
            uSectorShift += c[1];
        }

        posn += 2;
        toSkip = (long) 0x30 - posn;
        long skipped = 0;
        if ((skipped = skipForward(is, toSkip)) < toSkip) {
            is.reset();
            return false;
        }
        posn += skipped;

        if (readBytes(c, 4, is) < 0) {
            is.reset();
            return false;
        }

        int sectDirStart;
        if (byteOrder == 0xFE) {
            sectDirStart = c[0];
            sectDirStart += c[1] << 8;
            sectDirStart += c[2] << 16;
            sectDirStart += c[3] << 24;
        } else {
            sectDirStart = c[0] << 24;
            sectDirStart += c[1] << 16;
            sectDirStart += c[2] << 8;
            sectDirStart += c[3];
        }
        posn += 4;
        is.reset(); // Reset back to the beginning

        toSkip = 0x200L + (long) (1 << uSectorShift) * sectDirStart + 0x50L;

        // Sanity check!
        if (toSkip < 0) {
            return false;
        }


        is.mark((int) toSkip + 0x30);

        if ((skipForward(is, toSkip)) < toSkip) {
            is.reset();
            return false;
        }



        if (readBytes(c, 16, is) < 0) {
            is.reset();
            return false;
        }

        // intel byte order
        if (byteOrder == 0xFE &&
                c[0] == 0x00 && c[2] == 0x61 && c[3] == 0x56 &&
                c[4] == 0x54 && c[5] == 0xC1 && c[6] == 0xCE &&
                c[7] == 0x11 && c[8] == 0x85 && c[9] == 0x53 &&
                c[10] == 0x00 && c[11] == 0xAA && c[12] == 0x00 &&
                c[13] == 0xA1 && c[14] == 0xF9 && c[15] == 0x5B) {
            is.reset();
            return true;
        }

        // non-intel byte order
        else if (c[3] == 0x00 && c[1] == 0x61 && c[0] == 0x56 &&
                c[5] == 0x54 && c[4] == 0xC1 && c[7] == 0xCE &&
                c[6] == 0x11 && c[8] == 0x85 && c[9] == 0x53 &&
                c[10] == 0x00 && c[11] == 0xAA && c[12] == 0x00 &&
                c[13] == 0xA1 && c[14] == 0xF9 && c[15] == 0x5B) {
            is.reset();
            return true;
        }
        is.reset();
        return false;
    }

    /**
     * Tries to read the specified number of bytes from the stream
     * Returns -1, If EOF is reached before len bytes are read, returns 0
     * otherwise
     */
    static private int readBytes(int c[], int len, InputStream is)
            throws IOException {

        byte buf[] = new byte[len];
        if (is.read(buf, 0, len) < len) {
            return -1;
        }

        // fill the passed in int array
        for (int i = 0; i < len; i++) {
            c[i] = buf[i] & 0xff;
        }
        return 0;
    }

    /**
     * Skips through the specified number of bytes from the stream
     * until either EOF is reached, or the specified
     * number of bytes have been skipped
     */
    static private long skipForward(InputStream is, long toSkip)
            throws IOException {

        long eachSkip = 0;
        long skipped = 0;

        while (skipped != toSkip) {
            eachSkip = is.skip(toSkip - skipped);

            // check if EOF is reached
            if (eachSkip <= 0) {
                if (is.read() == -1) {
                    return skipped;
                } else {
                    skipped++;
                }
            }
            skipped += eachSkip;
        }
        return skipped;
    }

    /**
     * Opens a communications link to the resource referenced by this
     * URL, if such a connection has not already been established.
     * <p>
     * If the {@code connect} method is called when the connection
     * has already been opened (indicated by the {@code connected}
     * field having the value {@code true}), the call is ignored.
     * <p>
     * URLConnection objects go through two phases: first they are
     * created, then they are connected.  After being created, and
     * before being connected, various options can be specified
     * (e.g., doInput and UseCaches).  After connecting, it is an
     * error to try to set them.  Operations that depend on being
     * connected, like getContentLength, will implicitly perform the
     * connection, if necessary.
     *
     * @throws IOException if an I/O error occurs while opening the
     *                     connection.
     * @see URLConnection#connected
     * @see #getConnectTimeout()
     * @see #setConnectTimeout(int)
     */
    abstract public void connect() throws IOException;

    /**
     * Returns setting for connect timeout.
     * <p>
     * 0 return implies that the option is disabled
     * (i.e., timeout of infinity).
     *
     * @return an {@code int} that indicates the connect timeout
     * value in milliseconds
     * @see #setConnectTimeout(int)
     * @see #connect()
     * @since 1.5
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets a specified timeout value, in milliseconds, to be used
     * when opening a communications link to the resource referenced
     * by this URLConnection.  If the timeout expires before the
     * connection can be established, a
     * interpreted as an infinite timeout.
     *
     * <p> Some non-standard implementation of this method may ignore
     * the specified timeout. To see the connect timeout set, please
     * call getConnectTimeout().
     *
     * @param timeout an {@code int} that specifies the connect
     *                timeout value in milliseconds
     * @throws IllegalArgumentException if the timeout parameter is negative
     * @see #getConnectTimeout()
     * @see #connect()
     * @since 1.5
     */
    public void setConnectTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        connectTimeout = timeout;
    }

    /**
     * Returns setting for read timeout. 0 return implies that the
     * option is disabled (i.e., timeout of infinity).
     *
     * @return an {@code int} that indicates the read timeout
     * value in milliseconds
     * @see #setReadTimeout(int)
     * @see InputStream#read()
     * @since 1.5
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the read timeout to a specified timeout, in
     * milliseconds. A non-zero value specifies the timeout when
     * reading from Input stream when a connection is established to a
     * resource. If the timeout expires before there is data available
     * timeout of zero is interpreted as an infinite timeout.
     *
     * <p> Some non-standard implementation of this method ignores the
     * specified timeout. To see the read timeout set, please call
     * getReadTimeout().
     *
     * @param timeout an {@code int} that specifies the timeout
     *                value to be used in milliseconds
     * @throws IllegalArgumentException if the timeout parameter is negative
     * @see #getReadTimeout()
     * @see InputStream#read()
     * @since 1.5
     */
    public void setReadTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        readTimeout = timeout;
    }

    /**
     * Returns the value of this {@code URLConnection}'s {@code URL}
     * field.
     *
     * @return the value of this {@code URLConnection}'s {@code URL}
     * field.
     * @see URLConnection#url
     */
    public URL getURL() {
        return url;
    }

    /**
     * Returns the value of the {@code content-length} header field.
     * <p>
     * <B>Note</B>: {@link #getContentLengthLong() getContentLengthLong()}
     * should be preferred over this method, since it returns a {@code long}
     * instead and is therefore more portable.</P>
     *
     * @return the content length of the resource that this connection's URL
     * references, {@code -1} if the content length is not known,
     * or if the content length is greater than Integer.MAX_VALUE.
     */
    public int getContentLength() {
        long l = getContentLengthLong();
        if (l > Integer.MAX_VALUE)
            return -1;
        return (int) l;
    }

    /**
     * Returns the value of the {@code content-length} header field as a
     * long.
     *
     * @return the content length of the resource that this connection's URL
     * references, or {@code -1} if the content length is
     * not known.
     * @since 7.0
     */
    public long getContentLengthLong() {
        return getHeaderFieldLong("content-length", -1);
    }

    /**
     * Returns the value of the {@code content-type} header field.
     *
     * @return the content type of the resource that the URL references,
     * or {@code null} if not known.
     * @see URLConnection#getHeaderField(String)
     */
    public String getContentType() {
        return getHeaderField("content-type");
    }

    /**
     * Returns the value of the {@code content-encoding} header field.
     *
     * @return the content encoding of the resource that the URL references,
     * or {@code null} if not known.
     * @see URLConnection#getHeaderField(String)
     */
    public String getContentEncoding() {
        return getHeaderField("content-encoding");
    }

    /**
     * Returns the value of the {@code expires} header field.
     *
     * @return the expiration date of the resource that this URL references,
     * or 0 if not known. The value is the number of milliseconds since
     * January 1, 1970 GMT.
     * @see URLConnection#getHeaderField(String)
     */
    public long getExpiration() {
        return getHeaderFieldDate("expires", 0);
    }

    /**
     * Returns the value of the {@code date} header field.
     *
     * @return the sending date of the resource that the URL references,
     * or {@code 0} if not known. The value returned is the
     * number of milliseconds since January 1, 1970 GMT.
     * @see URLConnection#getHeaderField(String)
     */
    public long getDate() {
        return getHeaderFieldDate("date", 0);
    }

    /**
     * Returns the value of the {@code last-modified} header field.
     * The result is the number of milliseconds since January 1, 1970 GMT.
     *
     * @return the date the resource referenced by this
     * {@code URLConnection} was last modified, or 0 if not known.
     * @see URLConnection#getHeaderField(String)
     */
    public long getLastModified() {
        return getHeaderFieldDate("last-modified", 0);
    }

    /**
     * Returns the value of the named header field.
     * <p>
     * If called on a connection that sets the same header multiple times
     * with possibly different values, only the last value is returned.
     *
     * @param name the name of a header field.
     * @return the value of the named header field, or {@code null}
     * if there is no such field in the header.
     */
    public String getHeaderField(String name) {
        return null;
    }

    /**
     * Returns an unmodifiable Map of the header fields.
     * The Map keys are Strings that represent the
     * response-header field names. Each Map value is an
     * unmodifiable List of Strings that represents
     * the corresponding field values.
     *
     * @return a Map of header fields
     * @since 1.4
     */
    public Map<String, List<String>> getHeaderFields() {
        return Collections.emptyMap();
    }

    /**
     * Returns the value of the named field parsed as a number.
     * <p>
     * This form of {@code getHeaderField} exists because some
     * connection types (e.g., {@code http-ng}) have pre-parsed
     * headers. Classes for that connection type can override this method
     * and short-circuit the parsing.
     *
     * @param name    the name of the header field.
     * @param Default the default value.
     * @return the value of the named field, parsed as an integer. The
     * {@code Default} value is returned if the field is
     * missing or malformed.
     */
    public int getHeaderFieldInt(String name, int Default) {
        String value = getHeaderField(name);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
        }
        return Default;
    }

    /**
     * Returns the value of the named field parsed as a number.
     * <p>
     * This form of {@code getHeaderField} exists because some
     * connection types (e.g., {@code http-ng}) have pre-parsed
     * headers. Classes for that connection type can override this method
     * and short-circuit the parsing.
     *
     * @param name    the name of the header field.
     * @param Default the default value.
     * @return the value of the named field, parsed as a long. The
     * {@code Default} value is returned if the field is
     * missing or malformed.
     * @since 7.0
     */
    public long getHeaderFieldLong(String name, long Default) {
        String value = getHeaderField(name);
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
        }
        return Default;
    }


    @SuppressWarnings("deprecation")
    public long getHeaderFieldDate(String name, long Default) {
        String value = getHeaderField(name);
        try {
            return Date.parse(value);
        } catch (Exception e) {
        }
        return Default;
    }

    /**
     * Returns the key for the {@code n}<sup>th</sup> header field.
     * It returns {@code null} if there are fewer than {@code n+1} fields.
     *
     * @param n an index, where {@code n>=0}
     * @return the key for the {@code n}<sup>th</sup> header field,
     * or {@code null} if there are fewer than {@code n+1}
     * fields.
     */
    public String getHeaderFieldKey(int n) {
        return null;
    }

    /**
     * Returns the value for the {@code n}<sup>th</sup> header field.
     * It returns {@code null} if there are fewer than
     * {@code n+1}fields.
     * <p>
     * This method can be used in conjunction with the
     * {@link #getHeaderFieldKey(int) getHeaderFieldKey} method to iterate through all
     * the headers in the message.
     *
     * @param n an index, where {@code n>=0}
     * @return the value of the {@code n}<sup>th</sup> header field
     * or {@code null} if there are fewer than {@code n+1} fields
     * @see URLConnection#getHeaderFieldKey(int)
     */
    public String getHeaderField(int n) {
        return null;
    }

    /**
     * Retrieves the contents of this URL connection.
     * <p>
     * This method first determines the content type of the object by
     * calling the {@code getContentType} method. If this is
     * the first time that the application has seen that specific content
     * type, a content handler for that content type is created:
     * <ol>
     * <li>If the application has set up a content handler factory instance
     * using the {@code setContentHandlerFactory} method, the
     * {@code createContentHandler} method of that instance is called
     * with the content type as an argument; the result is a content
     * handler for that content type.
     * <li>If no content handler factory has yet been set up, or if the
     * factory's {@code createContentHandler} method returns
     * {@code null}, then the application loads the class named:
     * <blockquote><pre>
     *         sun.net.www.content.&lt;<i>contentType</i>&gt;
     *     </pre></blockquote>
     * where &lt;<i>contentType</i>&gt; is formed by taking the
     * content-type string, replacing all slash characters with a
     * {@code period} ('.'), and all other non-alphanumeric characters
     * with the underscore character '{@code _}'. The alphanumeric
     * characters are specifically the 26 uppercase ASCII letters
     * '{@code A}' through '{@code Z}', the 26 lowercase ASCII
     * letters '{@code a}' through '{@code z}', and the 10 ASCII
     * digits '{@code 0}' through '{@code 9}'. If the specified
     * class does not exist, or is not a subclass of
     * {@code ContentHandler}, then an
     * {@code UnknownServiceException} is thrown.
     * </ol>
     *
     * @return the object fetched. The {@code instanceof} operator
     * should be used to determine the specific kind of object
     * returned.
     * @throws IOException if an I/O error occurs while
     *                     getting the content.
     *                     the content type.
     * @see URLConnection#getContentType()
     * @see URLConnection#setContentHandlerFactory(ContentHandlerFactory)
     */
    public Object getContent() throws IOException {
        // Must call getInputStream before GetHeaderField gets called
        // so that FileNotFoundException has a chance to be thrown up
        // from here without being caught.
        getInputStream();
        return getContentHandler().getContent(this);
    }

    /**
     * Retrieves the contents of this URL connection.
     *
     * @param classes the {@code Class} array
     *                indicating the requested types
     * @return the object fetched that is the first match of the type
     * specified in the classes array. null if none of
     * the requested types are supported.
     * The {@code instanceof} operator should be used to
     * determine the specific kind of object returned.
     * @throws IOException if an I/O error occurs while
     *                     getting the content.
     * @see URLConnection#getContent()
     * @see ContentHandlerFactory#createContentHandler(String)
     * @see URLConnection#getContent(Class[])
     * @see URLConnection#setContentHandlerFactory(ContentHandlerFactory)
     * @since 1.3
     */
    public Object getContent(Class[] classes) throws IOException {
        // Must call getInputStream before GetHeaderField gets called
        // so that FileNotFoundException has a chance to be thrown up
        // from here without being caught.
        getInputStream();
        return getContentHandler().getContent(this, classes);
    }

    /**
     * Returns a permission object representing the permission
     * necessary to make the connection represented by this
     * object. This method returns null if no permission is
     * required to make the connection. By default, this method
     * returns {@code java.security.AllPermission}. Subclasses
     * should override this method and return the permission
     * that best represents the permission required to make a
     * a connection to the URL. For example, a {@code URLConnection}
     * representing a {@code file:} URL would return a
     * {@code java.io.FilePermission} object.
     *
     * <p>The permission returned may dependent upon the state of the
     * connection. For example, the permission before connecting may be
     * different from that after connecting. For example, an HTTP
     * sever, say foo.com, may redirect the connection to a different
     * host, say bar.com. Before connecting the permission returned by
     * the connection will represent the permission needed to connect
     * to foo.com, while the permission returned after connecting will
     * be to bar.com.
     *
     * <p>Permissions are generally used for two purposes: to protect
     * caches of objects obtained through URLConnections, and to check
     * the right of a recipient to learn about a particular URL. In
     * the first case, the permission should be obtained
     * <em>after</em> the object has been obtained. For example, in an
     * HTTP connection, this will represent the permission to connect
     * to the host from which the data was ultimately fetched. In the
     * second case, the permission should be obtained and tested
     * <em>before</em> connecting.
     *
     * @return the permission object representing the permission
     * necessary to make the connection represented by this
     * URLConnection.
     * @throws IOException if the computation of the permission
     *                     requires network or file I/O and an exception occurs while
     *                     computing it.
     */
    public Permission getPermission() throws IOException {
        return SecurityConstants.ALL_PERMISSION;
    }

    /**
     * Returns an input stream that reads from this open connection.
     * <p>
     * A SocketTimeoutException can be thrown when reading from the
     * returned input stream if the read timeout expires before data
     * is available for read.
     *
     * @return an input stream that reads from this open connection.
     * @throws IOException if an I/O error occurs while
     *                     creating the input stream.
     * @see #setReadTimeout(int)
     * @see #getReadTimeout()
     */
    public InputStream getInputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support input");
    }

    /**
     * Returns an output stream that writes to this connection.
     *
     * @return an output stream that writes to this connection.
     * @throws IOException             if an I/O error occurs while
     *                                 creating the output stream.
     * @throws UnknownServiceException if the protocol does not support
     *                                 output.
     */
    public OutputStream getOutputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support output");
    }

    /**
     * Returns a {@code String} representation of this URL connection.
     *
     * @return a string representation of this {@code URLConnection}.
     */
    public String toString() {
        return this.getClass().getName() + ":" + url;
    }

    /**
     * Returns the value of this {@code URLConnection}'s
     * {@code doInput} flag.
     *
     * @return the value of this {@code URLConnection}'s
     * {@code doInput} flag.
     * @see #setDoInput(boolean)
     */
    public boolean getDoInput() {
        return doInput;
    }

    /**
     * Sets the value of the {@code doInput} field for this
     * {@code URLConnection} to the specified value.
     * <p>
     * A URL connection can be used for input and/or output.  Set the DoInput
     * flag to true if you intend to use the URL connection for input,
     * false if not.  The default is true.
     *
     * @param doinput the new value.
     * @throws IllegalStateException if already connected
     * @see URLConnection#doInput
     * @see #getDoInput()
     */
    public void setDoInput(boolean doinput) {
        if (connected)
            throw new IllegalStateException("Already connected");
        doInput = doinput;
    }

    /**
     * Returns the value of this {@code URLConnection}'s
     * {@code doOutput} flag.
     *
     * @return the value of this {@code URLConnection}'s
     * {@code doOutput} flag.
     * @see #setDoOutput(boolean)
     */
    public boolean getDoOutput() {
        return doOutput;
    }

    /**
     * Sets the value of the {@code doOutput} field for this
     * {@code URLConnection} to the specified value.
     * <p>
     * A URL connection can be used for input and/or output.  Set the DoOutput
     * flag to true if you intend to use the URL connection for output,
     * false if not.  The default is false.
     *
     * @param dooutput the new value.
     * @throws IllegalStateException if already connected
     * @see #getDoOutput()
     */
    public void setDoOutput(boolean dooutput) {
        if (connected)
            throw new IllegalStateException("Already connected");
        doOutput = dooutput;
    }

    /**
     * Returns the value of the {@code allowUserInteraction} field for
     * this object.
     *
     * @return the value of the {@code allowUserInteraction} field for
     * this object.
     * @see #setAllowUserInteraction(boolean)
     */
    public boolean getAllowUserInteraction() {
        return allowUserInteraction;
    }

    /**
     * Set the value of the {@code allowUserInteraction} field of
     * this {@code URLConnection}.
     *
     * @param allowuserinteraction the new value.
     * @throws IllegalStateException if already connected
     * @see #getAllowUserInteraction()
     */
    public void setAllowUserInteraction(boolean allowuserinteraction) {
        if (connected)
            throw new IllegalStateException("Already connected");
        allowUserInteraction = allowuserinteraction;
    }

    /**
     * Returns the value of this {@code URLConnection}'s
     * {@code useCaches} field.
     *
     * @return the value of this {@code URLConnection}'s
     * {@code useCaches} field.
     * @see #setUseCaches(boolean)
     */
    public boolean getUseCaches() {
        return useCaches;
    }

    /**
     * Sets the value of the {@code useCaches} field of this
     * {@code URLConnection} to the specified value.
     * <p>
     * Some protocols do caching of documents.  Occasionally, it is important
     * to be able to "tunnel through" and ignore the caches (e.g., the
     * "reload" button in a browser).  If the UseCaches flag on a connection
     * is true, the connection is allowed to use whatever caches it can.
     * If false, caches are to be ignored.
     * The default value comes from DefaultUseCaches, which defaults to
     * true.
     *
     * @param usecaches a {@code boolean} indicating whether
     *                  or not to allow caching
     * @throws IllegalStateException if already connected
     * @see #getUseCaches()
     */
    public void setUseCaches(boolean usecaches) {
        if (connected)
            throw new IllegalStateException("Already connected");
        useCaches = usecaches;
    }

    /**
     * Returns the value of this object's {@code ifModifiedSince} field.
     *
     * @return the value of this object's {@code ifModifiedSince} field.
     * @see #setIfModifiedSince(long)
     */
    public long getIfModifiedSince() {
        return ifModifiedSince;
    }

    /**
     * Sets the value of the {@code ifModifiedSince} field of
     * this {@code URLConnection} to the specified value.
     *
     * @param ifmodifiedsince the new value.
     * @throws IllegalStateException if already connected
     * @see #getIfModifiedSince()
     */
    public void setIfModifiedSince(long ifmodifiedsince) {
        if (connected)
            throw new IllegalStateException("Already connected");
        ifModifiedSince = ifmodifiedsince;
    }

    /**
     * Returns the default value of a {@code URLConnection}'s
     * {@code useCaches} flag.
     * <p>
     * Ths default is "sticky", being a part of the static state of all
     * URLConnections.  This flag applies to the next, and all following
     * URLConnections that are created.
     *
     * @return the default value of a {@code URLConnection}'s
     * {@code useCaches} flag.
     * @see #setDefaultUseCaches(boolean)
     */
    public boolean getDefaultUseCaches() {
        return defaultUseCaches;
    }

    /**
     * Sets the default value of the {@code useCaches} field to the
     * specified value.
     *
     * @param defaultusecaches the new value.
     * @see #getDefaultUseCaches()
     */
    public void setDefaultUseCaches(boolean defaultusecaches) {
        defaultUseCaches = defaultusecaches;
    }

    /**
     * Sets the general request property. If a property with the key already
     * exists, overwrite its value with the new value.
     *
     * <p> NOTE: HTTP requires all request properties which can
     * legally have multiple instances with the same key
     * to use a comma-separated list syntax which enables multiple
     * properties to be appended into a single property.
     *
     * @param key   the keyword by which the request is known
     *              (e.g., "{@code Accept}").
     * @param value the value associated with it.
     * @throws IllegalStateException if already connected
     * @throws NullPointerException  if key is <CODE>null</CODE>
     * @see #getRequestProperty(String)
     */
    public void setRequestProperty(String key, String value) {
        if (connected)
            throw new IllegalStateException("Already connected");
        if (key == null)
            throw new NullPointerException("key is null");

        if (requests == null)
            requests = new MessageHeader();

        requests.set(key, value);
    }

    /**
     * Adds a general request property specified by a
     * key-value pair.  This method will not overwrite
     * existing values associated with the same key.
     *
     * @param key   the keyword by which the request is known
     *              (e.g., "{@code Accept}").
     * @param value the value associated with it.
     * @throws IllegalStateException if already connected
     * @throws NullPointerException  if key is null
     * @see #getRequestProperties()
     * @since 1.4
     */
    public void addRequestProperty(String key, String value) {
        if (connected)
            throw new IllegalStateException("Already connected");
        if (key == null)
            throw new NullPointerException("key is null");

        if (requests == null)
            requests = new MessageHeader();

        requests.add(key, value);
    }

    /**
     * Returns the value of the named general request property for this
     * connection.
     *
     * @param key the keyword by which the request is known (e.g., "Accept").
     * @return the value of the named general request property for this
     * connection. If key is null, then null is returned.
     * @throws IllegalStateException if already connected
     * @see #setRequestProperty(String, String)
     */
    public String getRequestProperty(String key) {
        if (connected)
            throw new IllegalStateException("Already connected");

        if (requests == null)
            return null;

        return requests.findValue(key);
    }

    /**
     * Returns an unmodifiable Map of general request
     * properties for this connection. The Map keys
     * are Strings that represent the request-header
     * field names. Each Map value is a unmodifiable List
     * of Strings that represents the corresponding
     * field values.
     *
     * @return a Map of the general request properties for this connection.
     * @throws IllegalStateException if already connected
     * @since 1.4
     */
    public Map<String, List<String>> getRequestProperties() {
        if (connected)
            throw new IllegalStateException("Already connected");

        if (requests == null)
            return Collections.emptyMap();

        return requests.getHeaders(null);
    }

    /**
     * Gets the Content Handler appropriate for this connection.
     */
    synchronized ContentHandler getContentHandler()
            throws UnknownServiceException {
        String contentType = stripOffParameters(getContentType());
        ContentHandler handler = null;
        if (contentType == null)
            throw new UnknownServiceException("no content-type");
        try {
            handler = handlers.get(contentType);
            if (handler != null)
                return handler;
        } catch (Exception e) {
        }

        if (factory != null)
            handler = factory.createContentHandler(contentType);
        if (handler == null) {
            try {
                handler = lookupContentHandlerClassFor(contentType);
            } catch (Exception e) {
                e.printStackTrace();
                handler = UnknownContentHandler.INSTANCE;
            }
            handlers.put(contentType, handler);
        }
        return handler;
    }

    /*
     * Media types are in the format: type/subtype*(; parameter).
     * For looking up the content handler, we should ignore those
     * parameters.
     */
    private String stripOffParameters(String contentType) {
        if (contentType == null)
            return null;
        int index = contentType.indexOf(';');

        if (index > 0)
            return contentType.substring(0, index);
        else
            return contentType;
    }

    /**
     * Looks for a content handler in a user-defineable set of places.
     * By default it looks in sun.net.www.content, but users can define a
     * vertical-bar delimited set of class prefixes to search through in
     * addition by defining the java.content.handler.pkgs property.
     * The class name must be of the form:
     * <pre>
     *     {package-prefix}.{major}.{minor}
     * e.g.
     *     YoyoDyne.experimental.text.plain
     * </pre>
     */
    private ContentHandler lookupContentHandlerClassFor(String contentType)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        String contentHandlerClassName = typeToPackageName(contentType);

        String contentHandlerPkgPrefixes = getContentHandlerPkgPrefixes();

        StringTokenizer packagePrefixIter =
                new StringTokenizer(contentHandlerPkgPrefixes, "|");

        while (packagePrefixIter.hasMoreTokens()) {
            String packagePrefix = packagePrefixIter.nextToken().trim();

            try {
                String clsName = packagePrefix + "." + contentHandlerClassName;
                Class<?> cls = null;
                try {
                    cls = Class.forName(clsName);
                } catch (ClassNotFoundException e) {
                    ClassLoader cl = ClassLoader.getSystemClassLoader();
                    if (cl != null) {
                        cls = cl.loadClass(clsName);
                    }
                }
                if (cls != null) {
                    ContentHandler handler =
                            (ContentHandler) cls.newInstance();
                    return handler;
                }
            } catch (Exception e) {
            }
        }

        return UnknownContentHandler.INSTANCE;
    }

    /**
     * Utility function to map a MIME content type into an equivalent
     * pair of class name components.  For example: "text/html" would
     * be returned as "text.html"
     */
    private String typeToPackageName(String contentType) {
        // make sure we canonicalize the class name: all lower case
        contentType = contentType.toLowerCase();
        int len = contentType.length();
        char nm[] = new char[len];
        contentType.getChars(0, len, nm, 0);
        for (int i = 0; i < len; i++) {
            char c = nm[i];
            if (c == '/') {
                nm[i] = '.';
            } else if (!('A' <= c && c <= 'Z' ||
                    'a' <= c && c <= 'z' ||
                    '0' <= c && c <= '9')) {
                nm[i] = '_';
            }
        }
        return new String(nm);
    }

    /**
     * Returns a vertical bar separated list of package prefixes for potential
     * content handlers.  Tries to get the java.content.handler.pkgs property
     * to use as a set of package prefixes to search.  Whether or not
     * that property has been defined, the sun.net.www.content is always
     * the last one on the returned package list.
     */
    private String getContentHandlerPkgPrefixes() {
        String packagePrefixList = AccessController.doPrivileged(
                new sun.security.action.GetPropertyAction(contentPathProp, ""));

        if (packagePrefixList != "") {
            packagePrefixList += "|";
        }

        return packagePrefixList + contentClassPrefix;
    }

}


