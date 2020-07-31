package com.hrun.component.common;

import com.hrun.component.intf.Parseable;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
public class Export implements Serializable, Parseable {
    List<String> export = new ArrayList<>();

    public Export(){
    }

    public Export(List raw_output){
        for(int i=0;i<raw_output.size();i++){
            export.add((String)raw_output.get(i));
        }
    }


    @Override
    public void parse(Set check_variables_set) {

    }

    @Override
    public Parseable to_value(Variables variables_mapping) {
        return null;
    }
}
