package com.hrun.component.common;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.hrun.component.intf.*;
import com.hrun.lazyContent.LazyContent;
import com.hrun.lazyContent.LazyFunction;
import com.hrun.lazyContent.LazyString;
import lombok.Data;

@Data
public class SetupHooks implements Serializable, Hooks {

    private List<LazyFunction> setup_hooks = new ArrayList<>();

    public SetupHooks(List<String> raw_setup_hooks){
        raw_setup_hooks.stream().forEach(e -> this.setup_hooks.add(new LazyFunction(e)));
    }

    @Override
    public void parse(Set check_variables_set) {
        if(this.setup_hooks == null || this.setup_hooks.size() == 0)
            return;

        for(LazyFunction value : setup_hooks){
            value.parse(check_variables_set);
        }
    }

    @Override
    public Parseable to_value(Variables variables_mapping) throws InvocationTargetException, IllegalAccessException {
        for(LazyFunction each : setup_hooks)
            each.to_value(variables_mapping);
        return null;
    }

    public void extend(SetupHooks setupHooks){
        if(setupHooks != null)
            this.setup_hooks.addAll(setupHooks.getSetup_hooks());
    }

    public Boolean isEmpty(){
        if(this.getSetup_hooks().size() == 0){
            return true;
        }
        return false;
    }
}
