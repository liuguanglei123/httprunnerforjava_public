package com.hrun;

import com.hrun.component.TestMapping;
import com.hrun.component.api.Api;
import com.hrun.component.intf.RunableComponent;
import com.hrun.component.testcase.TestCase;
import com.hrun.component.testsuite.TestSuite;
import com.hrun.exceptions.HrunBizException;
import com.hrun.exceptions.HrunExceptionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Loader {
    private static Logger logger = LoggerFactory.getLogger(Loader.class);

    private static String project_working_directory;

    /**
     * 相对路径转绝对路径，这里的相对路径的相对地址是变量project_working_directory
     * @param file_path 相对路径，比如 testcases/setup.yml
     * @return
     */
    public static String trans2AbsolutePath(String file_path){
        String pwd = get_project_working_directory();
        Path tmp = Paths.get(pwd);
        for (String each : file_path.split("/")) {
            tmp = tmp.resolve(each);
        }

        return tmp.toAbsolutePath().toString();
    }

    /**
     * load_file 加载指定文件内容并直接返回，不做其他处理；
     * TODO：暂时只支持加载yml类型文件，后续升级支持json和excel
     * @param file_path 文件路径必须是绝对路径！
     * @return
     */
    public static Object load_file(String file_path){
        File file = new File(file_path);
        if (file.isDirectory() || !file.exists()) {
            logger.error("file does not exist or not is file,file_path is " + file_path);
            HrunExceptionFactory.create("E0015");
        }

        String file_suffix = file_path.substring(file_path.lastIndexOf(".")).toLowerCase();
        if (file_suffix.equals(".json"))
            HrunExceptionFactory.create("E0029");
        else if (file_suffix.equals(".yaml") || file_suffix.equals(".yml"))
            return load_yaml_file(file);
        else if (file_suffix.equals(".csv"))
            HrunExceptionFactory.create("E0029");
        else {
            logger.error("Unsupported file format:" + file_path);
            HrunExceptionFactory.create("E0029");
        }

        return null;
    }

    public static String get_project_working_directory(){
        if (project_working_directory == null) {
            logger.error("loader.load_cases() has not been called!");
            HrunExceptionFactory.create("E0017");
        }

        return project_working_directory;
    }

    /**
     * 加载yaml文件并简单检查文件内容是否是list或map
     * @param file 文件对象
     * @return
     */
    public static Object load_yaml_file(File file) {
        //load yaml file and check file content format
        Object obj = null;
        FileInputStream fileInputStream;
        try {
            Yaml yaml = new Yaml();
            fileInputStream = new FileInputStream(file);
            obj = yaml.load(fileInputStream);
            _check_format(file.getAbsolutePath(), obj);
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            logger.error("load file error,file_path is" + file.getAbsolutePath());
            return null;
        } catch (IOException e){
            logger.error("文件流关闭失败.");
        }
        return obj;
    }

    public static Boolean _check_format(String yaml_file, Object content) {
        if (content == null) {
            logger.error("Testcase file content is empty: " + yaml_file);
            HrunExceptionFactory.create("E0002");
        }
        if (!(content instanceof Map || content instanceof List)) {
            logger.error("Testcase file content format invalid: " + yaml_file);
            HrunExceptionFactory.create("E0003");
        }
        return true;
    }

    public static RunableComponent load_teststep(Map raw_testinfo){
        if(raw_testinfo.containsKey("api")) {
            Api api = new Api(raw_testinfo,false);
            return api;
        }else if(raw_testinfo.containsKey("testcase")){
            TestCase testCase = new TestCase((String)raw_testinfo.get("name"),raw_testinfo);
            return testCase;
        }else{
            HrunExceptionFactory.create("E0016");
        }

        return null;
    }

    /**
     * load_cases 加载测试用例文件的总入口，整个加载是在该方法中完成的，执行内容包括 加载测试文件/目录，编译Debugtalk.java文件，加载env文件等
     * @param path 测试用例文件/目录，可能是相对/绝对路径
     * @param dot_env_path 环境变量文件路径
     * @return
     */
    //TODO:path是目录的场景还没有测试
    public static TestMapping load_cases(String path, String dot_env_path) {
        TestMapping tests_mapping = new TestMapping();
        File file = Utils.getFile(path);
        tests_mapping.load_project_data(file.getAbsolutePath(), dot_env_path);

        if (file.isDirectory()) {
            // TODO: test
            List<String> files_list = load_folder_files(path, true);
            for (Iterator<String> everypath = files_list.iterator(); everypath.hasNext(); ) {
                //TODO:__load_file_content只支持绝对路径加载
                __load_file_content((String) everypath.next(), tests_mapping);
            }
        } else if (file.isFile()) {
            __load_file_content(file.getAbsolutePath(), tests_mapping);
        }

        return tests_mapping;
    }

    /**
     * 加载测试文件内容
     * @param path 文件绝对路径！
     * @param tests_mapping
     */
    public static void __load_file_content(String path, TestMapping tests_mapping) {
        RunableComponent loaded_content = null;
        try {
            loaded_content = load_test_file(path);
        } catch (Exception e) {
            //TODO:异常工厂方法需要进一步细化，不同的异常要用不同的工厂方法创建。比如api加载错误的异常，用apifactory创建异常，
            // 这样后文才可以进行区分 low_prority
            logger.error("Invalid test file format: " + path);
            e.printStackTrace();
        }
        switch(loaded_content.getClass().getSimpleName()) {
            case "TestSuite":
                tests_mapping.getTestSuites().add((TestSuite)loaded_content);
                break;
            case "TestCase":
                tests_mapping.getTestCases().add((TestCase)loaded_content);
                break;
            case "Api":
                tests_mapping.getApis().add((Api)loaded_content);
                break;
            default:
                logger.error("加载的文件内容为空？");
                HrunExceptionFactory.create("E0032");
        }
    }

    /**
     * load_test_file 加载测试文件，测试文件可以是testsuite testcase或api文件，暂时只支持加载TestSuite级别。
     * TODO：后续升级解决支持加载TestCase和Api low_priority
     * @param path 路径是绝对路径！
     * @return
     */
    public static RunableComponent load_test_file(String path) {
        // 原样加载文件内容
        Object raw_content = load_file(path);

        if (raw_content instanceof Map) {
            if (((Map) raw_content).containsKey("testcases")) {
                TestSuite testSuite = new TestSuite((Map) raw_content);
                testSuite.setPath(path);
                return testSuite;
            } else if (((Map) raw_content).containsKey("teststeps")) {
                HrunExceptionFactory.create("E0060");
            } else if (((Map) raw_content).containsKey("request")) {
                HrunExceptionFactory.create("E0060");
            } else {
                HrunExceptionFactory.create("E0060");
            }
        } else if (raw_content instanceof List && ((List) raw_content).size() > 0) {
            //TODO:
        } else {
            HrunExceptionFactory.create("E0060");
        }

        return null;
    }

    public static List<String> load_folder_files(String folder_path, Boolean recursive) {
        //TODO:
        File folder = new File(folder_path);
        if (!folder.exists())
            return new ArrayList<String>();
        List<String> file_list = new ArrayList<String>();
        File[] files = folder.listFiles();
        for (File tmpfile : files) {
            String fileName = tmpfile.getAbsolutePath();
            if (fileName.endsWith(".yml") || fileName.endsWith(".yaml") || fileName.endsWith(".json")) {
                file_list.add(fileName);
            }
            if (tmpfile.isDirectory() && recursive == true) {
                file_list.addAll(load_folder_files(tmpfile.getAbsolutePath(), recursive));
            }
        }
        return file_list;
    }

    /*
        init_project_working_directory 初始化项目路径，定位到debugtalk.java文件路径，并作为项目路径存储
        test_path 要执行的文件或目录路径，必须是绝对路径
     */
    public static List<String> init_project_working_directory(String test_path) {
        String debugtalk_path = locate_debugtalk_py(test_path);
        set_project_working_directory(
                Optional.of(debugtalk_path)
                        .map(str -> new File(str).getParentFile().getAbsolutePath()).get());

        List<String> result = new ArrayList<String>() {{
            add(debugtalk_path);
            add(get_project_working_directory());
        }};

        return result;
    }

    /*
        locate_debugtalk_py 定位debugtal.java文件路径，在start_path目录逐层向上递归，最终返回debugtalk.java文件的绝对路径，否则抛异常
        TODO：这里还是要修改的，找不到debugtalk文件就返回给空
        start_path 必须是绝对路径
     */
    public static String locate_debugtalk_py(String start_path) {
        String debugtalk_path = null;
        try {
            debugtalk_path = locate_file(start_path, "Debugtalk.java");
        } catch (HrunBizException e) {
            logger.info("debugtalk.java file does not locate.");
            throw e;
            // REMARK：这里与hrun原版不同，debugtalk.java文件必须存在，否则会抛出异常
        }
        return debugtalk_path;
    }

    /*
        locate_file 定位文件路径，在start_path目录逐层向上递归，查找指定file_name文件是否存在，如果存在，返回file_name文件的绝对路径
        start_path 必须是绝对路径
     */
    public static String locate_file(String start_path, String file_name) {
        File file = new File(start_path);
        String start_dir_path = "";
        if (file.isFile())
            start_dir_path = file.getParentFile().getAbsolutePath();
        else if (file.isDirectory())
            start_dir_path = file.getAbsolutePath();
        else {
            logger.error("invalid path: " + start_path);
            HrunExceptionFactory.create("E0006");
        }

        Path path = Paths.get(start_dir_path).resolve(file_name);
        File resultFile = new File(path.toString());
        if (resultFile.exists() && resultFile.isFile())
            return resultFile.getAbsolutePath();

        /*
            TODO：hrun原版这里有个对根目录的判断，我觉得不需要，但需要测试，测试通过后可以把这个TODO删掉
         */

        // locate recursive upward
        return locate_file(new File(start_dir_path).getParentFile().getAbsolutePath(), file_name);
    }

    public static void set_project_working_directory(String path) {
        project_working_directory = path;
    }

    public static Map<String, Object> load_dot_env_file(String dot_env_path) {
        Map<String, Object> env_variables_mapping = new HashMap<String, Object>();
        File env_file = new File(dot_env_path);
        if (!env_file.exists() || !env_file.isFile())
            return env_variables_mapping;

        logger.info("Loading environment variables from " + dot_env_path);

        try {
            FileInputStream fileInputStream = new FileInputStream(env_file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                List<Object> key_value = new ArrayList<Object>();
                if (str.contains("="))
                    key_value = Arrays.asList(str.split("="));
                else if (str.contains(":"))
                    key_value = Arrays.asList(str.split(":"));
                else
                    HrunExceptionFactory.create("E0005");
                env_variables_mapping.put((String)key_value.get(0), key_value.get(1));
            }
        } catch (IOException e) {
            HrunExceptionFactory.create("E0030");
        }
        //TODO 原hrun中有下面一段逻辑
        //utils.set_os_environ(env_variables_mapping)

        return env_variables_mapping;
    }

    public static Class load_builtin_functions() {
        return load_module_functions("builtin");
    }

    public static Class load_module_functions(String module) {
        Class module_functions = CompilerFile.loadClass(module);

        return module_functions;
    }

}
