package com.hrun.component.testsuite;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Strings;
import com.hrun.Loader;
import com.hrun.Parse;
import com.hrun.Utils;
import com.hrun.component.ProjectMapping;
import com.hrun.component.common.*;
import com.hrun.component.intf.Parseable;
import com.hrun.component.intf.RunableComponent;
import com.hrun.component.testcase.TestCase;
import com.hrun.exceptions.HrunExceptionFactory;
import com.hrun.lazyContent.LazyString;
import lombok.Data;

import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.*;

//该类仅用来加载testsuite文件或者文本内容，测试用例集合的执行时TestSuiteRun类
@Data
public class TestSuite implements RunableComponent,Serializable, Parseable {

    @JSONField(ordinal=1)
    private Config config;

    /**
     * 用来存储加载后的测试case，key值为测试用例名称，value为测试用例body
     */
    @JSONField(ordinal=2)
    private Map<String, TestCase> testCasesMap = new LinkedHashMap<String,TestCase>();

    @JSONField(ordinal=4)
    private Map<String, TestCase> parsed_testCasesMap = new HashMap<String,TestCase>();

    // 在add_test步骤中开始起作用，用来保存整理后的testcase列表
    private List<TestCase> testCases = new ArrayList<TestCase>();

    @JSONField(ordinal=3)
    private String path;

    public TestSuite(){

    }
    /**
     * 构造函数一：用于直接加载文件名，已废弃
     * @param file_path 文件的相对路径
     */
    @Deprecated
    public TestSuite(String file_path){
        Map fileContent = (Map) Loader.load_file(Loader.trans2AbsolutePath(file_path));

        if(fileContent.containsKey("config"))
            this.config = new Config((Map)fileContent.get("config"));

        if(fileContent.containsKey("testcases")) {
            if(fileContent.get("testcases") instanceof Map){
                for(Map.Entry<String,Map> entry : ((Map<String,Map>)fileContent.get("testcases")).entrySet()){
                    TestCase testCase = new TestCase(entry.getKey(),entry.getValue());
                    this.testCasesMap.put(entry.getKey(),testCase);
                }
            }else if(fileContent.get("testcases") instanceof List){
                //TODO:
            }
        }else{
            HrunExceptionFactory.create("E0010");
        }
    }

    /**
     * 构造函数二，用于直接加载Map，这里代替的是hrun中的load_testsuite方法，根据其类型Map/List执行不同的加载策略
     * @param raw_testsuite testsuite文件的原始内容
     */
    public TestSuite(Map raw_testsuite){
        //TODO: 格式校验 JsonSchemaChecker.validate_testsuite_v1_format(raw_testsuite)
        if(raw_testsuite.containsKey("config"))
            this.config = new Config((Map)raw_testsuite.get("config"));

        if(raw_testsuite.containsKey("testcases")) {
            if(raw_testsuite.get("testcases") instanceof Map){
                for(Map.Entry<String,Map> entry :
                        ((Map<String,Map>)raw_testsuite.get("testcases")).entrySet()){
                    //TODO：  工厂模式？？
                    TestCase testCase = new TestCase(entry.getKey(),entry.getValue());
                    this.testCasesMap.put(entry.getKey(),testCase);
                }
            }else if(raw_testsuite.get("testcases") instanceof List){
                //TODO: low_priority
            }
        }else{
            HrunExceptionFactory.create("E0010");
        }
    }

    public void setPath(String path){
        this.path = path;
    }

    @Override
    public void parse(Set check_variables_set){
        Optional.ofNullable(config).ifPresent( c -> c.parse(check_variables_set));

        for(Map.Entry<String, TestCase> entry : testCasesMap.entrySet()){
            entry.getValue().parse(check_variables_set);
        }
    }

    @Override
    public Parseable to_value(Variables variables_mapping) {
        return null;
    }

    public List<TestCase> _parse_testsuite(ProjectMapping project_mapping){
        if(this.config == null)
            setConfig(new Config());

        Config prepared_config = this.config.parse_config(project_mapping);

        List<TestCase> parsed_testcase_list = __get_parsed_testsuite_testcases(
                prepared_config,
                project_mapping
        );
        return parsed_testcase_list;
    }

    /**
     *使用config中的变量 base_url verfig覆盖testSuite中的内容：
     * variables 优先级:
     *      参数化 parameters > testsuite config > testcase config > testcase_def config > testcase_def tests > api
     * base_url 优先级:
     *      testcase_def tests > testcase_def config > testcase config > testsuite config
     * @param testsuite_config testsuite节点中的config
     * @param project_mapping
     * @return
     */
    public List<TestCase> __get_parsed_testsuite_testcases(Config testsuite_config,ProjectMapping project_mapping){

        LazyString testsuite_base_url = testsuite_config.getBase_url();
        Variables testsuite_config_variables = testsuite_config.getVariables();
        List<TestCase> parsed_testcase_list = new ArrayList<TestCase>();

        for(Map.Entry<String,TestCase> entry : this.testCasesMap.entrySet()){
            Optional.of(entry.getValue().getTestCase_def());
            TestCase parsed_testcase = entry.getValue().getTestCase_def();

            if(parsed_testcase.getConfig() == null)
                parsed_testcase.setConfig(new Config());

            parsed_testcase.setTestCase_path(entry.getValue().getTestCase_path());
            parsed_testcase.getConfig().setName(new LazyString(entry.getKey()));

            // base_url priority: testcase config > testsuite config
            if(parsed_testcase.getBase_url() == null)
                parsed_testcase.getConfig().setBase_url(testsuite_base_url);

            // 1, testsuite config => testcase config
            // testsuite优先级高
            // override test_dict variables
            Variables testcase_config_variables = Variables.extend2Variables(
                    entry.getValue().getVariables(),
                    testsuite_config_variables
            );
            entry.getValue().setVariables(null);

            // 2, testcase config > testcase_def config
            // testsuite文件中的testcase节点的变量优先级比testcase文件中的config部分高
            // override testcase_def config variables
            Variables overrided_testcase_config_variables = Variables.extend2Variables(
                    parsed_testcase.getConfig().getVariables(),
                    testcase_config_variables
            );
            parsed_testcase.getConfig().setVariables(null);

            if(!Variables.isNullOrEmpty(overrided_testcase_config_variables))
                parsed_testcase.getConfig().setVariables(overrided_testcase_config_variables);

            // parse config variables
            // TODO:感觉这里的Parse.parse_variables_mapping啥也没做，应该可以忽略的
            // Variables parsed_config_variables = Parse.parse_variables_mapping(overrided_testcase_config_variables)

            if(Parameters.isNullOrEmpty(entry.getValue().getParameters())){
                parsed_testcase = parsed_testcase._parse_testcase(project_mapping);
                parsed_testcase_list.add(parsed_testcase);
            }else {
                List<Map<String,Object>> cartesian_product_parameters = entry.getValue().getParameters().parse(
                        overrided_testcase_config_variables
                );
                for(Map<String,Object> parameter_variables : cartesian_product_parameters){
                    TestCase testcase_copied = Utils.deepcopy_dict(parsed_testcase);
                    Variables parsed_config_variables_copied = Utils.deepcopy_dict(overrided_testcase_config_variables);
                    testcase_copied.getConfig().setVariables(Variables.extend2Variables(parsed_config_variables_copied,
                            new Variables(parameter_variables)));

                    TestCase parsed_testcase_copied = testcase_copied._parse_testcase(project_mapping);

                    parsed_testcase_copied.getConfig().setName(parsed_testcase_copied.getConfig().getName());

                    parsed_testcase_list.add(parsed_testcase_copied);
                }
            }
        }

        return parsed_testcase_list;
    }

    @Override
    public Variables getVariables() {
        return null;
    }

    @Override
    public void setVariables(Variables variables) {

    }

    @Override
    public LazyString getBase_url() {
        return null;
    }

    @Override
    public void setBase_url(LazyString base_url) {

    }

    @Override
    public Validate getValidate() {
        return null;
    }

    @Override
    public void setValidate(Validate validate) {

    }

    public void addTest(TestCase testCase){
        this.testCases.add(testCase);
    }

    @Override
    public Extract getExtract(){
        return null;
    }

    @Override
    public LazyString getName() {
        return null;
    }
}
