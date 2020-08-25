package com.hrun.component;

import com.alibaba.fastjson.annotation.JSONField;
import com.hrun.component.common.Variables;
import lombok.Data;

import java.util.Map;

@Data
public class ProjectMapping {

    // 环境变量内容
    @JSONField(ordinal=1)
    private Map<String, Object> env;

    // 当前执行的路径，一般是项目的根路径
    @JSONField(ordinal=2)
    private String PWD;

    @JSONField(ordinal=3)
    public static Class functions;

    @JSONField(ordinal=3)
    private String test_path;

    @JSONField(ordinal=4)
    private Variables variables;

    public void setVariables(Map variables){
        this.variables.setVariables(variables);
    }

    public void setFunctions(Class cls){
        functions = cls;
    }
}
