package com.hrun.component.common;

import com.hrun.Utils;
import com.hrun.component.intf.Parseable;
import com.hrun.exceptions.HrunExceptionFactory;
import com.hrun.lazyContent.LazyContent;
import com.hrun.lazyContent.LazyString;
import lombok.Data;

import java.util.*;

@Data
public class Parameters implements Parseable {

    private HashMap<String, List> parameters = new HashMap<String,List>();

    private HashMap<String, LazyContent> raw_parameters = new HashMap<String,LazyContent>();

    public Parameters(){

    }
    /**
     *
     * @param raw_parameters Parameters节点的原始内容，可能是如下形式，暂时只做第一种
     *      parameters:
     *          uid: [101, 102, 103]
     *          device_sn: [TESTSUITE_X1, TESTSUITE_X2]
     * 或者 TODO：
     *      parameters:
     *          uid: $function($param1,$param2)
     *          device_sn: xxxx.excel
     *
     */
    public Parameters(Map raw_parameters) {
        for (Map.Entry<String, Object> entry : ((HashMap<String, Object>) raw_parameters).entrySet()) {
            if (entry.getValue() instanceof List)
                parameters.put(entry.getKey(), (List)entry.getValue());
            else if(entry.getValue() instanceof String)
                this.raw_parameters.put(entry.getKey(),(LazyContent)entry.getValue());
            else
                HrunExceptionFactory.create("E0042");
        }
    }

    @Override
    public void parse(Set check_variables_set) {
    }

    @Override
    public Parseable to_value(Variables variables_mapping) {
        return null;
    }

    public static Boolean isNullOrEmpty(Parameters parameters){
        if(parameters == null || parameters.getParameters().size() == 0)
            return true;
        else
            return false;
    }

    public List<Map<String,Object>> parse(Variables variables_mapping){
        List<List<Map<String,Object>>> parsed_parameters_list = new ArrayList<>();
        List<Map<String,Object>> parameter_content_list = new ArrayList<>();

        for(Map.Entry<String,List> entry : parameters.entrySet()) {
            String[] parameter_name_list = entry.getKey().split("-");

            parameter_content_list.clear();
            if (parameter_name_list.length == 1) {
                /*
                 * e.g. {"app_version": ["2.8.5", "2.8.6"]}
                 *       => [{"app_version": "2.8.5", "app_version": "2.8.6"}]
                 */
                for (Object parameter_item : entry.getValue()) {
                    if (parameter_item instanceof Collections) {
                        HrunExceptionFactory.create("E0044");
                    }
                    Map<String,Object> singleParameter = new HashMap<>();
                    singleParameter.put(parameter_name_list[0],parameter_item);
                    parameter_content_list.add(new HashMap<String,Object>(singleParameter));
                }
            } else {
                /*
                 * e.g. {"username-password": [["user1", "111111"], ["test2", "222222"]}
                 *       => [{"username": "user1", "password": "111111"}, {"username": "user2", "password": "222222"}]
                 */
                for(Object parameter_item : entry.getValue()) {
                    if (!(parameter_item instanceof List) || ((List)parameter_item).size() != parameter_name_list.length ) {
                        HrunExceptionFactory.create("E0044");
                    }
                    for(int i=0;i<parameter_name_list.length;i++){
                        Map<String,Object> singleParameter = new HashMap<>();
                        singleParameter.put(parameter_name_list[i],((List)parameter_item).get(i));
                        parameter_content_list.add(singleParameter);
                    }
                }
            }
            parsed_parameters_list.add(new ArrayList<>(parameter_content_list));
        }

        return gen_cartesian_product(parsed_parameters_list);
    }

    public List gen_cartesian_product(List<List<Map<String,Object>>> args){
        if(args == null || args.size() == 0)
            return new ArrayList<Object>();
        if(args.size() == 1)
            return args.get(0);

        return product(args);
    }

    public List<Map<String,Object>> product(List<List<Map<String,Object>>> args){
        if(args.size() > 2){
            List<List<Map<String,Object>>> subArgs = args.subList(1,args.size());
            List<Map<String,Object>> resolvedArgs = product(subArgs);

            List<List<Map<String,Object>>> newArgs = new ArrayList<List<Map<String,Object>>>(){{
                add(args.get(0));
                add(resolvedArgs);
            }};
            return product(newArgs);
        }else{
            List<Map<String,Object>> result = new ArrayList<>();
            for(int i=0;i<args.get(0).size();i++){
                for(int j=0;j<args.get(1).size();j++){
                    Map<String,Object> tmpParam = new HashMap<>();
                    tmpParam.putAll(args.get(0).get(i));
                    tmpParam.putAll(args.get(1).get(j));
                    result.add(tmpParam);
                }
            }
            return result;
        }
    }


}
