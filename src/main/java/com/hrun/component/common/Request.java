package com.hrun.component.common;

import com.alibaba.fastjson.annotation.JSONField;
import com.hrun.component.intf.Parseable;
import com.hrun.exceptions.HrunExceptionFactory;
import com.hrun.lazyContent.LazyContent;
import com.hrun.lazyContent.LazyString;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.Data;

@Data
public class Request implements Serializable, Parseable {

    @JSONField(ordinal=1)
    private LazyString url;

    @JSONField(ordinal=2)
    private LazyString method;

    @JSONField(ordinal=4)
    private Headers headers;

    //post使用的json数据
    @JSONField(ordinal=5)
    private JsonData jsonData;

    //post使用的表单数据
    @JSONField(ordinal=6)
    private FormData formData;

    //get请求使用的param
    @JSONField(ordinal=3)
    private Params params;

    @JSONField(ordinal=7)
    private Boolean verify;

    public Request(){

    }

    public Request(Map raw_request){
        this.url = new LazyString((String)raw_request.getOrDefault("url",null));
        this.method = new LazyString((String)raw_request.getOrDefault("method",null));

        if(this.url == null || this.method == null)
            HrunExceptionFactory.create("E0015");

        Optional.ofNullable(raw_request.get("headers")).map(h -> this.headers = new Headers((Map)h));
        Optional.ofNullable(raw_request.get("json")).map(h -> this.jsonData = new JsonData((Map)h));
        Optional.ofNullable(raw_request.get("form")).map(h -> this.formData = new FormData((Map)h));
        Optional.ofNullable(raw_request.get("params")).map(h -> this.params = new Params((Map)h));
    }

    @Override
    public void parse(Set check_variables_set){
        Optional.ofNullable(this.url).ifPresent( u -> u.parse(check_variables_set));
        Optional.ofNullable(this.method).ifPresent( m -> m.parse(check_variables_set));
        Optional.ofNullable(this.headers).ifPresent( h -> h.parse(check_variables_set));
        Optional.ofNullable(this.jsonData).ifPresent( j -> j.parse(check_variables_set));
        Optional.ofNullable(this.formData).ifPresent( f -> f.parse(check_variables_set));
        Optional.ofNullable(this.params).ifPresent( p -> p.parse(check_variables_set));
    }

    @Override
    public Request to_value(Variables variables_mapping) {
        Optional.ofNullable(this.url).ifPresent( u -> u.to_value(variables_mapping));
        Optional.ofNullable(this.method).ifPresent( m -> m.to_value(variables_mapping));
        Optional.ofNullable(this.headers).ifPresent( h -> h.to_value(variables_mapping));
        Optional.ofNullable(this.jsonData).ifPresent( j -> j.to_value(variables_mapping));
        Optional.ofNullable(this.formData).ifPresent( f -> f.to_value(variables_mapping));
        Optional.ofNullable(this.params).ifPresent( p -> p.to_value(variables_mapping));
        return this;
    }

}
