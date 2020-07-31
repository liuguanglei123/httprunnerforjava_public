package com.hrun.component.common;

import com.alibaba.fastjson.JSON;
import com.hrun.component.intf.Parseable;
import com.hrun.lazyContent.LazyContent;
import com.hrun.lazyContent.LazyString;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class JsonData implements Serializable, Parseable {
    private HashMap<String, LazyContent> jsonData = new HashMap<String,LazyContent>();

    public JsonData(Map raw_json) {
        for (Map.Entry<String, Object> entry : ((HashMap<String, Object>) raw_json).entrySet()) {
            if (entry.getValue() instanceof String )
                jsonData.put(entry.getKey(), new LazyString(String.valueOf(entry.getValue())));
            else
                jsonData.put(entry.getKey(), new LazyContent(entry.getValue()));
        }
    }

    @Override
    public void parse(Set check_variables_set) {
        if(this.jsonData == null || this.jsonData.size() == 0)
            return;

        for(LazyContent value : jsonData.values()){
            if(value instanceof LazyString)
                ((LazyString)value).parse(check_variables_set);
        }
    }

    @Override
    public JsonData to_value(Variables variables_mapping) {
        if(this.jsonData == null || this.jsonData.size() == 0)
            return this;

        for(LazyContent value : jsonData.values()){
            if(value instanceof LazyString)
                ((LazyString)value).to_value(variables_mapping);
        }
        return this;
    }

    public String toJson(){
        HashMap<String,Object> tmpJsonData = new HashMap<String,Object>();
        for(Map.Entry<String,LazyContent> entry : this.jsonData.entrySet()){
            if(entry.getValue() instanceof LazyString){
                tmpJsonData.put(entry.getKey(),((LazyString) entry.getValue()).getEvalString());
            }else{
                tmpJsonData.put(entry.getKey(),entry.getValue());
            }
        }
        String result = JSON.toJSONString(tmpJsonData);
        return result;
    }

    public Boolean isEmpty(){
        return (jsonData == null || jsonData.size() == 0);
    }

    public Map<String,String> toMap(){
        Map<String,String> jsonDataMap = this.jsonData.entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getKey(), entry -> String.valueOf(Optional.ofNullable(entry.getValue().getEvalValue()).orElse(""))
                )
        );

        return jsonDataMap;
    }

}
