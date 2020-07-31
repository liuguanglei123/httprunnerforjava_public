package com.hrun;

import com.google.errorprone.annotations.Var;
import com.hrun.component.ProjectMapping;
import com.hrun.component.TestMapping;
import com.hrun.component.common.Config;
import com.hrun.component.common.Parameters;
import com.hrun.component.common.Variables;
import com.hrun.component.testcase.TestCase;
import com.hrun.component.testsuite.TestSuite;
import com.hrun.exceptions.HrunExceptionFactory;
import com.hrun.lazyContent.LazyContent;
import com.hrun.lazyContent.LazyString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static com.hrun.component.ProjectMapping.functions;

public class Parse {
    static Logger logger = LoggerFactory.getLogger(Parse.class);

    //use $$ to escape $ notation
    static public Pattern dolloar_regex_compile = Pattern.compile("\\$\\$");

    //variable notation, e.g. ${var} or $var
    static public Pattern variable_regex_compile = Pattern.compile("\\$\\{(\\w+)\\}|\\$(\\w+)");

    //function notation, e.g. ${func1($var_1, $var_3)}
    static public Pattern function_regex_compile = Pattern.compile("\\$\\{(\\w+)\\(([\\$\\w\\.\\-/\\s=,]*)\\)\\}");

    /**
     * 解析用例的主入口，主要对用例中每个节点每个层级进行parse操作，对于需要处理的LazyString，进行标记和整理
     * @param tests_mapping
     * @return 整理后的TestCase集合
     */
    public static List<TestCase> parse_tests(TestMapping tests_mapping) {
        ProjectMapping project_mapping = tests_mapping.getProject_mapping();
        List<TestCase> testcases = new ArrayList<>();

        if(tests_mapping.getTestSuites() != null && tests_mapping.getTestSuites().size() != 0) {
            List<TestSuite> testsuites = tests_mapping.getTestSuites();
            for (int i = 0; i < testsuites.size(); i++) {
                List<TestCase> parsed_testcases = testsuites.get(i)._parse_testsuite(project_mapping);
                for (TestCase parsed_testcase : parsed_testcases) {
                    testcases.add(parsed_testcase);
                }
            }
        }

        return testcases;
    }

    public static Method get_mapping_function(String function_name){
        Method method = null;
        try{
            //TODO:看看能否优化这里的流程
            method = functions.getDeclaredMethod(function_name);
            return method;
        }catch(NoSuchMethodException e){
//            HrunExceptionFactory.create("E0024");
//            logger.info(String.format("方法 %s 在debugtalk.java中没有找到",function_name));
        }
        //TODO:方法名如果是parameterize 或者environ或者multipart_encoder等等，需要解析

        //TODO:hrun这里采用的是多层调用，获取builtin模块的各个方法，但是java与此不同的是，builtin中的
        // comparator类是一个泛型类，经过测试，虽然能够获取到对应名字的方法，但是执行的时候，需要先实例化一个对象才可以
        try{
            Class built_in_functions = Loader.load_builtin_functions();
            Class[] classed = new Class[2];
            classed[0] = Object.class;
            classed[1] = Object.class;
            method = built_in_functions.getMethod(function_name,classed);
        }catch(Exception e){
            logger.info(String.format("方法 %s 在builtin中也没有找到",function_name));
        }
        if(method == null)
            HrunExceptionFactory.create("E0024");
        return method;
    }

    public static Object parse_lazy_data(LazyString content, Variables variables_mapping) {
        //TODO: refactor type check hrun原版注释
        return content.to_value(variables_mapping);
    }

    /**
     * 对每个变量进行求值
     * TODO：暂时只支持 1 2 3 4 5
     * Args:
     *         variables_mapping (dict):
     *             {
     *  1               "varA": LazyString(123$varB),
     *  2               "varB": LazyString(456$varC),
     *  3               "varC": LazyString(${sum_two($a, $b)}),
     *  4               "a": 1,
     *  5               "b": 2,
     *  6               "c": {"key": LazyString($b)},
     *  7               "d": [LazyString($a), 3]
     *             }
     *
     *     Returns:
     *         dict: parsed variables_mapping should not contain any variable or function.
     *             {
     *                 "varA": "1234563",
     *                 "varB": "4563",
     *                 "varC": "3",
     *                 "a": 1,
     *                 "b": 2,
     *                 "c": {"key": 2},
     *                 "d": [1, 3]
     *             }
     * @param variables_mapping
     * @return
     */
    public static Variables parse_variables_mapping(Variables variables_mapping){
        Integer run_times = 0;
        Variables parsed_variables_mapping = new Variables();

        while(parsed_variables_mapping.getSize() != variables_mapping.getSize() )
            loop1:for(String var_name : variables_mapping.getVariables().keySet()){
                run_times += 1;
                if(run_times > variables_mapping.getSize() * 4){
                    Variables not_found_variables = new Variables();
                    variables_mapping.getVariables().forEach((key, value) ->{
                        if(!parsed_variables_mapping.getVariables().containsKey(key)){
                            not_found_variables.getVariables().put(key,value);
                        }
                    });
                    HrunExceptionFactory.create("E0043");
                }

                if(parsed_variables_mapping.getVariables().containsKey(var_name))
                    continue;

                /*LazyContent value = variables_mapping.getVariable(var_name);
                variables = extract_variables(value);

                if(variables.contains(var_name)){
                    HrunExceptionFactory.create("E0043");
                }

                if(!variables.isEmpty()){
                    for(String _var_name : variables){
                        if(parsed_variables_mapping.getVariables().containsKey(_var_name))
                            continue loop1;
                    }
                }

                parsed_value = parse_lazy_data(value, parsed_variables_mapping);
                parsed_variables_mapping[var_name] = parsed_value*/

            }


            /*for var_name in variables_mapping:

                run_times += 1

                value = variables_mapping[var_name]
                variables = extract_variables(value)

                # check if reference variable itself
                if var_name in variables:
                    # e.g.
                    # var_name = "token"
                    # variables_mapping = {"token": LazyString($token)}
                    # var_name = "key"
                    # variables_mapping = {"key": [LazyString($key), 2]}
                    raise exceptions.VariableNotFound(var_name)

                if variables:
                    # reference other variable, or function call with other variable
                    # e.g. {"varA": "123$varB", "varB": "456$varC"}
                    # e.g. {"varC": "${sum_two($a, $b)}"}
                    if any([_var_name not in parsed_variables_mapping for _var_name in variables]):
                        # reference variable not parsed
                        continue

                parsed_value = parse_lazy_data(value, parsed_variables_mapping)
                parsed_variables_mapping[var_name] = parsed_value
*/
        return parsed_variables_mapping;
    }

    public Object parse_parameters(Parameters parameters){
        return parse_parameters(parameters,null);
    }

    /**
     * 解析参数并生成笛卡尔积
     * 当前支持的参数形式是：
     *  {"user_agent": ["iOS/10.1", "iOS/10.2", "iOS/10.3"]},
     * @param parameters
     * @param variables_mapping
     * @return
     */
    public Object parse_parameters(Parameters parameters, Variables variables_mapping){
        if(Variables.isNullOrEmpty(variables_mapping))
            variables_mapping = new Variables();
        else
            variables_mapping = Utils.deepcopy_obj(variables_mapping);

        return parameters.parse(variables_mapping);
    }
}
