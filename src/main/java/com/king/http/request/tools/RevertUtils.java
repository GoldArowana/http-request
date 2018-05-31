package com.king.http.request.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 金龙
 * @date 2018/5/31 at 上午11:14
 */
public class RevertUtils {
    /**
     * Represents array of any type as list of objects so we can easily iterate over it
     *
     * @param array of elements
     * @return list with the same elements
     */
    public static List<Object> arrayToList(final Object array) {
        if (array instanceof Object[])
            return Arrays.asList((Object[]) array);

        List<Object> result = new ArrayList<Object>();
        // Arrays of the primitive types can't be cast to array of Object, so this:
        if (array instanceof int[])
            for (int value : (int[]) array) result.add(value);
        else if (array instanceof boolean[])
            for (boolean value : (boolean[]) array) result.add(value);
        else if (array instanceof long[])
            for (long value : (long[]) array) result.add(value);
        else if (array instanceof float[])
            for (float value : (float[]) array) result.add(value);
        else if (array instanceof double[])
            for (double value : (double[]) array) result.add(value);
        else if (array instanceof short[])
            for (short value : (short[]) array) result.add(value);
        else if (array instanceof byte[])
            for (byte value : (byte[]) array) result.add(value);
        else if (array instanceof char[])
            for (char value : (char[]) array) result.add(value);
        return result;
    }
}
