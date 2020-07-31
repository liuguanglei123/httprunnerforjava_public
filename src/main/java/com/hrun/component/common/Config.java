package com.hrun.component.common;

import com.alibaba.fastjson.annotation.JSONField;
import com.hrun.Utils;
import com.hrun.component.ProjectMapping;
import com.hrun.component.intf.Parseable;
import com.hrun.lazyContent.LazyString;
import lombok.Data;

import java.io.Serializable;
import java.util.*;

@Data
public class Config implements Serializable, Parseable {

    @JSONField(ordinal=1)
    private LazyString name;

    @JSONField(ordinal=2)
    private String id;

    @JSONField(ordinal=3)
    private LazyString base_url;

    @JSONField(ordinal=4)
    private Variables variables = new Variables();

//TODO:    private Export export;

    @JSONField(ordinal=5)
    private Boolean verify;

    @JSONField(ordinal=6)
    private String testCase_path;

    @JSONField(ordinal=7)
    private SetupHooks setup_hooks;

    @JSONField(ordinal=8)
    private TeardownHooks teardown_hooks;

    @JSONField(ordinal=10)
    private Extract extract;

    @JSONField(ordinal=12)
    private Export export;

    public Config(){

    }

    public Config(Map raw_config){
        // TODO:再次检查config中是否需要加载别的参数
        if(Optional.ofNullable(raw_config.get("name")).isPresent()){
            this.name = new LazyString(String.valueOf(raw_config.get("name")));
        }else{
            this.name = new LazyString("default config name");
        }
        // REMARK: 暂时没有用到 id
        Optional.ofNullable(raw_config.get("id")).map(h -> this.id = (String) raw_config.get("id"));
        Optional.ofNullable(raw_config.get("base_url")).map(h -> new LazyString((String)h));
        Optional.ofNullable(raw_config.get("verify")).map(h -> this.verify = (Boolean) raw_config.get("verify"));
        Optional.ofNullable(raw_config.get("variables")).map(h -> this.variables = new Variables((Map) h));
        Optional.ofNullable(raw_config.get("export")).map(o -> this.export = new Export((List) o));
    }

    /**
     * 对所有内部成员变量进行lazyString的标记和处理
     * @param check_variables_set
     */
    @Override
    public void parse(Set check_variables_set){
        // output 不需要进行解析
        Optional.ofNullable(this.name).ifPresent( n -> n.parse(check_variables_set));
        Optional.ofNullable(this.base_url).ifPresent( b -> b.parse(check_variables_set));
        Optional.ofNullable(this.variables).ifPresent( v -> v.parse(check_variables_set));
        Optional.ofNullable(this.setup_hooks).ifPresent( h -> h.parse(check_variables_set));
        Optional.ofNullable(this.teardown_hooks).ifPresent( t -> t.parse(check_variables_set));
    }

    @Override
    public Parseable to_value(Variables variables_mapping) {
        return null;
    }

    /**
     * 解析config部分的入口一，一般在parse环节调用，此时没有会话上下文变量
     * @param project_mapping
     * @return
     */
    public Config parse_config(ProjectMapping project_mapping){
        return parse_config(project_mapping,null);
    }

    /**
     * 解析config部分的入口二，一般在执行环节直接调用，此时存在会话上下文
     * @param project_mapping
     * @session_variables_set 会话中的变量集合
     * @return
     */
    public Config parse_config(ProjectMapping project_mapping,Set<String> session_variables_set){
        Config prepared_config = Utils.deepcopy_dict(this);
        if(prepared_config.getVariables() == null)
            prepared_config.setVariables(new Variables());

        // TODO: "variables": "${gen_variables()}"
        Variables override_variables = Utils.deepcopy_dict(project_mapping.getVariables());
        prepared_config.getVariables().extend(override_variables);

        // check_variables_set 是什么作用？
        // 首先将所有的变量key值收集起来，如果其他节点比如name，extract，hook等引用了这些变量，可以进行替换或者标记
        Set<String> check_variables_set = new HashSet(prepared_config.getVariables().getVariables().keySet());
        Optional.ofNullable(session_variables_set).ifPresent(
            s -> check_variables_set.addAll(s)
        );

        prepared_config.parse(check_variables_set);
        return prepared_config;
    }
}
