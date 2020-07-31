package com.hrun;

import com.hrun.component.api.Api;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ApiTest {

    @Test
    public void testApi(){
        Map map = new HashMap<String,Object>();
        map.put("name","this is name");
    }

    @Test
    public void testreg(){
        String tmp = "12345$$abcde";
        tmp = tmp.replace("$$","$");
        System.out.println(tmp);
    }
}
