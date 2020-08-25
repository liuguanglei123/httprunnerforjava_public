package com.hrun.component.common;

import com.hrun.Utils;
import com.hrun.component.intf.Parseable;
import com.hrun.lazyContent.LazyContent;
import com.hrun.lazyContent.LazyString;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Data
public class Variables implements Serializable, Parseable {

    private HashMap<String, LazyContent> variables = new HashMap<String,LazyContent>();

    public Variables(){
    }

    public Variables(Map raw_variables) {
        for (Map.Entry<String, Object> entry : ((HashMap<String, Object>) raw_variables).entrySet()) {
            if (entry.getValue() instanceof String)
                variables.put(entry.getKey(), new LazyString(String.valueOf(entry.getValue())));
            else if(entry.getValue() instanceof LazyContent)
                variables.put(entry.getKey(),(LazyContent)entry.getValue());
            else
                variables.put(entry.getKey(), new LazyContent(entry.getValue()));
        }
    }

    public void setVariables(Map<String,Object> raw_variables){
        for(Map.Entry<String,Object> entry : raw_variables.entrySet()){
            if (entry.getValue() instanceof String)
                variables.put(entry.getKey(), new LazyString(String.valueOf(entry.getValue())));
            else
                variables.put(entry.getKey(), new LazyContent(entry.getValue()));
        }
    }

    @Override
    public void parse(Set check_variables_set) {
        if(this.variables == null || this.variables.size() == 0)
            return;

        for(LazyContent value : variables.values()){
            if(value instanceof LazyString)
                ((LazyString)value).parse(check_variables_set);
        }
    }

    @Override
    public Parseable to_value(Variables variables_mapping) {
        return null;
    }

    /**
     * 在原variables对象上进行扩展
     * @param another_var 另一个Var对象
     */
    public void extend(Variables another_var){
        Optional.ofNullable(another_var).ifPresent(
                a -> this.getVariables().putAll(a.getVariables())
        );
    }

    /**
     * 合并两个Variables对象然后返回合并后的Variables对象
     * @param var1
     * @param var2
     * var2 的优先级比 var1 高
     */
    public static Variables extend2Variables(Variables var1,Variables var2){
        Variables override_variables_mapping = Utils.deepcopy_dict(Optional.ofNullable(var1).orElse(new Variables()));
        override_variables_mapping.extend(Utils.deepcopy_dict(var2));

        return override_variables_mapping;
    }

    /**
     * 静态方法，判断Var对象中的variables变量是否为空，静态方法的目的是防止参数本身就是空的
     * @param variables
     */
    public static Boolean isNullOrEmpty(Variables variables){
        if(variables == null || variables.getVariables().isEmpty())
            return true;
        return false;
    }

    /**
     * 非静态方法，判断Var对象中的variables变量是否为空
     */
    public Boolean isEmpty(){
        if(this.variables.isEmpty())
            return true;
        return false;
    }

    public LazyContent getVariable(String name){
        //TODO: 获得variables中的某个value，但是如果value是$var形式，需要再次转化或者什么
        if(this.variables.get(name) != null){
            return this.variables.get(name);
        }else{
            return null;
        }
    }

    public Integer getSize(){
        return this.variables.size();
    }

}
