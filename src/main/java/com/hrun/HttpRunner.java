package com.hrun;

import com.hrun.HrunLogger.LoggerSetting;
import com.hrun.component.ProjectMapping;
import com.hrun.component.RunTestCls;
import com.hrun.component.TestMapping;
import com.hrun.component.common.Config;
import com.hrun.component.intf.RunableComponent;
import com.hrun.component.testcase.TestCase;
import com.hrun.component.testcase.TestCaseRun;
import com.hrun.component.testsuite.TestSuiteRun;
import com.hrun.exceptions.HrunExceptionFactory;
import com.hrun.lazyContent.LazyString;
import com.hrun.report.HtmlTestResult;
import com.hrun.report.Summarize;
import com.hrun.report.Summary;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Data
public class HttpRunner {

    static Logger logger = LoggerFactory.getLogger(HttpRunner.class);

    private Boolean failfast;

    private Boolean save_test;

    private String report_template;

    private String report_dir;

    private String log_level;

    private String log_file;

    private String report_file;

    private TextTestRunner unittest_runner;

    private Summary _summary;

    private String exception_stage;

    private String project_working_directory;


    public HttpRunner(Boolean failfast, Boolean save_test,  String log_level, String log_file){
        if(log_level != null) {
            LoggerSetting.setLogLevel(log_level);
        }

        this.exception_stage = "initialize HttpRunner()";

        this.failfast = failfast;
        this.save_test = save_test;
        this.log_file = log_file;
        this.unittest_runner = new TextTestRunner(failfast, HtmlTestResult.class);
    }

    public Summary run_path(String path,String dot_env_path){
        return run_path(path,dot_env_path,null);
    }

    public Summary run_path(String path,String dot_env_path,Map mapping){
        //TODO: hrun可以执行两种测试用例：filepath，或者 testcasebody，在java实现中分别对应两个方法，run_path和run_tests，这里暂时只实现了执行filepath方式
        // hrun中还有一个逻辑，就是可以执行变量集合mapping，详见api.py中的run_path，可以用来更新tests_mapping["project_mapping"]["variables"]
        // 目的是hrun如果作为包被其他项目引入时，可以直接指定变量集合
        logger.info("HttpRunner version: {}".format(App.version));

        this.exception_stage = "load tests";
        TestMapping tests_mapping = Loader.load_cases(path, null);

        if(mapping != null){
            tests_mapping.getProject_mapping().setVariables(mapping);
        }

        return run_tests(tests_mapping);
    }

    /**
     * 执行测试的总入口
     * @param tests_mapping 已经加载号的TestMapping对象，包含了要执行的用例/变量/Debugtalk类
     * @return 测试报告类
     */
    public Summary run_tests(TestMapping tests_mapping){
        logger.debug("start to run tests");

        ProjectMapping project_mapping = tests_mapping.getProject_mapping();
        project_working_directory = project_mapping.getPWD();
        logger.debug("当前执行的根目录是" + project_working_directory);

        if(this.save_test)
            Utils.dump_logs(tests_mapping, project_mapping, "loaded");

        this.exception_stage = "parse tests";
        List<TestCase> parsed_testcases = Parse.parse_tests(tests_mapping);
        //TODO:parse_failed_testfiles = parser.get_parse_failed_testfiles()

        if(parsed_testcases.size() == 0){
            logger.error("failed to parse all cases, abort.");
            HrunExceptionFactory.create("E0033");
        }
        if(this.save_test){
            Utils.dump_logs(parsed_testcases, project_mapping, "parsed");
        }

        // add tests to test suite
        this.exception_stage = "add tests to test suite";
        List<TestSuiteRun> test_suite = _add_tests(parsed_testcases);

        // run test suite
        this.exception_stage = "run test suite";
        Map results = _run_suite(test_suite);

        // aggregate results
        this.exception_stage = "aggregate results";
        this._summary = this._aggregate(results);

        //generate html report
        this.exception_stage = "generate html report";
        this._summary.stringify_summary();

        if(this.save_test){
            Utils.dump_logs(_summary, project_mapping, "summary");
//            save variables and export data
//            vars_out = self.get_vars_out()
//            utils.dump_logs(vars_out, project_mapping, "io")
        }

        return _summary;
    }

    public List<TestSuiteRun> _add_tests(List<TestCase> parsed_testcases){
        // 最终add_test之后，所有的测试用例都放在了一个test_suite中，如果在执行的时候选择了多个testsuite文件，
        // 那么这些testsuite内容加载后，会以多个testcase形式存在这里定义的test_suite中。每个testcase中，都有一个runner，用来存储上下文什么的
        // 也就是说，testsuite文件，降级整理到了testcase级别存储起来再执行
        List<TestSuiteRun> test_suite_runs = new ArrayList<TestSuiteRun>();

        for(TestCase testcase : parsed_testcases){
            Config config = Optional.ofNullable(testcase.getConfig()).orElse(new Config());
            // 每个testsuite中，都包含一个suite内公用的test_runner，起点在这里
            // 那么test_runner存在的意义是什么？
            // 暂时的理解是，这里的test_runner包含了上下文的信息，
            // 在hrun原版中，保存了一个requests.Session和一些变量信息，
            // session的作用是保持会话，只要在同一个testcase中，上一步的cookie等会保存起来让下一步使用，
            // 变量信息的作用更简单了，让每个testcase都能共享某些变量值
            // 所以，我们的目的也很简单，在一个testcase中能够共享某些信息就可以了
            // TODO：添加一些测试用例尝试一下是否有效
            Runner test_runner = new Runner(config);
            TestSuiteRun testSuiteRun = new TestSuiteRun();

            List<RunableComponent> tests = testcase.getTestSteps();
            for(int i=0;i<tests.size();i++){
                //TODO：我觉得这里还可以实现随机执行用例的逻辑
                String test_method_name = String.format("test_%4d",i);
                RunTestCls test_method = createRunTestStep(test_runner,tests.get(i));
                TestCaseRun testCaseRun = new TestCaseRun();
                testCaseRun.setTest_method_name(test_method_name);
                testCaseRun.setTest_method(test_method);
                testSuiteRun.add_test(testCaseRun);
            }

            testSuiteRun.setConfig(config);
            testSuiteRun.setTestSteps(tests);
            testSuiteRun.setRunner(test_runner);
            test_suite_runs.add(testSuiteRun);

        }

        return test_suite_runs;
    }

    public RunTestCls createRunTestStep(Runner test_runner, RunableComponent test_dict){
        return new RunTestCls(test_runner,test_dict);
    }

    public Map _run_suite(List<TestSuiteRun> testSuite){
        Map<TestSuiteRun,HtmlTestResult> tests_results = new LinkedHashMap<>();

        for(TestSuiteRun testSuiteRun : testSuite){
            LazyString testcase_name = testSuiteRun.getConfig().getName();
            // TODO:testcase_name 这里的testcase_name需要parse
            logger.info("Start to run testcase: {}",testcase_name.getRaw_value());
            HtmlTestResult result = unittest_runner.run(testSuiteRun);
            tests_results.put(testSuiteRun,result);
            //TODO:
            /* if result.wasSuccessful():
                tests_results.append((testcase, result))
            else:
                tests_results.insert(0, (testcase, result))*/
        }
        return tests_results;
    }

    public Summary _aggregate(Map<TestSuiteRun,HtmlTestResult> tests_results){
        Summary summary = new Summary();
        summary.getTestcasesStat().put("total",tests_results.size());
        for(Map.Entry<TestSuiteRun,HtmlTestResult> entry : tests_results.entrySet()){
            Map<String,Object> testcase_summary = Summarize.get_summary(entry.getValue());

            if((Boolean)testcase_summary.get("success"))
                summary.getTestcasesStat().put("success",summary.getTestcasesStat().get("success")+1);
            else
                summary.getTestcasesStat().put("fail",summary.getTestcasesStat().get("fail")+1);

            summary.setSuccess(summary.getSuccess() & (Boolean)testcase_summary.get("success"));
            testcase_summary.put("name",entry.getKey().getConfig().getName());
            //TODO: testcase_summary["in_out"] = utils.get_testcase_io(testcase)

            Summarize.aggregate_stat(summary.getTeststepsStat(), (Map<String,Long>)testcase_summary.get("stat"));
            Summarize.aggregate_stat(summary.getTime(), (Map<String,Long>)testcase_summary.get("time"));
            summary.getDetails().add(testcase_summary);
        }

        return summary;
    }

}
