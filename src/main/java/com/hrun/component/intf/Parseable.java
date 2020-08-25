package com.hrun.component.intf;

import com.hrun.component.common.Variables;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

//继承此接口的所有类都必须实现parse方法，也就是可保存或解析lazyString对象的
public interface Parseable {
    public void parse(Set check_variables_set);
    public Parseable to_value(Variables variables_mapping) throws InvocationTargetException, IllegalAccessException;
}
