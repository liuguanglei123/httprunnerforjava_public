package com.hrun;

import com.hrun.component.common.Parameters;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class testgen_cartesian_product {
    @Test
    public void test1(){
        Parameters param = new Parameters();

        List<Map<String,Object>> list1 = new ArrayList<Map<String,Object>>(){{
            add(new HashMap<String,Object>(){{
                put("a",1);
            }});
            add(new HashMap<String,Object>(){{
                put("a",2);
            }});
            add(new HashMap<String,Object>(){{
                put("a",3);
            }});
        }};

        List<Map<String,Object>> list2 = new ArrayList<Map<String,Object>>(){{
            add(new HashMap<String,Object>(){{
                put("b",11);
            }});
            add(new HashMap<String,Object>(){{
                put("b",22);
            }});
            add(new HashMap<String,Object>(){{
                put("b",33);
            }});
        }};

        List<Map<String,Object>> list3 = new ArrayList<Map<String,Object>>(){{
            add(new HashMap<String,Object>(){{
                put("c",111);
                put("d",1111);
            }});
            add(new HashMap<String,Object>(){{
                put("c",222);
                put("d",2222);
            }});
            add(new HashMap<String,Object>(){{
                put("c",333);
                put("d",3333);
            }});
        }};

        List<Map<String,Object>> list4 = new ArrayList<Map<String,Object>>(){{
            add(new HashMap<String,Object>(){{
                put("e",11111);
            }});
            add(new HashMap<String,Object>(){{
                put("e",22222);
            }});
            add(new HashMap<String,Object>(){{
                put("e",33333);
            }});
        }};

        List<List<Map<String,Object>>> args = new ArrayList<List<Map<String,Object>>>(){{
            add(list1);
            add(list2);
            add(list3);
            add(list4);
        }};

        List<Map<String,Object>> result = param.product(args);
        System.out.println(result);
        System.out.println(result.size());
    }
}
