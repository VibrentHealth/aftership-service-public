package com.vibrent.aftership.exception;

import lombok.Getter;

@Getter
public abstract class AfterShipException extends RuntimeException {

    private static final long serialVersionUID = -4790103003081986136L;

    protected final String message;
    protected final Integer errorCode;

    /** Constructs a new AfterShip exception with the specified detail message and error code.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     * @param errorCode Error code received from AfterShip service while creating tracking
     *                   later retrieval by the {@link #getErrorCode()} method.
     */
    protected AfterShipException(String message, Integer errorCode) {
        super(message);
        this.message = message;
        this.errorCode = errorCode;
    }

    /***
     * Constructs a new AfterShip exception with the specified cause, a detail message and error code
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).
     * @param errorCode Error code received from AfterShip service while creating tracking
     *                   later retrieval by the {@link #getErrorCode()} method.
     */
    protected AfterShipException(String message, Integer errorCode, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.errorCode = errorCode;
    }
}
