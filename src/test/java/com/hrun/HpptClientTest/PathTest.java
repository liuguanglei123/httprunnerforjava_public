package com.hrun.HpptClientTest;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathTest {
    @Test
    public void test1(){
        Path path = Paths.get("C:\\Users\\liu\\Desktop\\HttpRunner\\hrunforjava\\src\\main\\java\\com\\hrun\\Utils.java");
        String absolute_path = path.toAbsolutePath().toString();
    }
}
