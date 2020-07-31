package com.hrun.builtin;

import com.hrun.exceptions.HrunExceptionFactory;

public class Comparator<T> {

    private Class cls;

    public Comparator(T t1){
        cls = t1.getClass();
    }

    public void equals(T check_value,T expect_value){
        assert check_value.equals(expect_value);
    }

    public void less_than(T check_value,T expect_value){
        if(check_value instanceof Integer){
            assert (Integer)check_value < (Integer)expect_value;
        }else if(check_value instanceof Double){
            //TODO: int 和 double 有没有大于小于的比较？
            assert (Double)check_value < (Double)expect_value;
        }else {
            HrunExceptionFactory.create("E0023");
        }
    }


}
