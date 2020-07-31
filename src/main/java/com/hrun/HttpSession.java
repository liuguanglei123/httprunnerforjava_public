package com.hrun;

import com.alibaba.fastjson.JSONObject;
import com.hrun.component.Meta_data.Meta_data;
import com.hrun.component.Meta_data.ResultData;
import com.hrun.component.common.*;
import com.hrun.lazyContent.LazyContent;
import lombok.Data;
import org.apache.http.*;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Stream;

@Data
public class HttpSession {

    static Logger logger = LoggerFactory.getLogger(HttpSession.class);

    private BasicCookieStore cookieStore;

    private CloseableHttpClient httpclient;

    private Meta_data meta_data;

    public HttpSession() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        // 添加代理
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                return true;
            }
        }).build();

        HttpHost proxy = new HttpHost("127.0.0.1", 8888, "http");
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(5000).setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
//                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .setProxy(proxy)
                .build();

        //支持cookie上下文数据的保存
        this.cookieStore = new BasicCookieStore();
        this.httpclient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setDefaultRequestConfig(defaultRequestConfig)
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

        init_meta_data();
    }

    public void init_meta_data(){
        //TODO 再分析一下，里面的数据到底做了什么？
        meta_data = new Meta_data();
    }

    public ResultData get_req_resp_record(Request req_obj, HttpResponse resp_obj){
        ResultData resultData = new ResultData();
        resultData.getRequest().put("url",req_obj.getUrl().getRealValue());
        resultData.getRequest().put("method",req_obj.getMethod().getRealValue());
        resultData.getRequest().put("headers",req_obj.getHeaders().toMap());

        //TODo: 文件上传
        if(req_obj.getFormData() != null)
            resultData.getRequest().put("formData",req_obj.getFormData().toMap());

        if(req_obj.getJsonData() != null)
            resultData.getRequest().put("jsonData",req_obj.getJsonData().toMap());

        log_print(resultData.getRequest(), "request");

        // record response info
        // TODO: httpclient的response结构和python的结构区别很大，很多字段都没有
        resultData.getResponse().put("status_code",resp_obj.getStatusLine().getStatusCode());
        StringBuffer cookieStr = new StringBuffer();
        cookieStore.getCookies().stream().forEach( cookie ->
                cookieStr.append(cookie.getName() + ":" + cookie.getValue())
        );
        resultData.getResponse().put("cookies",cookieStr);
        resultData.getResponse().put("encoding",resp_obj.getEntity().getContentEncoding());
        StringBuffer headerStr = new StringBuffer();
        Stream.of(resp_obj.getAllHeaders()).forEach( each ->
                headerStr.append(each.getName() + ":" + each.getValue())
        );
        resultData.getResponse().put("headers",headerStr);
        resultData.getResponse().put("content_type",resp_obj.getEntity().getContentType().getElements()[0].toString());
        // TODO:  if "image" in content_type:
        // try to record json data
        try {
            //TODO:body如何获取

            String tmp = EntityUtils.toString(resp_obj.getEntity());
            tmp = Utils.decodeUnicode(tmp);
            resultData.getResponse().put("body",tmp);


//            InputStream instreams = resp_obj.getEntity().getContent();
//            String str = Utils.convertStreamToString(instreams);
//            String tmp = Utils.decodeUnicode(str);
//            resultData.getResponse().put("body",tmp);

//            String tmp = EntityUtils.toString(resp_obj.getEntity());
//            resultData.getResponse().put("body", Utils.unicodeToCn(tmp));
        }catch(IOException | UnsupportedOperationException e){
        }

        log_print(resultData.getResponse(), "response");

        return resultData;
    }

    //TODO:顺序打印
    public void log_print(Map<String,Object> req_resp_dict, String r_type){
        StringBuilder msg = new StringBuilder(String.format("\n================== %s details ==================\n",r_type));
        req_resp_dict.entrySet().stream().forEach(entry -> {
                msg.append(String.format("%-16s : %s \n", entry.getKey(), entry.getValue()));
            }
        );
        logger.debug(msg.toString());
    }

    public CloseableHttpResponse request(String method, String url, String name, Request request)
                throws Exception {
        this.init_meta_data();
        this.meta_data.setName(name);
        this.meta_data.getData().get(0).getRequest().put("method",url);
        this.meta_data.getData().get(0).getRequest().put("url",method);
        // TODO:
        //  kwargs.setdefault("timeout", 120)
        //  self.meta_data["data"][0]["request"].update(kwargs)
        //  data form header 都要保存到 meta_data中

        Long start_timestamp = System.currentTimeMillis();
        CloseableHttpResponse response = this._send_request_safe_mode(method, url, request);
        Long response_time_ms = System.currentTimeMillis() - start_timestamp;

        // get the length of the content, but if the argument stream is set to True, we take
        // the size from the content-length header, in order to not trigger fetching of the body
        //TODO:if kwargs.get("stream", False):
        // content_size = int(dict(response.headers).get("content-length") or 0)
        // else:

        Long content_size = response.getEntity().getContentLength() == -1 ?
                0 : response.getEntity().getContentLength();

        // record the consumed time
        this.meta_data.getStat().setResponse_time_ms(response_time_ms);
        //TODO:httpclient没有Elapsed参数
        // "elapsed_ms": response.elapsed.microseconds / 1000.0,
        this.meta_data.getStat().setElapsed_ms(response_time_ms);
        this.meta_data.getStat().setContent_size(content_size);

        // record request and response histories, include 30X redirection
        //TODO: response.history记录的是请求重定向的内容，暂时不支持重定向
        // response_list = response.history + [response]
        this.meta_data.getData().clear();
        this.meta_data.getData().add(
            get_req_resp_record(request,response)
        );

        //TODO:response.raise_for_status()
        logger.info(String.format("status_code: %s, response_time(ms): %s ms, response_length: %s bytes",response.getStatusLine().getStatusCode(),
                response_time_ms,
                content_size));


        return response;
        /*
        start_timestamp = time.time()
        response = self._send_request_safe_mode(method, url, **kwargs)
        response_time_ms = round((time.time() - start_timestamp) * 1000, 2)

        # get the length of the content, but if the argument stream is set to True, we take
        # the size from the content-length header, in order to not trigger fetching of the body
        if kwargs.get("stream", False):
        content_size = int(dict(response.headers).get("content-length") or 0)
        else:
        content_size = len(response.content or "")

        # record the consumed time
        self.meta_data["stat"] = {
                "response_time_ms": response_time_ms,
                "elapsed_ms": response.elapsed.microseconds / 1000.0,
                "content_size": content_size
        }

        # record request and response histories, include 30X redirection
        response_list = response.history + [response]
        self.meta_data["data"] = [
        self.get_req_resp_record(resp_obj)
        for resp_obj in response_list
        ]

        try:
        response.raise_for_status()
        except RequestException as e:
        logger.log_error(u"{exception}".format(exception=str(e)))
        else:
        logger.log_info(
                """status_code: {}, response_time(ms): {} ms, response_length: {} bytes\n""".format(
                        response.status_code,
                        response_time_ms,
                        content_size
                )
        )

        return response*/


    }

    public CloseableHttpResponse _send_request_safe_mode(String method, String url, Request request)
        throws Exception
    {
        CloseableHttpResponse response = null;
        if(method.equals("GET")) {
            //TODO:日志合并
            String msg = "processed request:\n";
            msg += String.format("> %s %s\n",method,url);
            msg += String.format("> kwargs: %s",JSON.toJSONString(request));
            logger.debug(msg);
            try {
                URIBuilder uriBuilder = new URIBuilder(url);
                addParameter(request.getParams(),uriBuilder);

                HttpGet httpget = new HttpGet(uriBuilder.build());
                addHeaders(request.getHeaders(), httpget);

                response = httpclient.execute(httpget);
                try {
                    HttpEntity entity = response.getEntity();
//                    logger.debug(EntityUtils.toString(response.getEntity()));
                } finally {
//                    response.close();
                }
            } finally {
                //TODO: 不要以为这里注释掉close就可以了，要在httpsession对象销毁时主动close呢！！！
//                httpclient.close();
            }
        }
        else if(method.equals("POST")) {
            String msg = "processed request:\n";
            msg += String.format("> %s %s\n",method,url);
            msg += String.format("> kwargs: %s",JSON.toJSONString(request));
            logger.debug(msg);
            try {
                HttpPost httppost = new HttpPost(url);
                addHeaders(request.getHeaders(), httppost);

                StringEntity entity = null;

                if(request.getFormData() != null){
                    entity = addFormData(request.getFormData());
                }else if(request.getJsonData() != null){
                    entity = addJsonData(request.getJsonData());
                }

                httppost.setEntity(entity);

                logger.debug("Executing post request " + httppost.getRequestLine());

                response = httpclient.execute(httppost);

            } finally {
                //TODO: 不要以为这里注释掉close就可以了，要在httpsession对象销毁时主动close呢！！！
//                httpclient.close();
            }
        }
        return response;
    }


    /* def _send_request_safe_mode(self, method, url, **kwargs):

            try:
    msg = "processed request:\n"
    msg += "> {method} {url}\n".format(method=method, url=url)
    msg += "> kwargs: {kwargs}".format(kwargs=kwargs)
            logger.log_debug(msg)
            return requests.Session.request(self, method, url, **kwargs)
    except (MissingSchema, InvalidSchema, InvalidURL):
    raise
    except RequestException as ex:
    resp = ApiResponse()
    resp.error = ex
    resp.status_code = 0  # with this status_code, content returns None
    resp.request = Request(method, url).prepare()
            return resp*/


    public void addParameter(Params params,URIBuilder uriBuilder){
        if(params == null || params.isEmpty())
            return;
        for(Map.Entry<String, LazyContent> entry : params.getParams().entrySet()){
            uriBuilder.addParameter(entry.getKey(),String.valueOf(entry.getValue().getEvalValue()));
        }
    }

    public void addHeaders(Headers headers, HttpRequestBase httpRequestBase){
        if(headers == null || headers.isEmpty())
            return;
        for(Map.Entry<String, LazyContent> entry : headers.getHeaders().entrySet()){
            httpRequestBase.addHeader(entry.getKey(),String.valueOf(entry.getValue().getEvalValue()));
        }
    }

    public UrlEncodedFormEntity addFormData(FormData formData){
        if(formData == null || formData.isEmpty())
            return null;

        List<NameValuePair> form = new ArrayList<>();
        for(Map.Entry<String, LazyContent> entry : formData.getFormData().entrySet()){
            form.add(new BasicNameValuePair(entry.getKey(),String.valueOf(Optional.ofNullable(entry.getValue().getEvalValue()).orElse(""))));
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);

        return entity;
    }

    public StringEntity addJsonData(JsonData jsonData){
        if(jsonData == null || jsonData.isEmpty())
            return null;

        JSONObject jsonObj = new JSONObject();
        for(Map.Entry<String, LazyContent> entry : jsonData.getJsonData().entrySet()){
            jsonObj.put(entry.getKey(),entry.getValue().getEvalValue());
        }

        StringEntity stringEntity = new StringEntity(jsonObj.toString(),"UTF-8");
        stringEntity.setContentEncoding("UTF-8");
        stringEntity.setContentType("application/json");

        return stringEntity;
    }
}
