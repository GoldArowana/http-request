package com.king.http.request;

import com.king.http.request.eception.HttpRequestException;
import com.king.http.request.io.RequestOutputStream;
import com.king.http.request.lib.Base64;
import com.king.http.request.lib.CharSet;
import com.king.http.request.lib.ConnectionFactory;
import com.king.http.request.lib.UploadProgress;
import com.king.http.request.operation.CloseOperation;
import com.king.http.request.operation.FlushOperation;
import com.king.http.request.protocol.Headers;
import com.king.http.request.protocol.Method;
import com.king.http.request.tools.*;
import com.king.jdk.net.HttpURLConnection;
import com.king.jdk.net.URL;
import com.king.jdk.net.URLEncoder;
import com.king.jdk.net.exception.MalformedURLException;
import com.king.jdk.net.exception.URISyntaxException;

import javax.net.ssl.*;
import java.io.*;
import java.nio.CharBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

/**
 * A fluid interface for making HTTP requests using an underlying
 * {@link HttpURLConnection} (or sub-class).
 * <p>
 * Each instance supports making a single request and cannot be reused for
 * further requests.
 */
public class HttpRequest {

    private static SSLSocketFactory TRUSTED_FACTORY;
    private static HostnameVerifier TRUSTED_VERIFIER;
    private static ConnectionFactory CONNECTION_FACTORY;

    static {
        CONNECTION_FACTORY = ConnectionFactory.DEFAULT;
    }

    private final URL url;
    private final String requestMethod;
    private boolean form;
    private int bufferSize;
    private long totalSize;
    private int httpProxyPort;
    private long totalWritten;
    private boolean multipart;
    private boolean uncompress;
    private boolean ignoreCloseExceptions;
    private String httpProxyHost;
    private UploadProgress progress;
    private RequestOutputStream output;
    private HttpURLConnection connection;

    {
        totalSize = -1;
        bufferSize = 8192;
        ignoreCloseExceptions = true;
        progress = UploadProgress.DEFAULT;
    }

    /*-start********************构造器*********************-*/
    public HttpRequest(final CharSequence url, final String method) throws HttpRequestException {
        try {
            this.url = new URL(url.toString());
        } catch (MalformedURLException e) {
            throw new HttpRequestException(e);
        }
        this.requestMethod = method;
    }

    protected HttpRequest(final URL url, final String method) throws HttpRequestException {
        this.url = url;
        this.requestMethod = method;
    }
    /*-end********************构造器*********************-*/


    /*-start******************请求**********************-*/
    public static HttpRequest req(Method method, final CharSequence url) throws HttpRequestException {
        return new HttpRequest(url, method.value());
    }

    public static HttpRequest req(Method method, final URL url) throws HttpRequestException {
        return new HttpRequest(url, method.value());
    }

    public static HttpRequest req(Method method, final CharSequence baseUrl, final Map<?, ?> params, final boolean encode) throws URISyntaxException {
        String url = HttpPathUtils.append(baseUrl, params);
        return req(method, encode ? HttpPathUtils.encode(url) : url);
    }

    public static HttpRequest req(Method method, final CharSequence baseUrl, final boolean encode, final Object... params) throws URISyntaxException {
        String url = HttpPathUtils.append(baseUrl, params);
        return req(method, encode ? HttpPathUtils.encode(url) : url);
    }
    /*-end******************请求**********************-*/

    private static SSLSocketFactory getTrustedFactory() throws HttpRequestException {
        if (TRUSTED_FACTORY == null) {
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // Intentionally left blank
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // Intentionally left blank
                }
            }};
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, trustAllCerts, new SecureRandom());
                TRUSTED_FACTORY = context.getSocketFactory();
            } catch (GeneralSecurityException e) {
                IOException ioException = new IOException(
                        "Security exception configuring SSL context");
                ioException.initCause(e);
                throw new HttpRequestException(ioException);
            }
        }

        return TRUSTED_FACTORY;
    }

    private static HostnameVerifier getTrustedVerifier() {
        if (TRUSTED_VERIFIER == null)
            TRUSTED_VERIFIER = new HostnameVerifier() {

                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

        return TRUSTED_VERIFIER;
    }

    private HttpURLConnection createConnection() {
        try {
            final HttpURLConnection connection;
            if (httpProxyHost != null) {
                connection = CONNECTION_FACTORY.create(url, ProxyUtils.getNewProxy(httpProxyHost, httpProxyPort));
            } else {
                connection = CONNECTION_FACTORY.create(url);
            }
            connection.setRequestMethod(requestMethod);
            return connection;
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    public HttpURLConnection getConnection() {
        if (connection == null) {
            connection = createConnection();
        }
        return connection;
    }

    @Override
    public String toString() {
        return getMethod() + ' ' + getUrl();
    }


    /**
     * Specify the {@link ConnectionFactory} used to create new requests.
     */
//    public static void setConnectionFactory(final ConnectionFactory connectionFactory) {
//        if (connectionFactory == null) {
//            CONNECTION_FACTORY = ConnectionFactory.DEFAULT;
//        } else {
//            CONNECTION_FACTORY = connectionFactory;
//        }
//    }

    /*-start*******************getter & setter**********************-/

   /**
     * Get whether or not exceptions thrown by {@link Closeable#close()} are ignored
     */
    public boolean isIgnoreCloseExceptions() {
        return ignoreCloseExceptions;
    }

    /**
     * Set whether or not to ignore exceptions that occur from calling
     * {@link Closeable#close()}
     */
    public HttpRequest setIgnoreCloseExceptions(final boolean ignore) {
        ignoreCloseExceptions = ignore;
        return this;
    }

    /**
     * Get the status setCode of the response
     */
    public int code() throws HttpRequestException {
        try {
            closeOutput();
            return getConnection().getResponseCode();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Set the value of the given {@link AtomicInteger} to the status setCode of the response
     */
    public HttpRequest setCode(final AtomicInteger output) throws HttpRequestException {
        output.set(code());
        return this;
    }

    public boolean isOK() throws HttpRequestException {
        return HttpURLConnection.HTTP_OK == code();
    }

    /**
     * @return true if 201, false otherwise
     */
    public boolean isCreated() throws HttpRequestException {
        return HttpURLConnection.HTTP_CREATED == code();
    }

    /**
     * @return true if 204, false otherwise
     */
    public boolean isNoContent() throws HttpRequestException {
        return HttpURLConnection.HTTP_NO_CONTENT == code();
    }

    /**
     * @return true if 500, false otherwise
     */
    public boolean isServerError() throws HttpRequestException {
        return HttpURLConnection.HTTP_INTERNAL_ERROR == code();
    }

    /**
     * @return true if 400, false otherwise
     */
    public boolean isBadRequest() throws HttpRequestException {
        return HttpURLConnection.HTTP_BAD_REQUEST == code();
    }

    /**
     * @return true if 404, false otherwise
     */
    public boolean isNotFound() throws HttpRequestException {
        return HttpURLConnection.HTTP_NOT_FOUND == code();
    }

    /**
     * @return true if 304, false otherwise
     */
    public boolean isNotModified() throws HttpRequestException {
        return HttpURLConnection.HTTP_NOT_MODIFIED == code();
    }

    /**
     * Get status getResponseMessage of the response
     *
     * @return getResponseMessage
     * @throws HttpRequestException
     */
    public String getResponseMessage() throws HttpRequestException {
        try {
            closeOutput();
            return getConnection().getResponseMessage();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Disconnect the connection
     */
    public HttpRequest disconnect() {
        getConnection().disconnect();
        return this;
    }

    /**
     * Set chunked streaming mode to the given size
     */
    public HttpRequest chunk(final int size) {
        getConnection().setChunkedStreamingMode(size);
        return this;
    }

    /**
     * Get the configured buffer size
     * The default buffer size is 8,192 bytes
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Set the size used when buffering and copying between streams
     * <p>
     * This size is also used for send and receive buffers isCreated for both char
     * and byte arrays
     * <p>
     * The default buffer size is 8,192 bytes
     *
     * @param size
     * @return this request
     */
    public HttpRequest setBufferSize(final int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }
        bufferSize = size;
        return this;
    }

    /**
     * Set whether or not the response body should be automatically uncompressed
     * when read from.
     * <p>
     * This will only affect requests that have the 'Content-Encoding' response
     * header set to 'gzip'.
     * <p>
     * This causes all receive methods to use a {@link GZIPInputStream} when
     * applicable so that higher level streams and readers can read the data
     * uncompressed.
     * <p>
     * Setting this option does not cause any request headers to be set
     * automatically so {@link #acceptGzipEncoding()} should be used in
     * conjunction with this setting to tell the server to gzip the response.
     *
     * @param uncompress
     * @return this request
     */
    public HttpRequest setUncompress(final boolean uncompress) {
        this.uncompress = uncompress;
        return this;
    }

    /**
     * Create byte array output stream
     */
    protected ByteArrayOutputStream byteStream() {
        final int size = contentLength();
        if (size > 0) {
            return new ByteArrayOutputStream(size);
        } else {
            return new ByteArrayOutputStream();
        }
    }

    /**
     * Get response as {@link String} in given character set
     * <p>
     * This will fall back to using the UTF-8 character set if the given getCharset
     * is null
     *
     * @param charset
     * @return string
     * @throws HttpRequestException
     */
    public String body(final String charset) throws HttpRequestException {
        final ByteArrayOutputStream output = byteStream();
        try {
            copy(buffer(), output);
            return output.toString(ValidateUtils.getValidCharset(charset));
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Get response as {@link String} using character set returned from
     * {@link #getCharset()}
     *
     * @return string
     * @throws HttpRequestException
     */
    public String body() throws HttpRequestException {
        return body(getCharset());
    }

    /**
     * Get the response body as a {@link String} and set it as the value of the
     * given reference.
     *
     * @param output
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest body(final AtomicReference<String> output) throws HttpRequestException {
        output.set(body());
        return this;
    }

    /**
     * Get the response body as a {@link String} and set it as the value of the
     * given reference.
     *
     * @param output
     * @param charset
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest body(final AtomicReference<String> output, final String charset) throws HttpRequestException {
        output.set(body(charset));
        return this;
    }

    /**
     * Is the response body empty?
     *
     * @return true if the Content-Length response header is 0, false otherwise
     * @throws HttpRequestException
     */
    public boolean isBodyEmpty() throws HttpRequestException {
        return contentLength() == 0;
    }

    /**
     * Get response as byte array
     *
     * @return byte array
     * @throws HttpRequestException
     */
    public byte[] bytes() throws HttpRequestException {
        final ByteArrayOutputStream output = byteStream();
        try {
            copy(buffer(), output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return output.toByteArray();
    }

    /**
     * Get response in a buffered stream
     *
     * @return stream
     * @throws HttpRequestException
     * @see #setBufferSize(int)
     */
    public BufferedInputStream buffer() throws HttpRequestException {
        return new BufferedInputStream(stream(), bufferSize);
    }

    /**
     * Get stream to response body
     *
     * @return stream
     * @throws HttpRequestException
     */
    public InputStream stream() throws HttpRequestException {
        InputStream stream;
        if (code() < HttpURLConnection.HTTP_BAD_REQUEST) {
            try {
                stream = getConnection().getInputStream();
            } catch (IOException e) {
                throw new HttpRequestException(e);
            }
        } else {
            stream = getConnection().getErrorStream();
            if (stream == null) {
                try {
                    stream = getConnection().getInputStream();
                } catch (IOException e) {
                    if (contentLength() > 0) {
                        throw new HttpRequestException(e);
                    } else {
                        stream = new ByteArrayInputStream(new byte[0]);
                    }
                }
            }
        }

        if (!uncompress || !Headers.GZIP.value().equals(contentEncoding())) {
            return stream;
        } else {
            try {
                return new GZIPInputStream(stream);
            } catch (IOException e) {
                throw new HttpRequestException(e);
            }
        }
    }

    /**
     * Get reader to response body using given character set.
     * <p>
     * This will fall back to using the UTF-8 character set if the given getCharset
     * is null
     *
     * @param charset
     * @return reader
     * @throws HttpRequestException
     */
    public InputStreamReader reader(final String charset) throws HttpRequestException {
        try {
            return new InputStreamReader(stream(), ValidateUtils.getValidCharset(charset));
        } catch (UnsupportedEncodingException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Get reader to response body using the character set returned from
     * {@link #getCharset()}
     *
     * @return reader
     * @throws HttpRequestException
     */
    public InputStreamReader reader() throws HttpRequestException {
        return reader(getCharset());
    }

    /**
     * Get buffered reader to response body using the given character set r and
     * the configured buffer size
     *
     * @param charset
     * @return reader
     * @throws HttpRequestException
     * @see #setBufferSize(int)
     */
    public BufferedReader bufferedReader(final String charset)
            throws HttpRequestException {
        return new BufferedReader(reader(charset), bufferSize);
    }

    /**
     * Get buffered reader to response body using the character set returned from
     * {@link #getCharset()} and the configured buffer size
     *
     * @return reader
     * @throws HttpRequestException
     * @see #setBufferSize(int)
     */
    public BufferedReader bufferedReader() throws HttpRequestException {
        return bufferedReader(getCharset());
    }

    /**
     * Stream response body to file
     *
     * @param file
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest receive(final File file) throws HttpRequestException {
        final OutputStream output;
        try {
            output = new BufferedOutputStream(new FileOutputStream(file), bufferSize);
        } catch (FileNotFoundException e) {
            throw new HttpRequestException(e);
        }
        return new CloseOperation<HttpRequest>(output, ignoreCloseExceptions) {

            @Override
            protected HttpRequest run() throws HttpRequestException, IOException {
                return receive(output);
            }
        }.call();
    }

    /**
     * Stream response to given output stream
     *
     * @param output
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest receive(final OutputStream output)
            throws HttpRequestException {
        try {
            return copy(buffer(), output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Stream response to given print stream
     *
     * @param output
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest receive(final PrintStream output)
            throws HttpRequestException {
        return receive((OutputStream) output);
    }

    /**
     * Receive response into the given appendable
     *
     * @param appendable
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest receive(final Appendable appendable)
            throws HttpRequestException {
        final BufferedReader reader = bufferedReader();
        return new CloseOperation<HttpRequest>(reader, ignoreCloseExceptions) {

            @Override
            public HttpRequest run() throws IOException {
                final CharBuffer buffer = CharBuffer.allocate(bufferSize);
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    buffer.rewind();
                    appendable.append(buffer, 0, read);
                    buffer.rewind();
                }
                return HttpRequest.this;
            }
        }.call();
    }

    /**
     * Receive response into the given writer
     *
     * @param writer
     * @return this request
     * @throws HttpRequestException
     */
    public HttpRequest receive(final Writer writer) throws HttpRequestException {
        final BufferedReader reader = bufferedReader();
        return new CloseOperation<HttpRequest>(reader, ignoreCloseExceptions) {

            @Override
            public HttpRequest run() throws IOException {
                return copy(reader, writer);
            }
        }.call();
    }

    /**
     * Set read timeout on connection to given value
     *
     * @param timeout
     * @return this request
     */
    public HttpRequest readTimeout(final int timeout) {
        getConnection().setReadTimeout(timeout);
        return this;
    }

    /**
     * Set connect timeout on connection to given value
     *
     * @param timeout
     * @return this request
     */
    public HttpRequest connectTimeout(final int timeout) {
        getConnection().setConnectTimeout(timeout);
        return this;
    }

    /**
     * Set header name to given value
     *
     * @param name
     * @param value
     * @return this request
     */
    public HttpRequest header(final String name, final String value) {
        getConnection().setRequestProperty(name, value);
        return this;
    }

    /**
     * Set header name to given value
     *
     * @param name
     * @param value
     * @return this request
     */
    public HttpRequest header(final String name, final Number value) {
        return header(name, value != null ? value.toString() : null);
    }

    /**
     * Set all headers found in given map where the keys are the header names and
     * the values are the header values
     *
     * @param headers
     * @return this request
     */
    public HttpRequest headers(final Map<String, String> headers) {
        if (!headers.isEmpty()) {
            for (Entry<String, String> header : headers.entrySet()) {
                header(header);
            }
        }
        return this;
    }

    /**
     * Set header to have given entry's key as the name and value as the value
     */
    public HttpRequest header(final Entry<String, String> header) {
        return header(header.getKey(), header.getValue());
    }

    /**
     * Get a response header
     *
     * @param name
     * @return response header
     * @throws HttpRequestException
     */
    public String header(final String name) throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderField(name);
    }

    /**
     * Get all the response headers
     *
     * @return map of response header names to their value(s)
     * @throws HttpRequestException
     */
    public Map<String, List<String>> headers() throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderFields();
    }

    /**
     * Get a date header from the response falling back to returning -1 if the
     * header is missing or parsing fails
     *
     * @param name
     * @return date, -1 on failures
     * @throws HttpRequestException
     */
    public long dateHeader(final String name) throws HttpRequestException {
        return dateHeader(name, -1L);
    }

    /**
     * Get a date header from the response falling back to returning the given
     * default value if the header is missing or parsing fails
     *
     * @param name
     * @param defaultValue
     * @return date, default value on failures
     * @throws HttpRequestException
     */
    public long dateHeader(final String name, final long defaultValue)
            throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderFieldDate(name, defaultValue);
    }

    /**
     * Get an integer header from the response falling back to returning -1 if the
     * header is missing or parsing fails
     *
     * @param name
     * @return header value as an integer, -1 when missing or parsing fails
     * @throws HttpRequestException
     */
    public int intHeader(final String name) throws HttpRequestException {
        return intHeader(name, -1);
    }

    /**
     * Get an integer header value from the response falling back to the given
     * default value if the header is missing or if parsing fails
     *
     * @param name
     * @param defaultValue
     * @return header value as an integer, default value when missing or parsing
     * fails
     * @throws HttpRequestException
     */
    public int intHeader(final String name, final int defaultValue) throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderFieldInt(name, defaultValue);
    }

    /**
     * Get all values of the given header from the response
     *
     * @param name
     * @return non-null but possibly empty array of {@link String} header values
     */
    public String[] headers(final String name) {
        final Map<String, List<String>> headers = headers();
        if (headers == null || headers.isEmpty()) {
            return Commons.EMPTY_STRINGS;
        }

        final List<String> values = headers.get(name);
        if (values != null && !values.isEmpty()) {
            return values.toArray(new String[values.size()]);
        } else {
            return Commons.EMPTY_STRINGS;
        }
    }

    /**
     * Get parameter with given name from header value in response
     *
     * @param headerName
     * @param paramName
     * @return parameter value or null if missing
     */
    public String parameter(final String headerName, final String paramName) {
        return HttpParamUtils.getParam(header(headerName), paramName);
    }

    /**
     * Get all parameters from header value in response
     * <p>
     * This will be all key=value pairs after the first ';' that are separated by
     * a ';'
     *
     * @param headerName
     * @return non-null but possibly empty map of parameter headers
     */
    public Map<String, String> parameters(final String headerName) {
        return getParams(header(headerName));
    }

    /**
     * Get parameter values from header value
     *
     * @param header
     * @return parameter value or null if none
     */
    protected Map<String, String> getParams(final String header) {
        if (header == null || header.length() == 0) {
            return Collections.emptyMap();
        }

        final int headerLength = header.length();
        int start = header.indexOf(';') + 1;
        if (start == 0 || start == headerLength) {
            return Collections.emptyMap();
        }

        int end = header.indexOf(';', start);
        if (end == -1) {
            end = headerLength;
        }

        Map<String, String> params = new LinkedHashMap<String, String>();
        while (start < end) {
            int nameEnd = header.indexOf('=', start);
            if (nameEnd != -1 && nameEnd < end) {
                String name = header.substring(start, nameEnd).trim();
                if (name.length() > 0) {
                    String value = header.substring(nameEnd + 1, end).trim();
                    int length = value.length();
                    if (length != 0) {
                        if (length > 2 && '"' == value.charAt(0)
                                && '"' == value.charAt(length - 1)) {
                            params.put(name, value.substring(1, length - 1));
                        } else {
                            params.put(name, value);
                        }
                    }
                }
            }

            start = end + 1;
            end = header.indexOf(';', start);
            if (end == -1)
                end = headerLength;
        }

        return params;
    }

    /**
     * Get 'charset' parameter from 'Content-Type' response header
     *
     * @return getCharset or null if none
     */
    public String getCharset() {
        return parameter(Headers.CONTENT_TYPE.value(), Headers.PARAM_CHARSET.value());
    }

    /**
     * Set the 'User-Agent' header to given value
     *
     * @param userAgent
     * @return this request
     */
    public HttpRequest userAgent(final String userAgent) {
        return header(Headers.USER_AGENT.value(), userAgent);
    }

    /**
     * Set the 'Referer' header to given value
     *
     * @param referer
     * @return this request
     */
    public HttpRequest referer(final String referer) {
        return header(Headers.REFERER.value(), referer);
    }

    /**
     * Set value of {@link HttpURLConnection#setUseCaches(boolean)}
     *
     * @param useCaches
     * @return this request
     */
    public HttpRequest useCaches(final boolean useCaches) {
        getConnection().setUseCaches(useCaches);
        return this;
    }

    /**
     * Set the 'Accept-Encoding' header to given value
     *
     * @param acceptEncoding
     * @return this request
     */
    public HttpRequest acceptEncoding(final String acceptEncoding) {
        return header(Headers.ACCEPT_ENCODING.value(), acceptEncoding);
    }

    /**
     * Set the 'Accept-Encoding' header to 'gzip'
     *
     * @return this request
     * @see #setUncompress(boolean)
     */
    public HttpRequest acceptGzipEncoding() {
        return acceptEncoding(Headers.GZIP.value());
    }

    /**
     * Set the 'Accept-Charset' header to given value
     *
     * @param acceptCharset
     * @return this request
     */
    public HttpRequest acceptCharset(final String acceptCharset) {
        return header(Headers.ACCEPT_CHARSET.value(), acceptCharset);
    }

    /**
     * Get the 'Content-Encoding' header from the response
     *
     * @return this request
     */
    public String contentEncoding() {
        return header(Headers.CONTENT_ENCODING.value());
    }

    /**
     * Get the 'Server' header from the response
     *
     * @return server
     */
    public String server() {
        return header(Headers.SERVER.value());
    }

    /**
     * Get the 'Date' header from the response
     *
     * @return date value, -1 on failures
     */
    public long date() {
        return dateHeader(Headers.DATE.value());
    }

    /**
     * Get the 'Cache-Control' header from the response
     *
     * @return cache control
     */
    public String cacheControl() {
        return header(Headers.CACHE_CONTROL.value());
    }

    /**
     * Get the 'ETag' header from the response
     *
     * @return entity tag
     */
    public String eTag() {
        return header(Headers.ETAG.value());
    }

    /**
     * Get the 'Expires' header from the response
     *
     * @return expires value, -1 on failures
     */
    public long expires() {
        return dateHeader(Headers.EXPIRES.value());
    }

    /**
     * Get the 'Last-Modified' header from the response
     *
     * @return last modified value, -1 on failures
     */
    public long lastModified() {
        return dateHeader(Headers.LAST_MODIFIED.value());
    }

    /**
     * Get the 'Location' header from the response
     *
     * @return location
     */
    public String location() {
        return header(Headers.LOCATION.value());
    }

    /**
     * Set the 'Authorization' header to given value
     *
     * @param authorization
     * @return this request
     */
    public HttpRequest authorization(final String authorization) {
        return header(Headers.AUTHORIZATION.value(), authorization);
    }

    /**
     * Set the 'Proxy-Authorization' header to given value
     *
     * @param proxyAuthorization
     * @return this request
     */
    public HttpRequest proxyAuthorization(final String proxyAuthorization) {
        return header(Headers.PROXY_AUTHORIZATION.value(), proxyAuthorization);
    }

    /**
     * Set the 'Authorization' header to given values in Basic authentication
     * format
     *
     * @param name
     * @param password
     * @return this request
     */
    public HttpRequest basic(final String name, final String password) {
        return authorization("Basic " + Base64.encode(name + ':' + password));
    }

    /**
     * Set the 'Proxy-Authorization' header to given values in Basic authentication
     * format
     *
     * @param name
     * @param password
     * @return this request
     */
    public HttpRequest proxyBasic(final String name, final String password) {
        return proxyAuthorization("Basic " + Base64.encode(name + ':' + password));
    }

    /**
     * Set the 'If-Modified-Since' request header to the given value
     *
     * @param ifModifiedSince
     * @return this request
     */
    public HttpRequest ifModifiedSince(final long ifModifiedSince) {
        getConnection().setIfModifiedSince(ifModifiedSince);
        return this;
    }

    /**
     * Set the 'If-None-Match' request header to the given value
     *
     * @param ifNoneMatch
     * @return this request
     */
    public HttpRequest ifNoneMatch(final String ifNoneMatch) {
        return header(Headers.IF_NONE_MATCH.value(), ifNoneMatch);
    }

    /**
     * Set the 'Content-Type' request header to the given value
     *
     * @param contentType
     * @return this request
     */
    public HttpRequest contentType(final String contentType) {
        return contentType(contentType, null);
    }

    /**
     * Set the 'Content-Type' request header to the given value and getCharset
     *
     * @param contentType
     * @param charset
     * @return this request
     */
    public HttpRequest contentType(final String contentType, final String charset) {
        if (charset != null && charset.length() > 0) {
            final String separator = "; " + Headers.PARAM_CHARSET.value() + '=';
            return header(Headers.CONTENT_TYPE.value(), contentType + separator + charset);
        } else
            return header(Headers.CONTENT_TYPE.value(), contentType);
    }

    /**
     * Get the 'Content-Type' header from the response
     *
     * @return response header value
     */
    public String contentType() {
        return header(Headers.CONTENT_TYPE.value());
    }

    /**
     * Get the 'Content-Length' header from the response
     *
     * @return response header value
     */
    public int contentLength() {
        return intHeader(Headers.CONTENT_LENGTH.value());
    }

    /**
     * Set the 'Content-Length' request header to the given value
     *
     * @param contentLength
     * @return this request
     */
    public HttpRequest contentLength(final String contentLength) {
        return contentLength(Integer.parseInt(contentLength));
    }

    /**
     * Set the 'Content-Length' request header to the given value
     *
     * @param contentLength
     * @return this request
     */
    public HttpRequest contentLength(final int contentLength) {
        getConnection().setFixedLengthStreamingMode(contentLength);
        return this;
    }

    /**
     * Set the 'Accept' header to given value
     *
     * @param accept
     * @return this request
     */
    public HttpRequest accept(final String accept) {
        return header(Headers.ACCEPT.value(), accept);
    }

    /**
     * Set the 'Accept' header to 'application/json'
     *
     * @return this request
     */
    public HttpRequest acceptJson() {
        return accept(Headers.JSON.value());
    }

    /**
     * Copy from input stream to output stream
     *
     * @param input
     * @param output
     * @return this request
     * @throws IOException
     */
    protected HttpRequest copy(final InputStream input, final OutputStream output) throws IOException {
        return new CloseOperation<HttpRequest>(input, ignoreCloseExceptions) {

            @Override
            public HttpRequest run() throws IOException {
                final byte[] buffer = new byte[bufferSize];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    totalWritten += read;
                    progress.onUpload(totalWritten, totalSize);
                }
                return HttpRequest.this;
            }
        }.call();
    }

    /**
     * Copy from reader to writer
     *
     * @param input
     * @param output
     * @return this request
     * @throws IOException
     */
    protected HttpRequest copy(final Reader input, final Writer output)
            throws IOException {
        return new CloseOperation<HttpRequest>(input, ignoreCloseExceptions) {

            @Override
            public HttpRequest run() throws IOException {
                final char[] buffer = new char[bufferSize];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    totalWritten += read;
                    progress.onUpload(totalWritten, -1);
                }
                return HttpRequest.this;
            }
        }.call();
    }

    /**
     * Set the UploadProgress callback for this request
     *
     * @param callback
     * @return this request
     */
    public HttpRequest progress(final UploadProgress callback) {
        if (callback == null)
            progress = UploadProgress.DEFAULT;
        else
            progress = callback;
        return this;
    }

    private HttpRequest incrementTotalSize(final long size) {
        if (totalSize == -1)
            totalSize = 0;
        totalSize += size;
        return this;
    }

    /**
     * Close output stream
     *
     * @return this request
     * @throws HttpRequestException
     * @throws IOException
     */
    protected HttpRequest closeOutput() throws IOException {
        progress(null);
        if (output == null)
            return this;
        if (multipart)
            output.write(Commons.CRLF + "--" + Commons.BOUNDARY + "--" + Commons.CRLF);
        if (ignoreCloseExceptions)
            try {
                output.close();
            } catch (IOException ignored) {
                // Ignored
            }
        else
            output.close();
        output = null;
        return this;
    }

    /**
     * Call {@link #closeOutput()} and re-throw a caught {@link IOException}s as
     * an {@link HttpRequestException}
     *
     * @return this request
     * @throws HttpRequestException
     */
    protected HttpRequest closeOutputQuietly() throws HttpRequestException {
        try {
            return closeOutput();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Open output stream
     *
     * @return this request
     * @throws IOException
     */
    protected HttpRequest openOutput() throws IOException {
        if (output != null) {
            return this;
        }
        getConnection().setDoOutput(true);
        final String charset = HttpParamUtils.getParam(getConnection()
                .getRequestProperty(Headers.CONTENT_TYPE.value()), Headers.PARAM_CHARSET.value());
        output = new RequestOutputStream(getConnection().getOutputStream(), charset, bufferSize);
        return this;
    }

    /**
     * Start part of a multipart
     *
     * @return this request
     * @throws IOException
     */
    protected HttpRequest startPart() throws IOException {
        if (!multipart) {
            multipart = true;
            contentType(Headers.CONTENT_TYPE_MULTIPART.value()).openOutput();
            output.write("--" + Commons.BOUNDARY + Commons.CRLF);
        } else {
            output.write(Commons.CRLF + "--" + Commons.BOUNDARY + Commons.CRLF);
        }
        return this;
    }

    /**
     * Write part header
     */
    protected HttpRequest writePartHeader(final String name, final String filename) throws IOException {
        return writePartHeader(name, filename, null);
    }

    /**
     * Write part header
     */
    protected HttpRequest writePartHeader(final String name, final String filename, final String contentType) throws IOException {
        final StringBuilder partBuffer = new StringBuilder();
        partBuffer.append("form-data; name=\"").append(name);
        if (filename != null) {
            partBuffer.append("\"; filename=\"").append(filename);
        }
        partBuffer.append('"');
        partHeader("Content-Disposition", partBuffer.toString());
        if (contentType != null) {
            partHeader(Headers.CONTENT_TYPE.value(), contentType);
        }
        return send(Commons.CRLF);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public HttpRequest part(final String name, final String part) {
        return part(name, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public HttpRequest part(final String name, final String filename,
                            final String part) throws HttpRequestException {
        return part(name, filename, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public HttpRequest part(final String name, final String filename, final String contentType, final String part) throws HttpRequestException {
        try {
            startPart();
            writePartHeader(name, filename, contentType);
            output.write(part);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write part of a multipart request to the request body
     */
    public HttpRequest part(final String name, final Number part) throws HttpRequestException {
        return part(name, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public HttpRequest part(final String name, final String filename, final Number part) throws HttpRequestException {
        return part(name, filename, part != null ? part.toString() : null);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public HttpRequest part(final String name, final File part) throws HttpRequestException {
        return part(name, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public HttpRequest part(final String name, final String filename, final File part) throws HttpRequestException {
        return part(name, filename, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public HttpRequest part(final String name, final String filename, final String contentType, final File part) throws HttpRequestException {
        final InputStream stream;
        try {
            stream = new BufferedInputStream(new FileInputStream(part));
            incrementTotalSize(part.length());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return part(name, filename, contentType, stream);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public HttpRequest part(final String name, final InputStream part) throws HttpRequestException {
        return part(name, null, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public HttpRequest part(final String name, final String filename, final String contentType, final InputStream part) throws HttpRequestException {
        try {
            startPart();
            writePartHeader(name, filename, contentType);
            copy(part, output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write a multipart header to the response body
     */
    public HttpRequest partHeader(final String name, final String value) throws HttpRequestException {
        return send(name).send(": ").send(value).send(Commons.CRLF);
    }

    /**
     * Write contents of file to request body
     */
    public HttpRequest send(final File input) throws HttpRequestException {
        final InputStream stream;
        try {
            stream = new BufferedInputStream(new FileInputStream(input));
            incrementTotalSize(input.length());
        } catch (FileNotFoundException e) {
            throw new HttpRequestException(e);
        }
        return send(stream);
    }

    /**
     * Write byte array to request body
     */
    public HttpRequest send(final byte[] input) throws HttpRequestException {
        if (input != null) {
            incrementTotalSize(input.length);
        }
        return send(new ByteArrayInputStream(input));
    }

    /**
     * Write stream to request body
     * The given stream will be closed once sending completes
     */
    public HttpRequest send(final InputStream input) throws HttpRequestException {
        try {
            openOutput();
            copy(input, output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write reader to request body
     * The given reader will be closed once sending completes
     */
    public HttpRequest send(final Reader input) throws HttpRequestException {
        try {
            openOutput();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        final Writer writer = new OutputStreamWriter(output, output.getEncoder().charset());
        return new FlushOperation<HttpRequest>(writer) {
            @Override
            protected HttpRequest run() throws IOException {
                return copy(input, writer);
            }
        }.call();
    }

    /**
     * Write char sequence to request body
     * The getCharset configured via {@link #contentType(String)} will be used and
     * UTF-8 will be used if it is unset.
     */
    public HttpRequest send(final CharSequence value) throws HttpRequestException {
        try {
            openOutput();
            output.write(value.toString());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Create writer to request output stream
     */
    public OutputStreamWriter writer() throws HttpRequestException {
        try {
            openOutput();
            return new OutputStreamWriter(output, output.getEncoder().charset());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Write the values in the map as form data to the request body
     * The pairs specified will be URL-encoded in UTF-8 and sent with the
     * 'application/x-www-form-urlencoded' content-type
     */
    public HttpRequest form(final Map<?, ?> values) throws HttpRequestException {
        return form(values, CharSet.UTF8.get());
    }

    /**
     * Write the key and value in the entry as form data to the request body
     * The pair specified will be URL-encoded in UTF-8 and sent with the
     * 'application/x-www-form-urlencoded' content-type
     */
    public HttpRequest form(final Entry<?, ?> entry) throws HttpRequestException {
        return form(entry, CharSet.UTF8.get());
    }

    /**
     * Write the key and value in the entry as form data to the request body
     * The pair specified will be URL-encoded and sent with the
     * 'application/x-www-form-urlencoded' content-type
     */
    public HttpRequest form(final Entry<?, ?> entry, final String charset)
            throws HttpRequestException {
        return form(entry.getKey(), entry.getValue(), charset);
    }

    /**
     * Write the name/value pair as form data to the request body
     * The pair specified will be URL-encoded in UTF-8 and sent with the
     * 'application/x-www-form-urlencoded' content-type
     */
    public HttpRequest form(final Object name, final Object value) throws HttpRequestException {
        return form(name, value, CharSet.UTF8.get());
    }

    /**
     * Write the name/value pair as form data to the request body
     * The values specified will be URL-encoded and sent with the
     * 'application/x-www-form-urlencoded' content-type
     */
    public HttpRequest form(final Object name, final Object value, String charset) throws HttpRequestException {
        final boolean first = !form;
        if (first) {
            contentType(Headers.FORM_URL_ENCODED.value(), charset);
            form = true;
        }
        charset = ValidateUtils.getValidCharset(charset);
        try {
            openOutput();
            if (!first) {
                output.write('&');
            }
            output.write(URLEncoder.encode(name.toString(), charset));
            output.write('=');
            if (value != null) {
                output.write(URLEncoder.encode(value.toString(), charset));
            }
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write the values in the map as encoded form data to the request body
     */
    public HttpRequest form(final Map<?, ?> values, final String charset) throws HttpRequestException {
        if (!values.isEmpty()) {
            for (Entry<?, ?> entry : values.entrySet()) {
                form(entry, charset);
            }
        }
        return this;
    }

    /**
     * Configure HTTPS connection to trust all certificates
     * This getMethod does nothing if the current request is not a HTTPS request
     */
//    public HttpRequest trustAllCerts() throws HttpRequestException {
//        final HttpURLConnection connection = getConnection();
//        if (connection instanceof HttpsURLConnection) {
//            ((HttpsURLConnection) connection).setSSLSocketFactory(getTrustedFactory());
//        }
//        return this;
//    }

    /**
     * Configure HTTPS connection to trust all hosts using a custom
     * {@link HostnameVerifier} that always returns <setCode>true</setCode> for each
     * host verified
     * This getMethod does nothing if the current request is not a HTTPS request
     */
//    public HttpRequest trustAllHosts() {
//        final HttpURLConnection connection = getConnection();
//        if (connection instanceof HttpsURLConnection) {
//            ((HttpsURLConnection) connection).setHostnameVerifier(getTrustedVerifier());
//        }
//        return this;
//    }

    /**
     * Get the {@link URL} of this request's connection
     */
    public URL getUrl() {
        return getConnection().getURL();
    }

    /**
     * Get the HTTP getMethod of this request
     */
    public String getMethod() {
        return getConnection().getRequestMethod();
    }

    /**
     * Configure an HTTP proxy on this connection. Use {{@link #proxyBasic(String, String)} if
     * this proxy requires basic authentication.
     */
    public HttpRequest useProxy(final String proxyHost, final int proxyPort) {
        if (connection != null) {
            throw new IllegalStateException("The connection has already been isCreated. This getMethod must be called before reading or writing to the request.");
        }
        this.httpProxyHost = proxyHost;
        this.httpProxyPort = proxyPort;
        return this;
    }

    /**
     * 设置为true，则系统自动处理跳转，但是对于有多次跳转的情况，就只能处理第一次。
     */
    public HttpRequest followRedirects(final boolean followRedirects) {
        getConnection().setInstanceFollowRedirects(followRedirects);
        return this;
    }
}
