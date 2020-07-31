package com.hrun.HrunArgsParse;

import com.beust.jcommander.IStringConverter;

import java.util.Arrays;
import java.util.List;

public class ArgsParseListConverter implements IStringConverter<List<String>> {
    @Override
    public List<String> convert(String args){
        List<String> result = Arrays.asList(args.split(","));
        return result;
    }
}
