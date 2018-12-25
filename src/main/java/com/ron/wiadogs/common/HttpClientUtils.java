package com.ron.wiadogs.common;


import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.BestMatchSpecFactory;
import org.apache.http.impl.cookie.BrowserCompatSpecFactory;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

public class HttpClientUtils {


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

    /**
     * 发送HTTPS	POST请求
     *
     * @param url 要访问的HTTPS地址,POST访问的参数Map对象
     * @return  返回响应值
     * */
    public static final String sendHttpsRequestByPost(String url, Map<String, String> params) {
        String responseContent = null;
        HttpClient httpClient = new DefaultHttpClient();
        //创建TrustManager
        X509TrustManager xtm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        //这个好像是HOST验证
        X509HostnameVerifier hostnameVerifier = new X509HostnameVerifier() {
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
            public void verify(String arg0, SSLSocket arg1) throws IOException {}
            public void verify(String arg0, String[] arg1, String[] arg2) throws SSLException {}
            public void verify(String arg0, X509Certificate arg1) throws SSLException {}
        };
        try {
            //TLS1.0与SSL3.0基本上没有太大的差别，可粗略理解为TLS是SSL的继承者，但它们使用的是相同的SSLContext
            SSLContext ctx = SSLContext.getInstance("TLS");
            //使用TrustManager来初始化该上下文，TrustManager只是被SSL的Socket所使用
            ctx.init(null, new TrustManager[] { xtm }, null);
            //创建SSLSocketFactory
            org.apache.http.conn.ssl.SSLSocketFactory socketFactory = new SSLSocketFactory(ctx);
            socketFactory.setHostnameVerifier(hostnameVerifier);
            //通过SchemeRegistry将SSLSocketFactory注册到我们的HttpClient上
            httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", socketFactory, 443));
            HttpPost httpPost = new HttpPost(url);
            List<NameValuePair> formParams = new ArrayList<NameValuePair>(); // 构建POST请求的表单参数
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity(); // 获取响应实体
            if (entity != null) {
                responseContent = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭连接,释放资源
            httpClient.getConnectionManager().shutdown();
        }
        return responseContent;
    }

    public static void printResponse(HttpResponse httpResponse)
            throws ParseException, IOException {
        // 获取响应消息实体
        HttpEntity entity = httpResponse.getEntity();
        // 响应状态
        System.out.println("status:" + httpResponse.getStatusLine());
        System.out.println("headers:");
        HeaderIterator iterator = httpResponse.headerIterator();
        while (iterator.hasNext()) {
            System.out.println("\t" + iterator.next());
        }
        // 判断响应实体是否为空
        if (entity != null) {
            String responseString = EntityUtils.toString(entity);
            System.out.println("response length:" + responseString.length());
            System.out.println("response content:"
                    + responseString.replace("\r\n", ""));
        }
    }

    public static void setContext() {
        System.out.println("----setContext");
        context = HttpClientContext.create();
        Registry<CookieSpecProvider> registry = RegistryBuilder
                .<CookieSpecProvider> create()
                .register(CookieSpecs.BEST_MATCH, new BestMatchSpecFactory())
                .register(CookieSpecs.BROWSER_COMPATIBILITY,
                        new BrowserCompatSpecFactory()).build();
        context.setCookieSpecRegistry(registry);
        context.setCookieStore(cookieStore);
    }

    public static void setCookieStore(HttpResponse httpResponse) {
        System.out.println("----setCookieStore");
        cookieStore = new BasicCookieStore();
        // JSESSIONID
        String setCookie = httpResponse.getFirstHeader("Set-Cookie").getValue();
        String JSESSIONID = setCookie.substring("JSESSIONID=".length(),
                setCookie.indexOf(";"));
        System.out.println("JSESSIONID:" + JSESSIONID);
        // 新建一个Cookie
        BasicClientCookie cookie = new BasicClientCookie("JSESSIONID",
                JSESSIONID);
        cookie.setVersion(0);
        cookie.setDomain(".xiaojukeji.com");
        cookie.setPath("/");
        cookieStore.addCookie(cookie);
    }

    public static void setCookieStore(String phpSessionId) {
        System.out.println("----setCookieStore");
        cookieStore = new BasicCookieStore();
        System.out.println("phpSessionId:" + phpSessionId);
        // 新建一个Cookie
        BasicClientCookie cookie = new BasicClientCookie("PHPSESSID",
                phpSessionId);
        cookie.setVersion(0);
        cookie.setDomain("http://112.74.60.174");
        cookie.setPath("/");
        cookieStore.addCookie(cookie);
    }

    public static List<NameValuePair> getParam(Map parameterMap) {
        List<NameValuePair> param = new ArrayList<NameValuePair>();
        Iterator it = parameterMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry parmEntry = (Map.Entry) it.next();
            param.add(new BasicNameValuePair((String) parmEntry.getKey(),
                    (String) parmEntry.getValue()));
        }
        return param;
    }

    /**
     * 创建一个client对象，通过cookie
     */
    public static CloseableHttpClient singleCloseableHttpClient() {
        CloseableHttpClient client = HttpClients.custom()
                .setDefaultCookieStore(cookieStore).build();
        return client;
    }

    public static String getResponseContent(HttpResponse httpResponse)
            throws ParseException, IOException {
        String res = null;
        // 获取响应消息实体
        HttpEntity entity = httpResponse.getEntity();
        // 响应状态
        System.out.println("status:" + httpResponse.getStatusLine());
        System.out.println("headers:");
        // 判断响应实体是否为空
        if (entity != null) {
            String responseString = EntityUtils.toString(entity);
            System.out.println("response length:" + responseString.length());
            System.out.println("response content:"
                    + responseString.replace("\r\n", ""));
            res = responseString.replace("\r\n", "");
        }
        return res;
    }

    /**
     * get方法 raopeng 2017年8月25日 下午4:39:38
     *
     * @param url
     * @return
     */
    public static String httpGet(String url, Map<String, String> headers) {
        String text = null;
        // 进行登陆后的操作
        try {
            HttpGet httpGet = new HttpGet(url);
            if (headers != null) {
                for (String string : headers.keySet()) {
                    httpGet.setHeader(string, headers.get(string));
                }
            }
            //设置请求和传输超时时间
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(100000).setConnectTimeout(100000).build();
            httpGet.setConfig(requestConfig);
            HttpResponse httpResponse1 = HttpClientUtils.singleCloseableHttpClient().execute(
                    httpGet);
            // 获取响应对象中的响应码
            StatusLine statusLine = httpResponse1.getStatusLine();// 获取请求对象中的响应行对象
            int responseCode = statusLine.getStatusCode();// 从状态行中获取状态码
            if (responseCode == 200) {
                HttpEntity entity = httpResponse1.getEntity();
                // EntityUtils中的toString()方法转换服务器的响应数据
                text = EntityUtils.toString(entity, "utf-8");
                System.out.println("服务器的响应是:" + text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }


    public static void getToken(){
        String url = "http://112.74.60.174/user/login.html";
        Map<String, String> header = new HashMap<String, String>();
        header.put("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        header.put("Accept-Encoding", "gzip, deflate");
        header.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/16A404 Html5Plus/1.0 (Immersed/44)");
        header.put("Origin", "http://112.74.60.174");
        header.put("Accept-Language", "zh-cn");
        header.put("Connection", "keep-alive");
        header.put("Host", "112.74.60.174");
        header.put("Upgrade-Insecure-Requests", "1");
        try {

            // 执行post请求
            String res = httpGet(url, header);
            System.out.println(res);
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    public static void login(){
        String url = "http://112.74.60.174/user/do_zlllogin.html?time="+new Date().getTime()+"&token=81e9a1b82fc188330ae5a49ef2f2802b&t=0.4530087492717504";
        Map<String, String> header = new HashMap<String, String>();
        header.put("Accept",
                "application/x-www-form-urlencoded; charset=UTF-8");
        header.put("Accept-Encoding", "gzip, deflate");
        header.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/16A404 Html5Plus/1.0 (Immersed/44)");
        header.put("Origin", "http://112.74.60.174");
        header.put("Accept-Language", "zh-cn");
        header.put("Connection", "keep-alive");
        header.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        header.put("Host", "112.74.60.174");
        header.put("Referer", "http://112.74.60.174/user/login.html");
        header.put("X-Requested-With", "XMLHttpRequest");
        Map<String, String> parameterMap = new HashMap();
        parameterMap.put("referurl", "http://112.74.60.174/index/index.html");
        parameterMap.put("type", "accountLogin");
        parameterMap.put("username", "13031107134");
        parameterMap.put("password", "raopeng0514");
        try {
            HttpPost httpPost = new HttpPost(url);
            for (String string : header.keySet()) {
                httpPost.setHeader(string, header.get(string));
            }
            UrlEncodedFormEntity postEntity = new UrlEncodedFormEntity(
                    getParam(parameterMap), "UTF-8");
            httpPost.setEntity(postEntity);

            // 执行post请求
            HttpResponse httpResponse = httpClient.execute(httpPost);
            setCookieStore(httpResponse);
            printResponse(httpResponse);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args){
        String url = "http://112.74.60.174/index/ajaxjsonindex.html";
        Map<String, String> header = new HashMap<String, String>();
        header.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/16A404 Html5Plus/1.0 (Immersed/44)");

        header.put("Accept",
                "application/x-www-form-urlencoded; charset=UTF-8");
        header.put("Accept-Encoding", "gzip, deflate");
        header.put("Accept-Language", "zh-cn");
        header.put("Connection", "keep-alive");
        header.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        header.put("Host", "112.74.60.174");
        header.put("Referer", "http://112.74.60.174/index/index");
        header.put("X-Requested-With", "XMLHttpRequest");
        try {
            HttpClientUtils.getToken();
//            HttpClientUtils.login();

//            setCookieStore("b5ntm9n8vho8kd8td0iqb68smk");
//            HttpClientUtils.httpGet(url,header);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
