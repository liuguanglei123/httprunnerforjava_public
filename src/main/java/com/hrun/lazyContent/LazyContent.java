package com.hrun.lazyContent;

import com.alibaba.fastjson.annotation.JSONField;
import com.hrun.component.common.Variables;
import lombok.Data;

import java.io.Serializable;
import java.util.regex.Matcher;

import static com.hrun.Parse.*;

@Data
public class LazyContent<T> implements Cloneable, Serializable {
    @JSONField(serialize=false)
    private T raw_value;  //未做任何处理的原值

    public LazyContent(T t){
        this.raw_value = t;
    }

    public T get(){
        return this.raw_value;
    }

    public Object to_value(){
        return this.raw_value;
    }

    public Object to_value(Variables variables_mapping){
        return this.raw_value;
    }

    public static Boolean is_var_or_func_exist(String content){
        int match_start_position = 0;

        match_start_position = content.indexOf("$", 0);
        if(match_start_position == -1)
            return false;


        while(match_start_position < content.length()){
            Matcher dollar_match = dolloar_regex_compile.matcher(content);
            if(dollar_match.find(match_start_position)){
                match_start_position = dollar_match.end();
                continue;
            }

            Matcher func_match = function_regex_compile.matcher(content);
            if(func_match.find(match_start_position)){
                return true;
            }

            Matcher var_match = variable_regex_compile.matcher(content);
            if(var_match.find(match_start_position)){
                return true;
            }
            return false;
        }

        return false;
    }

    /**
     * 获得LazyContent对象的原始值 raw_value
     * @return
     */
    public T getRaw_value(){
        return this.raw_value;
    }

    /**
     * 获得LazyContent对象的实际值，如果对象是LazyString类型，返回其实际值realvalue，否则返回raw_value
     * @return
     */
    public Object getEvalValue(){
        if(this instanceof LazyString)
            return ((LazyString)this).getEvalValue();
        else
            return getRaw_value();
    }
}
