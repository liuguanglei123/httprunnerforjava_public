package com.hrun;

import com.hrun.component.common.*;
import com.hrun.component.intf.Parseable;
import com.hrun.lazyContent.LazyString;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
public class SessionContext {

    private Variables session_variables_mapping;

    private Variables test_variables_mapping;

    private Map<String,Object> test_variables_resquest_response = new HashMap<>();

    public Variables getTest_variables_mapping(){
        return this.test_variables_mapping;
    }

    public SessionContext(Variables variables){
        // TODO: self.session_variables_mapping = parser.parse_variables_mapping(variables_mapping)
        this.test_variables_mapping = new Variables();
        this.session_variables_mapping = new Variables();
        init_test_variables(null);
    }

    public void init_test_variables(Variables variables_mapping){
        variables_mapping = Optional.ofNullable(variables_mapping).orElse(new Variables());
        variables_mapping.extend(this.session_variables_mapping);
        //TODO: parsed_variables_mapping = parser.parse_variables_mapping(variables_mapping)

        this.test_variables_mapping = new Variables();
        //TODO: self.test_variables_mapping.update(parsed_variables_mapping)
        this.test_variables_mapping.extend(variables_mapping);
        this.test_variables_mapping.extend(this.session_variables_mapping);

    }

    public void update_test_variables(String variable_name, LazyString variable_value){
        this.test_variables_mapping.getVariables().put(variable_name,variable_value);
    }

    public void update_test_variables(String variable_name, Request request){
        this.test_variables_resquest_response.put("request",request);
    }

    public void update_test_variables(String variable_name, Response response){
        //TODO: 需要测试
        this.test_variables_resquest_response.put("response",response);
    }

    public void update_session_variables(Variables variables_mapping){
        this.session_variables_mapping.extend(variables_mapping);
        this.test_variables_mapping.extend(this.session_variables_mapping);
    }

    public LazyString eval_content(LazyString content){
        return content.to_value(this.test_variables_mapping);
    }

    public <T extends Parseable> T eval_content(T content){
        return (T)content.to_value(this.test_variables_mapping);
    }
}
