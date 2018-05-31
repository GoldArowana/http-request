package com.king.http;

import com.king.http.request.HttpRequest;
import org.junit.Test;

/**
 * @author 金龙
 * @date 2018/5/31 at 上午10:37
 */
public class HttpRequestTest {
    @Test
    public void constructor(){
        HttpRequest req1 =  new HttpRequest("http://www.baidu.com","GET");
    }

    @Test
    public void getBody() {
        HttpRequest req = new HttpRequest("http://www.baidu.com", "GET");
        String body = req.body();
        System.out.println(body);
    }
}
