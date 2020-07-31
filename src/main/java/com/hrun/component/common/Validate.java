package com.hrun.component.common;

import java.io.Serializable;
import java.util.*;

import com.hrun.Utils;
import com.hrun.component.intf.Parseable;
import com.hrun.exceptions.HrunExceptionFactory;
import com.hrun.lazyContent.LazyContent;
import com.hrun.lazyContent.LazyFunction;
import com.hrun.lazyContent.LazyString;
import lombok.Data;

@Data
public class Validate implements Serializable, Parseable {

    private List<Map<String, List<Object>>> raw_validate = new ArrayList<>();

    private List<LazyFunction> prepared_validators = new ArrayList<LazyFunction>();

    public Validate(){
    }

    // 加载原始validate内容，不做LazyContent转换，一直到parse_test部分，才进行LazyFunction的转换
    // 为什么这样做？
    // parse_test部分会将原始内容 raw_validate 转成function的列表保存，需要做一些lazystring的转换操作等，如果在加载validate
    // 的时候就进行lazyfuncion和lazystring的转换，会发现参数是不全面的
    public Validate(List raw_validate) {
        for (int i = 0; i < raw_validate.size(); i++) {
            if (raw_validate.get(i) instanceof Map) {
                if (((Map) raw_validate.get(i)).size() == 1) {
                    for (Map.Entry<String, Object> entry : (((Map<String, Object>) raw_validate.get(i)).entrySet())) {
                        List<Object> valid_content = new ArrayList<Object>();
                        ((List) entry.getValue()).forEach(each -> {
                            valid_content.add(each);
                        });
                        Map<String, List<Object>> valid_map = new HashMap<String, List<Object>>();
                        valid_map.put(entry.getKey(), valid_content);
                        this.raw_validate.add(valid_map);
                    }
                } else {
                    HrunExceptionFactory.create("E0034");
                }
            } else {
                HrunExceptionFactory.create("E0034");
            }
        }
    }

    @Override
    public void parse(Set check_variables_set) {
        if (this.raw_validate == null || this.raw_validate.size() == 0)
            return;

        for(Map<String, List<Object>> each : raw_validate){
            Map<String,Object> function_meta = new HashMap<String,Object>();
            for(Map.Entry<String,List<Object>> inner_each : each.entrySet()){
                function_meta.put("func_name",inner_each.getKey());
                function_meta.put("argsList",inner_each.getValue());
            }
            prepared_validators.add(new LazyFunction(function_meta,check_variables_set));
        }
    }

    @Override
    public Parseable to_value(Variables variables_mapping) {
        if (this.raw_validate == null || this.raw_validate.size() == 0)
            return this;

        return this;
    }

    public Boolean isEmpty() {
        if (this.raw_validate == null || this.raw_validate.size() == 0)
            return true;
        return false;
    }

    public static Validate extend2Validate(Validate raw_validators,Validate override_validators){
        if(override_validators == null || override_validators.isEmpty())
            return raw_validators;

        if(raw_validators == null || raw_validators.isEmpty())
            return override_validators;

        Set<Object> checkItem = new HashSet<Object>();
        Validate def_validators_mapping = Utils.deepcopy_dict(raw_validators);
        Validate ref_validators_mapping = Utils.deepcopy_dict(override_validators);
        for(Map<String, List<Object>> each : def_validators_mapping.getRaw_validate()){
            for(String inner_each : each.keySet()){
                checkItem.add(each.get(inner_each).get(0));
            }
        }

        for(Map<String, List<Object>> each : ref_validators_mapping.getRaw_validate()){
            for(String inner_each : each.keySet()){
                if(checkItem.contains(each.get(inner_each).get(0)))
                    continue;
                else
                    def_validators_mapping.getRaw_validate().add(each);
            }
        }

        return def_validators_mapping;
    }
}
