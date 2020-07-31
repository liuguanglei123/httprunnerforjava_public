package com.hrun.report;

import com.hrun.component.testcase.TestCase;
import com.hrun.component.testcase.TestCaseRun;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class HtmlTestResult {
    //用例错误后是否继续往下执行
    private Boolean failfast;

    //TODO:???
    private Boolean buffer;

    private Boolean shouldStop;

    private List<Map<String, Object>> records = new ArrayList<>();

    private Long start_at;

    private Double duration;

    private Integer testsRun;

    public HtmlTestResult(Boolean failfast, Boolean buffer) {
        this.failfast = failfast;
        this.buffer = buffer;
        shouldStop = false;
        testsRun = 0;
        start_at = 0L;
        duration = 0d;
    }

    public void _record_test(TestCaseRun test, String status) {
        _record_test(test, status, "");
    }

    public void _record_test(TestCaseRun test, String status, String attachment) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", test.getDoc());
        data.put("status", status);
        data.put("attachment", attachment);
        data.put("meta_datas", test.getMeta_datas());

        this.records.add(data);
    }

    public void startTest() {
        testsRun += 1;
        // logger.color_print(test.shortDescription(), "yellow")
    }

    public void addSuccess(TestCaseRun test) {
        _record_test(test, "success");
    }

    public void addError(TestCaseRun test, String errorMsg) {
        this.runNum.put("erroredStepNum", this.runNum.get("erroredStepNum") + 1);
        _record_test(test, "error", errorMsg);
    }

    public void addFailure(TestCaseRun test, String failMsg) {
        this.runNum.put("failedStepNum", this.runNum.get("failedStepNum") + 1);
        _record_test(test, "failure", failMsg);
    }

    private Map<String, Integer> runNum = new HashMap<String, Integer>() {{
        put("testsStepRun", 0);
        put("failedStepNum", 0);
        put("erroredStepNum", 0);
        put("successedStepNum", 0);
        put("skippedStepNum", 0);
    }};

    public Boolean wasSuccessful() {
        return this.runNum.get("failedStepNum") == 0 && this.runNum.get("erroredStepNum") == 0;
    }

    public Long startTestRun() {
        start_at = System.currentTimeMillis();
        return start_at;
    }

    public void printErrors() {
        //TODO:
//        if self.dots or self.showAll:
//            self.stream.writeln()
//        self.printErrorList('ERROR', self.errors)
//        self.printErrorList('FAIL', self.failures)
    }

}
