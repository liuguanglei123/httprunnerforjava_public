package com.hrun.component;

import com.hrun.App;
import com.hrun.Runner;
import com.hrun.component.intf.RunableComponent;
import com.hrun.exceptions.HrunBizException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

@Slf4j
@Data
public class RunTestCls implements Serializable {
    static Logger logger = LoggerFactory.getLogger(RunTestCls.class);

    private RunableComponent test_dict;

    private Runner test_runner;

    public void runTest() throws Exception {
        test_runner.run_test(test_dict);
    }

    public RunTestCls(Runner test_runner, RunableComponent test_dict) {
        this.test_runner = test_runner;
        this.test_dict = test_dict;
    }



}