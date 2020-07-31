package com.hrun;

import com.hrun.component.testsuite.TestSuite;
import org.junit.Test;

public class DeepCopyTest {

    @Test
    public void DeepCopyTestsuite(){
        Loader.set_project_working_directory("C:\\Users\\liu\\Desktop\\HttpRunner\\hrunforjava\\src\\test\\testProject");
        TestSuite testsuite = new TestSuite("C:\\Users\\liu\\Desktop\\HttpRunner\\hrunforjava\\src\\test\\testProject\\testsuites\\singletestcase.yml");
        TestSuite newTestSuite = (TestSuite)Utils.deepcopy_dict(testsuite);
//        Utils.dump_logs(newTestSuite,"tmp_tests_mapping");
    }

}
