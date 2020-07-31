package com.hrun.component.Meta_data;

import com.hrun.Utils;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Meta_data {

    private String name;

    private List<ResultData> data = new ArrayList<ResultData>();;

    private ResultStat stat ;

    private Map<String,Object> validators = new HashMap<>();

    public Meta_data(){
        this.stat = new ResultStat();
        data.add(new ResultData());
    }

    public void stringify(){
        List<ResultData> data_list = this.data;
        for(ResultData data : data_list){
            data.stringify();
        }
    }
}
