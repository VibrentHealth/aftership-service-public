package com.vibrent.aftership.exception;

import lombok.EqualsAndHashCode;

/**
 *  Non-Retriable Exception
 */
@EqualsAndHashCode(callSuper = false)
public class AfterShipNonRetriableException extends AfterShipException {

    private static final long serialVersionUID = 6754247008409261517L;

    public AfterShipNonRetriableException(String message, Integer errorCode) {
        super(message, errorCode);
    }

    public AfterShipNonRetriableException(String message, Integer errorCode, Throwable throwable) {
        super(message, errorCode, throwable);
    }
}
