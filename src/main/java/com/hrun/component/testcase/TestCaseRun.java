package com.hrun.component.testcase;

import com.hrun.Utils;
import com.hrun.component.RunTestCls;
import com.hrun.exceptions.HrunBizException;
import com.hrun.report.HtmlTestResult;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class TestCaseRun {
    static Logger logger = LoggerFactory.getLogger(TestCaseRun.class);

    private String test_method_name;

    private String doc;

    private RunTestCls test_method;

    private Object meta_datas;

    public String shortDescription(){
        return this.test_method_name;
    }

    public void run(HtmlTestResult result){
        HtmlTestResult orig_result = result;
        result.startTest();

        //TODO: python此处使用的是with语法，作用是在调用calltestmethod方法后，自动调用
        // _GeneratorContextManager的__exit__方法，将callTestMethod中的异常又捕获到，而且不影响后续流程的执行
        // 这里只是简单的使用try catch捕获异常，让流程可以正常执行下去
        // TODO: 可以研究一下，是否可以使用反射优化一下这里的流程
        try{
            _callTestMethod(result);
        }catch(Exception e){
            logger.error("发现不可捕获的异常，说明用例执行失败");
            e.printStackTrace();
        }
        finally{

        }
    }

    public void _callTestMethod(HtmlTestResult result) throws Exception{
        try {
            test_method.runTest();
        }catch(HrunBizException e){
            fail(e.toString());
            this.meta_datas = test_method.getTest_runner().getMeta_datas();
            result.addFailure(this, Utils.getErrorInfoFromException(e));
            return;
        }catch(Exception e){
            this.meta_datas = test_method.getTest_runner().getMeta_datas();
            result.addError(this, Utils.getErrorInfoFromException(e));
            throw e;
        }
        this.meta_datas = test_method.getTest_runner().getMeta_datas();
        result.addSuccess(this);
    }

    public void _feedErrorsToResult(){

    }

    public void fail(String failTrace){
        logger.error(failTrace);
        logger.error("发现可捕获的异常，说明用例执行失败");
    }

}
