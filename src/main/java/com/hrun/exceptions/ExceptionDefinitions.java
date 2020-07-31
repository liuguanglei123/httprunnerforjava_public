package com.hrun.exceptions;

import com.hrun.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class ExceptionDefinitions {
    // 单例
    private volatile static ExceptionDefinitions exceptionDefinitions;

    private Logger logger = LoggerFactory.getLogger(App.class);
//    private Resource location;
    private Properties exceptionDefinitionProps;

    /**
     * Creates a new instance of ExceptionDefinitions. Description
     */

    private ExceptionDefinitions() {
    }

//    public void setLocation(Resource location) {
//        this.location = location;
//    }

    private Properties getDefinitions() throws IOException {
        if (this.exceptionDefinitionProps == null) {
            this.exceptionDefinitionProps = new Properties();

            FileInputStream fileInputSream = new FileInputStream("src/main/resources/ErrorCode.properties");
            InputStreamReader reader = new InputStreamReader(fileInputSream,"UTF-8");
            this.exceptionDefinitionProps.load(reader);
        }
        return this.exceptionDefinitionProps;
    }



    public String getExceptionMessage(String errorCode) {

        String message = "";
        try {
            message = (String) getDefinitions().get(errorCode);
        } catch (IOException e) {
            this.logger.error(String.format("Error message for [code=%s] is not defined", new Object[] { errorCode }));
            e.printStackTrace();
        }
        if (message.equals("")) {
            message = String.format("系统错误[ErrorType = ERROR_MESSAGE_DEFINITION, ErrorCode=%s]", new Object[] { errorCode });
        }
        return message;
    }

    public static ExceptionDefinitions getExceptionDefinitions() {
        if (exceptionDefinitions == null) {
            synchronized (ExceptionDefinitions.class) {
                if (exceptionDefinitions == null) {
                    exceptionDefinitions = new ExceptionDefinitions();
                }
            }
        }
        return exceptionDefinitions;
    }
}
