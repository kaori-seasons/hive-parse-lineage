package com.complone.hiveparser.exeption;

public class SQLParseException extends RuntimeException{
    public SQLParseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SQLParseException(String message) {
        super(message);
    }
}
