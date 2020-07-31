package com.hrun;

import com.alibaba.fastjson.JSON;
import com.hrun.component.common.Variables;
import com.hrun.lazyContent.LazyString;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LaztStringTest {
    @Test
    public void test__parse(){
        LazyString a = new LazyString("ABC${func2($a, $b)}DE$c");
        Set<String> tmpset = new HashSet<String>(){{
            add("a");
            add("b");
            add("c");
        }};
        a.parse(tmpset);
    }

    @Test
    public void testdolloar_regex_compile(){
        String tmp = "ABC${func2($a, $b)}DE$c";
        Integer index = tmp.indexOf("$");
        System.out.print(index);
    }

    @Test
    public void testVarPrority(){
        Map<String,String> map = new HashMap<String,String>(){{
            put("k1","v1");
            put("k2","v2");
            put("k3","v3");
        }};
        Variables var1 = new Variables(map);
        map.put("k3","v4");
        Variables var2 = new Variables(map);
        System.out.println(JSON.toJSONString(var1));
        System.out.println(JSON.toJSONString(var2));
        System.out.println(JSON.toJSONString(Variables.extend2Variables(var1,var2)));

    }

    @Test
    public void TestReg(){
        String str = "123123{}";
        String str1 = "${func2()}";
        System.out.println(str1.replaceFirst("\\$","\\\\\\\\\\$"));
//                .replaceFirst("\\{","\\\\\\\\\\{")
//                .replaceFirst("\\}","\\\\\\\\\\}"));

        String tmp1 = str.replaceFirst("\\{\\}","\\${func2()}");
        System.out.println(tmp1);

        String tmp2 = str.replaceFirst("\\{\\}",
                str1.replaceFirst("\\$","\\\\\\$"));
        System.out.println(tmp2);
    }
}
