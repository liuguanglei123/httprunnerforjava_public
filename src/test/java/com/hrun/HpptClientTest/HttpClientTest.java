package com.hrun.HpptClientTest;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Slf4j
public class HttpClientTest {

    @Test
    public void httpClientTest_Response() throws Exception {

        // 添加代理
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                return true;
            }
        }).build();

        HttpHost proxy = new HttpHost("127.0.0.1", 8888, "http");
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setProxy(proxy)
                .build();

        String url = "https://m.ydl.com/user/login";
        URIBuilder uriBuilder = new URIBuilder(url);

        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(defaultRequestConfig)
                .setSSLContext(sslContext).build();
        HttpGet httpget = new HttpGet(uriBuilder.build());

        HttpResponse response = httpClient.execute(httpget);
        HttpEntity entity = response.getEntity();
        System.out.println(EntityUtils.toString(response.getEntity()));
    }

}
