package com.hrun.component.common;

import java.io.Serializable;
import java.util.*;

import com.alibaba.fastjson.annotation.JSONField;
import com.hrun.Utils;
import com.hrun.component.intf.Parseable;
import com.hrun.exceptions.HrunExceptionFactory;
import lombok.Data;

@Data
public class Extract implements Serializable, Parseable {

    private Map<String,String> extract = new HashMap<>();

    @JSONField(serialize=false)
    private List<String> extract2Output = new ArrayList<>();

    public Extract() {
    }

    /**
     * 构造函数一： 创建Extract对象，不支持LazyString形式
     * @param raw_extract extract的具体内容
     *                  extract:
     *                      - session_token: content.token
     *              false说明加载的是testcase节点，此时raw_extract的内容是
     *                  extract:
     *                      - session_token
     */
    public Extract(List raw_extract) {
        for (int i = 0; i < raw_extract.size(); i++) {
            if (raw_extract.get(i) instanceof Map) {
                for (Map.Entry<String, String> entry : (((Map<String, String>) raw_extract.get(i)).entrySet())) {
                    extract.put(entry.getKey(), entry.getValue());
                    break;
                }
            }else if (raw_extract.get(i) instanceof String) {
                extract2Output.add((String)raw_extract.get(i));
            }else {
                HrunExceptionFactory.create("E0037");
            }

        }
    }

    @Override
    public void parse(Set check_variables_set) {
        return;
    }

    @Override
    public Parseable to_value(Variables variables_mapping) {
        return null;
    }

    public static Extract extend2Extract(Extract ext1,Extract ext2){
        Extract override_extract_mapping = Utils.deepcopy_dict(Optional.ofNullable(ext1).orElse(new Extract()));
        Optional.ofNullable(ext2).ifPresent( e ->
                override_extract_mapping.getExtract().putAll(e.getExtract())
        );

        return override_extract_mapping;
    }

    @JSONField(serialize=false)
    public Boolean isEmpty(){
        return (extract == null || extract.size() == 0);
    }
}
