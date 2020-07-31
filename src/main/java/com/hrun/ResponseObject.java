package com.hrun;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hrun.component.common.Extract;
import com.hrun.exceptions.HrunExceptionFactory;
import lombok.Data;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Data
public class ResponseObject {
    static Logger logger = LoggerFactory.getLogger(App.class);

    private CloseableHttpResponse resp_obj;

    private String body;

    public ResponseObject(CloseableHttpResponse resp_obj){
        this.resp_obj = resp_obj;
    }

    public Object extract_field(String field,String respBody){
        //TODO: 原文中有这样一段逻辑，可能需要增加
        /* if not isinstance(field, basestring):
            err_msg = u"Invalid extractor! => {}\n".format(field)
            logger.log_error(err_msg)
            raise exceptions.ParamsError(err_msg)*/

        String msg = String.format("extract: %s",field);

        //TODO:
        /* if field.startswith("$"):
            value = self._extract_field_with_jsonpath(field)
        elif text_extractor_regexp_compile.match(field):
            value = self._extract_field_with_regex(field)*/
        Object value = this._extract_field_with_delimiter(field,respBody);

        msg += String.format("\t=> %s",value);
        logger.debug(msg);

        return value;
    }

    public Object _extract_field_with_delimiter(String field,String respBody){
        String[] query = field.split("\\.",2);
        String top_query="";
        String sub_query="";
        if(query.length == 1)
            top_query = field;
        else{
            top_query = query[0];
            sub_query = query[1];
        }

        if(Arrays.asList("status_code", "encoding", "ok", "reason", "url").contains(top_query)){
            if(!sub_query.equals("")){
                String err_msg = String.format("Failed to extract: %s",field);
                logger.error(err_msg);
                HrunExceptionFactory.create("E0026");
            }
            //TODO：原文支持了encoding，ok，reason，url等多个参数的获取，这里只支持一个status_code
            return getAttr(top_query);
        }
        //TODO: cookies
        //TODO: elapsed 好像可以获取响应时间等内容
        //TODO: headers
        else if(Arrays.asList("content", "text", "json").contains(top_query)){
            this.body = respBody;
            /*try{

                HttpEntity entity = resp_obj.getEntity();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        entity.getContent(), "UTF-8"));
                String line;
                try{
                    while ((line = br.readLine()) != null) {
                        body += line;
                    }
                    this.body = body;
                }catch(IOException e){
                    body = this.body;
                }

                br.close();
            }catch(Exception e){
                e.printStackTrace();
            }*/

            if(sub_query.equals(""))
                return respBody;
            respBody.replace("\\\\u","\\u");
            JSONObject jsonBody = JSON.parseObject(respBody);
            return Utils.query_json(jsonBody, sub_query);
            //TODO: if isinstance(body, (dict, list)):
            // 还有一点错误输出也要完善！

        }
        //TODO:else
        //  # new set response attributes in teardown_hooks
        //        elif top_query in self.__dict__:
        else{
            String err_msg = String.format("Failed to extract attribute from response! => %s \n",field);
            err_msg += String.format("available response attributes: status_code, cookies, elapsed, headers, content, text, json, encoding, ok, reason, url.\n");
            err_msg += String.format("If you want to set attribute in teardown_hooks, take the following example as reference:\n");
            err_msg += String.format("response.new_attribute = 'new_attribute_value'\n");
            logger.error(err_msg);
            HrunExceptionFactory.create("E0027");
        }
        return null;
    }

    public Object getAttr(String key){
        if(key.equals("status_code"))
            return this.resp_obj.getStatusLine().getStatusCode();
        return null;
    }

    public Map<String,Object> extract_response(Extract extractors,String respBody){
        if(extractors == null || extractors.isEmpty())
            return new HashMap<String,Object>();

        logger.debug("start to extract from response object.");
        Map<String,Object> extracted_variables_mapping = new HashMap<>();
        for(Map.Entry<String,String> entry : extractors.getExtract().entrySet()){
            extracted_variables_mapping.put(entry.getKey(),extract_field(entry.getValue(),respBody));
        }

        return extracted_variables_mapping;
    }
}
