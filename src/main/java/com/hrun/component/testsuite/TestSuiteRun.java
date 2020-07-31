package com.hrun.component.testsuite;

import com.alibaba.fastjson.annotation.JSONField;
import com.hrun.Runner;
import com.hrun.component.common.Config;
import com.hrun.component.intf.RunableComponent;
import com.hrun.component.testcase.TestCaseRun;
import com.hrun.report.HtmlTestResult;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestSuiteRun {

    private Config config;

    private List<RunableComponent> testSteps;

    private Runner runner;

    private List<TestCaseRun> _test = new ArrayList<TestCaseRun>();

    public void add_test(TestCaseRun testCaseRun){
        if(testCaseRun.getTest_method().getTest_dict().getConfig() != null){
            testCaseRun.setDoc(testCaseRun.getTest_method().getTest_dict().getConfig().getName().getRaw_value());
        }else{
            testCaseRun.setDoc(testCaseRun.getTest_method().getTest_dict().getName().getRaw_value());
        }
        _test.add(testCaseRun);
    }

    public HtmlTestResult run(HtmlTestResult result){
        for(int i=0;i<_test.size();i++){
            if(result.getShouldStop())
                break;

            _test.get(i).run(result);
        }

        return result;
    }
}
