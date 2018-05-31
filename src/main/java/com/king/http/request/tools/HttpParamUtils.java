package com.king.http.request.tools;

import java.util.Iterator;

import static com.king.http.request.tools.RevertUtils.arrayToList;

/**
 * @author 金龙
 * @date 2018/5/31 at 上午11:14
 */
public class HttpParamUtils {
    public static StringBuilder addParamPrefix(final String baseUrl, final StringBuilder result) {
        // Add '?' if missing and add '&' if params already exist in base url
        final int queryStart = baseUrl.indexOf('?');
        final int lastChar = result.length() - 1;
        if (queryStart == -1) {
            result.append('?');
        } else if (queryStart < lastChar && baseUrl.charAt(lastChar) != '&') {
            result.append('&');
        }
        return result;
    }

    public static StringBuilder addParam(final Object key, Object value, final StringBuilder result) {
        if (value != null && value.getClass().isArray()) {
            value = arrayToList(value);
        }

        if (value instanceof Iterable<?>) {
            Iterator<?> iterator = ((Iterable<?>) value).iterator();
            while (iterator.hasNext()) {
                result.append(key);
                result.append("[]=");
                Object element = iterator.next();
                if (element != null)
                    result.append(element);
                if (iterator.hasNext())
                    result.append("&");
            }
        } else {
            result.append(key);
            result.append("=");
            if (value != null)
                result.append(value);
        }

        return result;
    }

    /**
     * Get parameter value from header value
     */
    public static String getParam(final String value, final String paramName) {
        if (value == null || value.length() == 0)
            return null;

        final int length = value.length();
        int start = value.indexOf(';') + 1;
        if (start == 0 || start == length)
            return null;

        int end = value.indexOf(';', start);
        if (end == -1)
            end = length;

        while (start < end) {
            int nameEnd = value.indexOf('=', start);
            if (nameEnd != -1 && nameEnd < end
                    && paramName.equals(value.substring(start, nameEnd).trim())) {
                String paramValue = value.substring(nameEnd + 1, end).trim();
                int valueLength = paramValue.length();
                if (valueLength != 0)
                    if (valueLength > 2 && '"' == paramValue.charAt(0)
                            && '"' == paramValue.charAt(valueLength - 1))
                        return paramValue.substring(1, valueLength - 1);
                    else
                        return paramValue;
            }

            start = end + 1;
            end = value.indexOf(';', start);
            if (end == -1)
                end = length;
        }

        return null;
    }
}
