package com.vibrent.aftership.exception;


import lombok.EqualsAndHashCode;

/**
 *  Retriable Exception
 */
@EqualsAndHashCode(callSuper = false)
public class AfterShipRetriableException extends AfterShipException {

    private static final long serialVersionUID = 7424534805430853642L;

    public AfterShipRetriableException(String message, Integer errorCode) {
        super(message, errorCode);
    }

    public AfterShipRetriableException(String message, Integer errorCode, Throwable throwable) {
        super(message, errorCode, throwable);
    }
}
