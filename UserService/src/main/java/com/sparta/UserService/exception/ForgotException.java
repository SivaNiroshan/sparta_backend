package com.sparta.UserService.exception;

public class ForgotException extends RuntimeException {
    
    public ForgotException(String message) {
        super(message);
    }
    
    public ForgotException(String message, Throwable cause) {
        super(message, cause);
    }
}

