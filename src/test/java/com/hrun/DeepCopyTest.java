package com.hrun;

import com.hrun.component.testsuite.TestSuite;
import com.hrun.lazyContent.LazyFunction;
import org.junit.Test;

public class DeepCopyTest {

    @Test
    public void DeepCopyTestsuite(){
        Loader.set_project_working_directory("C:\\Users\\liu\\Desktop\\HttpRunner\\hrunforjava\\src\\test\\testProject");
        TestSuite testsuite = new TestSuite("C:\\Users\\liu\\Desktop\\HttpRunner\\hrunforjava\\src\\test\\testProject\\testsuites\\singletestcase.yml");
        TestSuite newTestSuite = (TestSuite)Utils.deepcopy_dict(testsuite);
//        Utils.dump_logs(newTestSuite,"tmp_tests_mapping");
    }

    @Test
    public void test2(){
        LazyFunction a = new LazyFunction("test");
        a.setFunc_name("123456");
        LazyFunction b = Utils.deepcopy_obj(a);
        System.out.println(b.getRaw_str());
        System.out.println(b.getFunc_name());
//        TestCloneClass a = new TestCloneClass("123456");
//        TestCloneClass b = Utils.deepcopy_obj(a);
//        System.out.println(b.getRaw_str());
    }

}
