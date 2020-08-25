package com.hrun.component.api;

import com.alibaba.fastjson.annotation.JSONField;
import com.hrun.App;
import com.hrun.Loader;
import com.hrun.component.common.*;
import com.hrun.component.intf.Parseable;
import com.hrun.component.intf.RunableComponent;
import com.hrun.exceptions.HrunExceptionFactory;
import com.hrun.lazyContent.LazyString;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Data
public class Api implements RunableComponent, Serializable {

    private static Logger logger = LoggerFactory.getLogger(Api.class);

    @JSONField(ordinal=4)
    private Request request;

    @JSONField(ordinal=1)
    private LazyString name;

    @JSONField(ordinal=3)
    private Variables variables;

    @JSONField(ordinal=5)
    private Validate validate;

    @JSONField(ordinal=7)
    private SetupHooks setup_hooks;

    @JSONField(ordinal=8)
    private TeardownHooks teardown_hooks;

    private LazyString base_url;

    private Boolean verify;

    @JSONField(ordinal=2)
    private LazyString api; // 实际上是api_path

    @JSONField(ordinal=11)
    private Api api_def;

    @JSONField(ordinal=10)
    private Extract extract;

    private Config config;

    private LazyString skip; // no use

    public Api(String file_name) {

    }

    /**
     * 构造函数一： 创建Api对象
     * @param raw_api api的具体内容
     * @param isApiDefineFile 是否是加载api文件，true说明加载的是api文件内容，false说明加载的是testcase中嵌套的api节点
     *                        区分的原因是api节点和文件内容需要校验的内容不同
     */
    public Api(Map raw_api, Boolean isApiDefineFile) {
        if (Optional.ofNullable(raw_api.get("name")).isPresent()) {
            this.name = new LazyString((String) raw_api.get("name"));
        }

        Optional.ofNullable(raw_api.get("variables")).map(
            h -> this.variables = new Variables((Map)h)
        );

        Optional.ofNullable(raw_api.get("skip")).map(
                h -> this.skip = new LazyString((String)h)
        );


        Optional.ofNullable(raw_api.get("setup_hooks")).map(
            s -> this.setup_hooks = new SetupHooks((List) s)
        );

        Optional.ofNullable(raw_api.get("teardown_hooks")).map(
            t -> this.teardown_hooks = new TeardownHooks((List) t)
        );

        Optional.ofNullable(raw_api.get("validate")).map(
                v -> this.validate = new Validate((List)raw_api.get("validate"))
        );

        Optional.ofNullable(raw_api.get("extract")).map(
                h -> this.extract = new Extract((List)raw_api.get("extract"))
        );

        if(!isApiDefineFile){
            this.api_def = new Api((Map) Loader.load_file(Loader.trans2AbsolutePath((String)raw_api.get("api"))), true);
        }else{
            try {
                Optional.of(raw_api.get("request")).map(
                        h -> this.request = new Request((Map) h)
                );
            } catch (NullPointerException e) {
                logger.error("api定义中缺少request，这部分是必须的");
                HrunExceptionFactory.create("E0015");
            }
        }
    }

    @Override
    public void parse(Set check_variables_set){
        Optional.ofNullable(this.name).ifPresent( name -> name.parse(check_variables_set));
        Optional.ofNullable(this.api).ifPresent( api -> api.parse(check_variables_set));
        Optional.ofNullable(this.variables).ifPresent( variables -> variables.parse(check_variables_set));
        Optional.ofNullable(this.request).ifPresent( request -> request.parse(check_variables_set));
        Optional.ofNullable(this.validate).ifPresent( validate -> validate.parse(check_variables_set));
        Optional.ofNullable(this.setup_hooks).ifPresent( setup_hooks -> setup_hooks.parse(check_variables_set));
        Optional.ofNullable(this.teardown_hooks).ifPresent( teardown_hooks -> teardown_hooks.parse(check_variables_set));
        Optional.ofNullable(this.base_url).ifPresent( base_url -> base_url.parse(check_variables_set));
        Optional.ofNullable(this.extract).ifPresent( extract -> extract.parse(check_variables_set));
        Optional.ofNullable(this.config).ifPresent( config -> config.parse(check_variables_set));
        Optional.ofNullable(this.skip).ifPresent( skip -> skip.parse(check_variables_set));
    }

    @Override
    public Parseable to_value(Variables variables_mapping) {
        return null;
    }

    @Override
    public Variables getVariables(){
        return this.variables;
    }

    public void _extend_with_api(Api api_def_dict){
        // override api name
        if(this.getName() == null){
            setName(Optional.ofNullable(api_def_dict.getName()).orElse(new LazyString("api name undefined")));
        }
        // override variables
        Variables def_variables = api_def_dict.getVariables();
        api_def_dict.setVariables(null);
        setVariables(Variables.extend2Variables(def_variables,getVariables()));

        // TODO: merge & override validators TODO: relocate
        Validate def_raw_validators = api_def_dict.getValidate();
        api_def_dict.setValidate(null);
        Validate ref_validators = this.getValidate();
        this.setValidate(Validate.extend2Validate(def_raw_validators,ref_validators));

        // TODO: merge & override extractors
        Extract def_extrators = api_def_dict.getExtract();
        api_def_dict.setExtract(null);
        this.setExtract(Extract.extend2Extract(def_extrators,getExtract()));

        // TODO: merge & override request
        this.setRequest(api_def_dict.getRequest());
        api_def_dict.setRequest(null);

        // base_url & verify: priority api_def_dict > test_dict
        Optional.ofNullable(api_def_dict.getBase_url()).ifPresent(base_url -> this.setBase_url(base_url));
        Optional.ofNullable(api_def_dict.getVerify()).ifPresent(varify -> this.setVerify(varify));

        // TODO: merge & override setup_hooks
        if(this.getSetup_hooks() == null)
            setSetup_hooks(api_def_dict.getSetup_hooks());
        else{
            this.getSetup_hooks().extend(api_def_dict.getSetup_hooks());
        }
        // TODO: merge & override teardown_hooks
        if(this.getTeardown_hooks() == null)
            setTeardown_hooks(api_def_dict.getTeardown_hooks());
        else{
            this.getTeardown_hooks().extend(api_def_dict.getTeardown_hooks());
        }

        // TODO: extend with other api definition items, e.g. times,skip
        Optional.ofNullable(api_def_dict.getSkip()).ifPresent(skip -> this.setSkip(skip));
    }

    @Override
    public LazyString getName(){
        return this.name;
    }

}














