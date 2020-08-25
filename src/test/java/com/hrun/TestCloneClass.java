package com.hrun;

import lombok.Data;

import java.io.Serializable;

@Data
public class TestCloneClass implements Serializable {

    private String raw_str;

    public TestCloneClass(String string){
        raw_str = string;
    }
}
