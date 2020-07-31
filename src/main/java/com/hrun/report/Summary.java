package com.hrun.report;

import com.google.common.base.Strings;
import com.hrun.component.Meta_data.Meta_data;
import com.hrun.lazyContent.LazyString;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hrun.App.version;

@Data
public class Summary {
    private Boolean success = true;

    private Map<String,Integer> testcasesStat = new HashMap<String,Integer>(){{
        put("total",(Integer)0);
        put("success",0);
        put("fail",0);
    }};

    private Map<String,Object> teststepsStat = new HashMap<>();

    private Map<String,Object> time = new HashMap<String,Object>();

    private Map<String,String> platform = new HashMap<String,String>();

    private List<Map> details = new ArrayList<>();

    public Summary(){
        platform.put("httprunner_version",version);
        platform.put("java_version","Java " + System.getProperty("java.version"));
        platform.put("platform",System.getProperty("os.name"));
    }

    public void stringify_summary(){
        for(int i=0;i<this.details.size();i++){
            if(Strings.isNullOrEmpty(((LazyString)this.details.get(i).get("name")).getRaw_value()))
                this.details.get(i).put("name",new LazyString(String.format("testcase %d",i)));

            for(Map record : (List<Map>)this.details.get(i).get("records")){
                Meta_data meta_datas = (Meta_data)record.get("meta_datas");
                __stringify_meta_datas(meta_datas);
                List<Meta_data> meta_datas_expanded = new ArrayList<>();
                __expand_meta_datas(meta_datas,meta_datas_expanded);
                record.put("meta_datas_expanded",meta_datas_expanded);
                record.put("response_time",__get_total_response_time(meta_datas_expanded));
            }
        }

    }

    public void __stringify_meta_datas(Meta_data meta_datas){
        meta_datas.stringify();

    }

    public void __expand_meta_datas(Meta_data meta_datas,List<Meta_data> meta_datas_expanded){
        meta_datas_expanded.add(meta_datas);
    }

    public Long __get_total_response_time(List<Meta_data> meta_datas_expanded){
        Long response_time = 0L;
        for(Meta_data meta_data : meta_datas_expanded){
            response_time += meta_data.getStat().getResponse_time_ms();
        }

        return response_time;
    }
}
