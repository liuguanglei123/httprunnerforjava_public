package com.hrun.component.common;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.hrun.component.intf.*;
import com.hrun.lazyContent.LazyContent;
import com.hrun.lazyContent.LazyFunction;
import lombok.Data;

@Data
public class TeardownHooks implements Serializable, Hooks {

    private List<LazyFunction> teardown_hooks = new ArrayList<>();

    public TeardownHooks(List<String> raw_setup_hooks){
        raw_setup_hooks.stream().forEach(e -> this.teardown_hooks.add(new LazyFunction(e)));
    }

    @Override
    public void parse(Set check_variables_set) {
        if(this.teardown_hooks == null || this.teardown_hooks.size() == 0)
            return;

        for(LazyFunction value : teardown_hooks){
            value.parse(check_variables_set);
        }
    }

    @Override
    public Parseable to_value(Variables variables_mapping) throws InvocationTargetException, IllegalAccessException {
        for(LazyFunction each : teardown_hooks)
            each.to_value(variables_mapping);
        return null;
    }

    public void extend(TeardownHooks teardownHooks){
        if(!teardownHooks.isEmpty()){
            this.getTeardown_hooks().addAll(teardownHooks.getTeardown_hooks());
        }
    }

    public Boolean isEmpty(){
        if(this.getTeardown_hooks().size() == 0){
            return true;
        }
        return false;
    }
}
