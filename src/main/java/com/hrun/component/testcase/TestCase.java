package com.hrun.component.testcase;

import com.alibaba.fastjson.annotation.JSONField;
import com.hrun.Loader;
import com.hrun.Runner;
import com.hrun.Utils;
import com.hrun.component.ProjectMapping;
import com.hrun.component.RunTestCls;
import com.hrun.component.api.Api;
import com.hrun.component.common.*;
import com.hrun.component.intf.Parseable;
import com.hrun.component.intf.RunableComponent;
import com.hrun.exceptions.HrunExceptionFactory;
import com.hrun.lazyContent.LazyString;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

//该类仅用来加载testcase文件或者文本内容，测试用例集合的执行时TestCaseRun类
@Data
public class TestCase implements RunableComponent, Serializable {

    private static Logger logger = LoggerFactory.getLogger(TestCase.class);

    @JSONField(ordinal=1)
    private Config config;

    /**
     * testcase中可执行步骤，testcase的步骤中嵌套的可能是api，也可能是另一个testcase
     */
    @JSONField(ordinal=7)
    private List<RunableComponent> testSteps = new ArrayList<RunableComponent>();

    @JSONField(ordinal=4)
    private LazyString name;

    @JSONField(ordinal=3)
    private Variables variables;

    @JSONField(ordinal=5)
    private Validate validate;

    private LazyString base_url;

    private String testCase_path;

    private Parameters parameters;

    @JSONField(ordinal=6)
    private TestCase testCase_def;

    @JSONField(ordinal=2)
    private String testCase; //其实就是testCase_path

    private Runner runner;

    @JSONField(ordinal=10)
    private Extract extract;

    private SetupHooks setup_hooks;

    private TeardownHooks teardown_hooks;

    public TestCase(){
    }

    /**
     * 造函数一： 用于在testsuite/testcase文件中，加载嵌套的testcase节点
     * @param testCaseName testcase的name
     * @param raw_testcase 原始testcase内容，这里的testcase是testsuite文件中的某个testcase节点内容
     */
    public TestCase(String testCaseName, Map raw_testcase){
        this.name = new LazyString(testCaseName);

        Optional.ofNullable(raw_testcase.get("variables")).map(
                v -> this.variables = new Variables((Map)raw_testcase.get("variables"))
        );

        Optional.of(raw_testcase.get("testcase")).map(
                v -> this.testCase_def = new TestCase((String)raw_testcase.get("testcase"))
        );

        Optional.ofNullable(raw_testcase.get("validate")).map(
                v -> this.validate = new Validate((List)raw_testcase.get("validate"))
        );

        Optional.ofNullable(raw_testcase.get("parameters")).map(
                v -> this.parameters = new Parameters((Map)raw_testcase.get("parameters"))
        );

        Optional.ofNullable(raw_testcase.get("extract")).map(
                h -> this.extract = new Extract((List)raw_testcase.get("extract"))
        );

        //TODO:还需要加载setup_hooks和teardown_hooks
        Optional.ofNullable(raw_testcase.get("setup_hooks")).map(
                h -> this.setup_hooks = new SetupHooks((List)raw_testcase.get("setup_hooks"))
        );

        Optional.ofNullable(raw_testcase.get("teardown_hooks")).map(
                h -> this.teardown_hooks = new TeardownHooks((List)raw_testcase.get("teardown_hooks"))
        );
    }

    /**
     * 造函数二： 用于加载指定路径下的testcase文件,file_path为相对路径
     * @param file_path 文件的相对路径，来源是TestSuite文件中的testcase值，形式例如 testcase: testcases/setup.yml
     */
    public TestCase(String file_path){
        List fileContent = (List) Loader.load_file(Loader.trans2AbsolutePath(file_path));

        for(int i=0;i<fileContent.size();i++){
            if(!(fileContent.get(i) instanceof Map))
                HrunExceptionFactory.create("E0016");
            if(((Map<String,Object>)fileContent.get(i)).containsKey("config")) {
                this.config = new Config((Map) (((Map<String, Object>) fileContent.get(i)).get("config")));
            }
            else if(((Map<String,Object>)fileContent.get(i)).containsKey("test")) {
                Map tmp = (Map<String, Object>) ((Map<String, Object>) fileContent.get(i)).get("test");
                this.testSteps.add(Loader.load_teststep(tmp));
            }
            else
                logger.error("unexpected block key: block key should only be 'config' or 'test'.");
        }

    }

    /**
     * 构造函数三：用于加载testcase文件中嵌套的testcase
     * @param raw_testcase 原始testcase内容
     */
    @Deprecated
    public TestCase(Map raw_testcase){
        if (Optional.ofNullable(raw_testcase.get("name")).isPresent()) {
            this.name = new LazyString((String) raw_testcase.get("name"));
        }

        Optional.ofNullable(raw_testcase.get("variables")).map(
                h -> this.variables = new Variables((Map)h)
        );

        TODO:Optional.ofNullable(raw_testcase.get("extract")).map(
                h -> this.extract = new Extract((List)raw_testcase.get("extract"))
        );

        this.testCase_def = new TestCase((String)raw_testcase.get("testcase"));
    }

    @Override
    public void parse(Set check_variables_set){
        Optional.ofNullable(this.config).ifPresent( u -> u.parse(check_variables_set));
        Optional.ofNullable(this.name).ifPresent( m -> m.parse(check_variables_set));
        Optional.ofNullable(this.variables).ifPresent( h -> h.parse(check_variables_set));
        Optional.ofNullable(this.validate).ifPresent( j -> j.parse(check_variables_set));
        Optional.ofNullable(this.base_url).ifPresent( p -> p.parse(check_variables_set));
        Optional.ofNullable(this.parameters).ifPresent( p -> p.parse(check_variables_set));
    }

    @Override
    public Parseable to_value(Variables variables_mapping) {
        return null;
    }

    public TestCase _parse_testcase(ProjectMapping project_mapping){
        return _parse_testcase(project_mapping,null);
    }

    public TestCase _parse_testcase(ProjectMapping project_mapping,Set session_variables_set){
        TestCase newTestCase = new TestCase();
        //这里设置了默认的config，这样后面就不会报空指针的错误了
        if(this.config == null)
            setConfig(new Config());

        Config prepared_config = this.config.parse_config(project_mapping,session_variables_set);
        List<RunableComponent> prepared_testcase_tests = __prepare_testcase_tests(
                prepared_config,
                project_mapping,
                session_variables_set
        );

        newTestCase.setConfig(prepared_config);
        newTestCase.setTestSteps(prepared_testcase_tests);
        return newTestCase;
    }

    public List<RunableComponent> __prepare_testcase_tests(Config config, ProjectMapping project_mapping,Set<String> session_variables_set){
        List<RunableComponent> prepared_testcase_tests = new ArrayList<RunableComponent>();

        Variables config_variables = config.getVariables();
        LazyString config_base_url = config.getBase_url();
        Boolean config_verify = config.getVerify();

        session_variables_set = Optional.ofNullable(session_variables_set).orElse(new HashSet<String>());
        if(!Variables.isNullOrEmpty(config_variables))
            session_variables_set.addAll(config_variables.getVariables().keySet());

        for(RunableComponent test_dict : testSteps){
            Set<String> teststep_variables_set = new HashSet<String>(){{
                add("request");
                add("response");
            }};
            Variables test_dict_variables = Variables.extend2Variables(
                    test_dict.getVariables(),
                    config_variables
            );
            test_dict.setVariables(test_dict_variables);
            // base_url & verify: priority test_dict > config
            if(test_dict.getBase_url() == null && config_base_url != null){
                test_dict.setBase_url(config_base_url);
            }

            if(test_dict instanceof TestCase){
                //# test_dict is nested testcase
                //
                // # pass former teststep's (as a testcase) export value to next teststep
                // # Since V2.2.2, `extract` is used to replace `output`,
                // # `output` is also kept for compatibility
                if(test_dict.getExtract() != null){
                    session_variables_set.addAll(test_dict.getExtract().getExtract2Output());
                }
                //if "extract" in test_dict:
                //                session_variables_set |= set(test_dict["extract"])
                //            elif "output" in test_dict:
                //                # kept for compatibility
                //                session_variables_set |= set(test_dict["output"])

                // 2, testcase test_dict => testcase_def config
                TestCase testcase_def = ((TestCase) test_dict).getTestCase_def();
                test_dict = ((TestCase)test_dict)._extend_with_testcase(testcase_def);

                // verify priority: nested testcase config > testcase config
                if(test_dict.getConfig().getVerify() == null)
                    test_dict.getConfig().setVerify(config_verify);

                test_dict = ((TestCase)test_dict)._parse_testcase(project_mapping, session_variables_set);
            }else if(test_dict instanceof Api) {
                Api api_def_dict = ((Api) test_dict).getApi_def();
                ((Api)test_dict)._extend_with_api(api_def_dict);
            }

            // TODO；verify priority: testcase teststep > testcase config

            // current teststep variables
            Optional.ofNullable(test_dict.getVariables()).ifPresent( var ->
                teststep_variables_set.addAll(var.getVariables().keySet())
            );

            // move extracted variable to session variables
            if(test_dict.getExtract() != null && !test_dict.getExtract().isEmpty()){
                session_variables_set.addAll(test_dict.getExtract().getExtract().keySet());
            }

            // TODO:move extracted variable to session variables
            teststep_variables_set.addAll(session_variables_set);

            // convert validators to lazy function
            // hrun原版的validator的解析，在java中放到了test_dict.parse方法中

            test_dict.parse(teststep_variables_set);

            prepared_testcase_tests.add(test_dict);

        }

        return prepared_testcase_tests;
    }

    @Override
    public Variables getVariables(){
        return this.variables;
    }

    public TestCase _extend_with_testcase(TestCase testcase_def_dict){
        //TODO：要像_extend_with_api方法一样，返回void，把this修改一下就可以了
        //override testcase config variables
        if(Optional.ofNullable(testcase_def_dict.getConfig().getVariables()).isPresent())
            testcase_def_dict.getConfig().setVariables(new Variables());
        Variables testcase_def_variables = testcase_def_dict.getConfig().getVariables();
        testcase_def_variables.extend(this.getVariables());
        testcase_def_dict.getConfig().setVariables(testcase_def_variables);

        // override base_url, verify
        // priority: testcase config > testsuite tests
        LazyString test_base_url = this.getBase_url();
        if(test_base_url != null && testcase_def_dict.getConfig().getBase_url() == null)
            testcase_def_dict.getConfig().setBase_url(test_base_url);

        // override name
        LazyString test_name;
        if(this.getName()==null){
            if(testcase_def_dict.getConfig().getName()==null){
                test_name = new LazyString("testcase name undefined");
            }else{
                test_name = testcase_def_dict.getConfig().getName();
            }
        }else{
            test_name = this.getName();
        }

        // override testcase config name, output, etc.
        Optional.ofNullable(getExtract()).ifPresent(
            ext ->testcase_def_dict.getConfig().setExtract(ext)
        );

        Optional.ofNullable(getExtract()).ifPresent(
                ext ->testcase_def_dict.getConfig().setExtract(ext)
        );
        if(this.getExtract() != null)
            testcase_def_dict.getConfig().setExtract(this.getExtract());
        // TODO:
        // Optional.ofNullable(getOutput()).ifPresent(
        //        name ->testcase_def_dict.getConfig().setName(name)
        // );
        testcase_def_dict.getConfig().setName(test_name);
        return testcase_def_dict;
    }

    @Override
    public LazyString getName(){
        return this.name;
    }

}

















