package com.king.http.request.tools;

import com.king.http.request.HttpRequest;
import com.king.http.request.eception.HttpRequestException;
import com.king.jdk.net.URI;
import com.king.jdk.net.URL;
import com.king.jdk.net.exception.URISyntaxException;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static com.king.http.request.tools.HttpParamUtils.addParam;
import static com.king.http.request.tools.HttpParamUtils.addParamPrefix;

/**
 * @author 金龙
 * @date 2018/5/31 at 上午11:13
 */
public class HttpPathUtils {
    public static StringBuilder addPathSeparator(final String baseUrl, final StringBuilder result) {
        // Add trailing slash if the base URL doesn't have any path segments.
        //
        // The following test is checking for the last slash not being part of
        // the protocol to host separator: '://'.
        if (baseUrl.indexOf(':') + 2 == baseUrl.lastIndexOf('/')) {
            result.append('/');
        }
        return result;
    }

    /**
     * Encode the given URL as an ASCII {@link String}
     * <p>
     * This method ensures the path and query segments of the URL are properly
     * encoded such as ' ' characters being encoded to '%20' or any UTF-8
     * characters that are non-ASCII. No value of URLs is done by default by
     * the {@link HttpRequest} constructors and so if URL value is needed this
     * method should be called before calling the {@link HttpRequest} constructor.
     *
     * @param url
     * @return encoded URL
     * @throws HttpRequestException
     */
    public static String encode(final CharSequence url) throws HttpRequestException, URISyntaxException {
        URL parsed;
        try {
            parsed = new URL(url.toString());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }

        String host = parsed.getHost();
        int port = parsed.getPort();
        if (port != -1) {
            host = host + ':' + Integer.toString(port);
        }

        try {
            String encoded = new URI(parsed.getProtocol(), host, parsed.getPath(), parsed.getQuery(), null).toASCIIString();
            int paramsStart = encoded.indexOf('?');
            if (paramsStart > 0 && paramsStart + 1 < encoded.length()) {
                encoded = encoded.substring(0, paramsStart + 1)
                        + encoded.substring(paramsStart + 1).replace("+", "%2B");
            }
            return encoded;
        } catch (URISyntaxException e) {
            IOException io = new IOException("Parsing URI failed");
            io.initCause(e);
            throw new HttpRequestException(io);
        }
    }

    /**
     * Append given map as query parameters to the base URL
     * <p>
     * Each map entry's key will be a parameter name and the value's
     * {@link Object#toString()} will be the parameter value.
     *
     * @param url
     * @param params
     * @return URL with appended query params
     */
    public static String append(final CharSequence url, final Map<?, ?> params) {
        final String baseUrl = url.toString();
        if (params == null || params.isEmpty())
            return baseUrl;

        final StringBuilder result = new StringBuilder(baseUrl);

        addPathSeparator(baseUrl, result);
        addParamPrefix(baseUrl, result);

        Map.Entry<?, ?> entry;
        Iterator<?> iterator = params.entrySet().iterator();
        entry = (Map.Entry<?, ?>) iterator.next();
        addParam(entry.getKey().toString(), entry.getValue(), result);

        while (iterator.hasNext()) {
            result.append('&');
            entry = (Map.Entry<?, ?>) iterator.next();
            addParam(entry.getKey().toString(), entry.getValue(), result);
        }

        return result.toString();
    }


    /**
     * Append given name/value pairs as query parameters to the base URL
     * <p>
     * The params argument is interpreted as a sequence of name/value pairs so the
     * given number of params must be divisible by 2.
     *
     * @param url
     * @param params name/value pairs
     * @return URL with appended query params
     */
    public static String append(final CharSequence url, final Object... params) {
        final String baseUrl = url.toString();
        if (params == null || params.length == 0)
            return baseUrl;

        if (params.length % 2 != 0)
            throw new IllegalArgumentException(
                    "Must specify an even number of parameter names/values");

        final StringBuilder result = new StringBuilder(baseUrl);

        addPathSeparator(baseUrl, result);
        addParamPrefix(baseUrl, result);

        addParam(params[0], params[1], result);

        for (int i = 2; i < params.length; i += 2) {
            result.append('&');
            addParam(params[i], params[i + 1], result);
        }

        return result.toString();
    }
}
