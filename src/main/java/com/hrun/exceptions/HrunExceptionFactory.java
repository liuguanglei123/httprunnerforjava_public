package com.hrun.exceptions;

public class HrunExceptionFactory {
    public static void create(String errorCode) {
        throw new HrunBizException(errorCode,
                ExceptionDefinitions.getExceptionDefinitions().getExceptionMessage(errorCode));
    }

    public static HrunBizException getException(String errorCode) {
        return new HrunBizException(errorCode,
                ExceptionDefinitions.getExceptionDefinitions().getExceptionMessage(errorCode));
    }
}
