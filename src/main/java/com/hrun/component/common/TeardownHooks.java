package com.hrun.component.common;

import java.io.Serializable;
import java.util.Set;

import com.hrun.component.intf.*;
import lombok.Data;

@Data
public class TeardownHooks implements Serializable, Parseable, Hooks {
    @Override
    public void parse(Set check_variables_set) {

    }

    @Override
    public Parseable to_value(Variables variables_mapping) {
        return null;
    }
}
