package com.hrun.component.common;

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
public class Headers implements Serializable, Parseable {
    private HashMap<String, LazyContent> headers = new HashMap<String,LazyContent>();

    public Headers(Map raw_headers) {
        for (Map.Entry<String, Object> entry : ((HashMap<String, Object>) raw_headers).entrySet()) {
            if (entry.getValue() instanceof String )
                headers.put(entry.getKey(), new LazyString(String.valueOf(entry.getValue())));
            else
                headers.put(entry.getKey(), new LazyContent(entry.getValue()));
        }
    }

    @Override
    public void parse(Set check_variables_set) {
        if(this.headers == null || this.headers.size() == 0)
            return;

        for(LazyContent value : headers.values()){
            if(value instanceof LazyString)
                ((LazyString)value).parse(check_variables_set);
        }
    }

    @Override
    public Headers to_value(Variables variables_mapping) {
        if(this.headers == null || this.headers.size() == 0)
            return this;

        for(LazyContent value : headers.values()){
            if(value instanceof LazyString)
                ((LazyString)value).to_value(variables_mapping);
        }
        return this;
    }

    public Boolean isEmpty(){
        return (headers == null || headers.size() == 0);
    }

    public Map<String,String> toMap(){
        Map<String,String> headerMap = this.headers.entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getKey(), entry -> String.valueOf(Optional.ofNullable(entry.getValue().getEvalValue()).orElse(""))
                )
        );

        return headerMap;
    }
}
