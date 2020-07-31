package com.hrun.component.Meta_data;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class ResultData implements Serializable {
    private Map<String,Object> request = new HashMap<String,Object>();

    private Map<String,Object> response = new HashMap<String,Object>();

    public ResultData(){
    }

    public void stringify(){
        __stringify_request();
        __stringify_response();
    }

    /**
     * 将request部分字符串化
     */
    public void __stringify_request(){

    }

    /**
     * 将response部分字符串化
     */
    public void __stringify_response(){

    }
}


