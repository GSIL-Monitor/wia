package com.ron.wiadogs.common;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * 狗信息相关类
 */
public class DogUtils {

    // 创建CookieStore实例
    static CookieStore cookieStore = null;
    static HttpClientContext context = null;
    private static CloseableHttpClient httpClient;

    static {
        cookieStore = new BasicCookieStore();
        // 将CookieStore设置到httpClient中
        httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore)
                .build();
    }

    public static void getDogList(){

    }
}
