package com.king.http;

import com.king.http.request.lib.CharSet;
import org.junit.Test;


/**
 * @author 金龙
 * @date 2018/5/30 at 下午4:20
 */
public class TestCharSet {

    @Test
    public void test() {
        System.out.println(CharSet.UTF8);
        System.out.println(CharSet.UTF8.get());
        System.out.println(CharSet.valueOf("UTF8").get());
    }
}
