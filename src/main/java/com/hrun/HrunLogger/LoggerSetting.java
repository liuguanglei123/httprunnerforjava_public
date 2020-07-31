package com.hrun.HrunLogger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

public class LoggerSetting {
    public static void setLogLevel(String log_level){
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.getLogger("com.hrun").setLevel(Level.valueOf(log_level));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("change log fail!");
        }
    }
}

//原httprunner中有设置日志文件等逻辑，需要后续实现

