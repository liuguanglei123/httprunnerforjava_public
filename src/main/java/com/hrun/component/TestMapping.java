package com.hrun.component;

import com.alibaba.fastjson.annotation.JSONField;
import com.beust.jcommander.Strings;
import com.hrun.CompilerFile;
import com.hrun.Loader;
import com.hrun.component.api.Api;
import com.hrun.component.testcase.TestCase;
import com.hrun.component.testsuite.TestSuite;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Data
public class TestMapping {

    @JSONField(ordinal=1)
    private ProjectMapping project_mapping;

    @JSONField(ordinal=2)
    private List<TestSuite> testSuites = new ArrayList<TestSuite>();

    @JSONField(ordinal=3)
    private List<TestCase> testCases = new ArrayList<TestCase>();

    @JSONField(ordinal=4)
    private List<Api> apis = new ArrayList<Api>();

    /*
        load_project_data 加载项目下文件，包括定位Debugtalk.java文件，加载.env文件，初始化项目ProjectMapping实例，
        并设置ProjectMapping实例中的pwd test_path属性等。
        另外，这个方法里还编译了Debugtalk.java文件，存放在ProjectMapping实例中。
        test_path 一定是绝对路径
     */
    public void load_project_data(String test_path, String dot_env_path){
        //TODO: 相对路径需要测试
        List<String> paths = Loader.init_project_working_directory(test_path);

        project_mapping = new ProjectMapping();

        String debugtalk_path = paths.get(0);
        String project_working_directory = paths.get(1);

        if (Strings.isStringEmpty(dot_env_path)) {
            Path path = Paths.get(project_working_directory).resolve(".env");
            dot_env_path = path.toString();
        }

        //TODO：.env文件没有测试
        project_mapping.setEnv(Loader.load_dot_env_file(dot_env_path));

        if (Strings.isStringEmpty(debugtalk_path)) {
            project_mapping.setFunctions(null);
        } else {
            ArrayList<String> ops = new ArrayList<String>();
            ops.add("-Xlint:unchecked");
            ops.add("-sourcepath");
            ops.add(project_working_directory);
            Class<?> cls = CompilerFile.loadClass(debugtalk_path, "Debugtalk", ops, project_working_directory);
            project_mapping.setFunctions(cls);
        }

        project_mapping.setPWD(project_working_directory);
        project_mapping.setTest_path(test_path);

        this.setProject_mapping(project_mapping);
    }

}
