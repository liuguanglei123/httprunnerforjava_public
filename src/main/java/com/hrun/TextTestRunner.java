package com.hrun;

import com.hrun.component.testsuite.TestSuiteRun;
import com.hrun.report.HtmlTestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextTestRunner {
    static Logger logger = LoggerFactory.getLogger(TextTestRunner.class);

    //TODO: 需要重写
    private Boolean failfast;

    private Class resultclass;

    public TextTestRunner(Boolean failfast,Class resultclass){
        //TODO:hrun原版中，这里有很多内容，比如stream descriptions verbosity等变量的定义，但大多数都是取的默认值
        this.failfast = failfast;
        this.resultclass = resultclass;
    }

    public HtmlTestResult run(TestSuiteRun testSuiteRun){
        //低配版的result记录
        HtmlTestResult result = new HtmlTestResult(failfast,false);
        Long startTime = result.startTestRun();

        testSuiteRun.run(result);

        Long stopTime = System.currentTimeMillis();
        Long timeTaken = stopTime - startTime;
        result.setDuration(timeTaken/1000d);

        result.printErrors();
        Integer run = result.getTestsRun();
        logger.info(String.format("Ran %d test%s in %.3f s",run,run.equals(1)?"s":"",timeTaken/1000d));
        logger.info("----------------------------------------------------------------------");
        return result;
    }

}
