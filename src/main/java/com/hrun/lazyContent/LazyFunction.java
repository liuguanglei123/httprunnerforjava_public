package com.hrun.lazyContent;

import com.alibaba.fastjson.annotation.JSONField;
import com.hrun.Parse;
import com.hrun.Utils;
import com.hrun.component.common.Variables;
import com.hrun.exceptions.HrunExceptionFactory;
import lombok.Data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import com.hrun.builtin.Comparator;

@Data
public class LazyFunction implements Serializable {

    @JSONField(serialize=false)
    private Set check_variables_set;

    @JSONField(serialize=false)
    private Boolean cache_key;

    @JSONField(ordinal=1)
    private String func_name;

    @JSONField(serialize=false)
    private Method _func;

    //原始加载的参数列表
    @JSONField(ordinal=2)
    private List<LazyContent> _args = new ArrayList<LazyContent>();

    //to_value后的参数列表，是实际用来执行的参数
    @JSONField(ordinal=3)
    private List<Object> args = new ArrayList<Object>();

    @JSONField(serialize=false)
    private Boolean isBuiltInFunc = false;

    public LazyFunction(Map function_meta, Set check_variables_set){
        this.check_variables_set = Optional.ofNullable(check_variables_set).orElse(new HashSet());
        this.__parse(function_meta);
    }

    public void __parse(Map function_meta){
        this._func = Parse.get_mapping_function((String)function_meta.get("func_name"));
        this.func_name = this._func.getName();
        if(IsBuiltInFunc(this.func_name)){
            this.isBuiltInFunc = true;
        }
        //TODO:需要实现_args的解析
        if(((List)function_meta.get("argsList")).size() != 0) {
            for (Object _arg : (List) function_meta.get("argsList")) {
                LazyContent lazy_arg;
                if (_arg instanceof String) {
                    lazy_arg = new LazyString((String) _arg);
                    ((LazyString) lazy_arg).parse(this.check_variables_set);
                } else {
                    lazy_arg = new LazyContent(_arg);
                }
                this._args.add(lazy_arg);
            }
        }

        //TODO: 和python不同的是，python支持*args 和 **kwargs，分别表示list和map类型的参数，而且python是弱类型语言，这样就使得
        // debugtalk中方法调用的功能非常强大，可以支持不同类型的参数的传入
        // 虽然java中也有泛型的支持，但是因为并不熟悉，暂时不支持list和map类型的参数，只能传基本的数据类型，与之相对应的是
        // lazyFunction中，_args只能是List<Object>形式，且并不支持kwargs的传入。
        // 等功能完善后，再对此处进行改动
//        self._kwargs = prepare_lazy_data(
//                function_meta.get("kwargs", {}),
//                self.functions_mapping,
//                self.check_variables_set
//        )
    }

    public static Boolean IsBuiltInFunc(String func_name){
        if(Arrays.asList("equals,less_than".split(",")).contains(func_name)){
            return true;
        }
        return false;
    }

    public Object to_value(Variables variables_mapping) throws InvocationTargetException, IllegalAccessException {
        //TODO:
        if(this.isBuiltInFunc) {
            if(this.args.get(0) == null)
                HrunExceptionFactory.create("E0046");
            Comparator comparator = new Comparator(this.args.get(0));
            this._func.invoke(comparator,this.args.get(0),this.args.get(1));
        }

        return null;
    }

    public Object to_value() throws InvocationTargetException, IllegalAccessException {
        return to_value(null);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(check_variables_set);
        out.writeObject(cache_key);
        out.writeObject(func_name);
        out.writeObject(_args);
        out.writeObject(args);
        out.writeObject(isBuiltInFunc);
        Utils.tmpMethod = this._func;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.check_variables_set = (Set) in.readObject();
        this.cache_key = (Boolean) in.readObject();
        this.func_name = (String) in.readObject();
        this._args = (List<LazyContent>) in.readObject();
        this.args = (List<Object>) in.readObject();
        this.isBuiltInFunc = (Boolean) in.readObject();
        this._func = Utils.tmpMethod;
        Utils.tmpMethod = null;
    }


}
