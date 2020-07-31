package com.hrun;

import org.junit.Test;

import java.util.regex.Matcher;

import static com.hrun.Parse.variable_regex_compile;

public class ParseTest {

    @Test
    public void test_dolloar_regex_compile(){

    }

    @Test
    public void test_variable_regex_compile(){
        String str1 = "${var}";
        String str2 = "$var";
        Matcher matcher1 = variable_regex_compile.matcher(str1);
        Matcher matcher2 = variable_regex_compile.matcher(str2);
        if(matcher1.find()){
            System.out.println(matcher1.groupCount());
            assert matcher1.group(1).equals("var");
        }
        if(matcher2.find()){
            System.out.println(matcher2.groupCount());
            System.out.println(matcher2.group(2));
            assert matcher2.group(2).equals("var");
        }
        assert variable_regex_compile.matcher("${var1}").matches() == true;
        assert variable_regex_compile.matcher("$var1}").matches() == false;
        assert variable_regex_compile.matcher("${var1").matches() == false;
    }

    @Test
    public void test_function_regex_compile(){

    }
}
