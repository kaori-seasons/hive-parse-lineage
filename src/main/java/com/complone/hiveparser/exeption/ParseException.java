package com.complone.hiveparser.exeption;

public abstract class ParseException extends RuntimeException{
    
    private static final long serialVersionUID = -3044979409127407014L;
    
    public ParseException(final int errorCode, final String message) {
        super(createErrorMessage(errorCode, message));
    }
    
    private static String createErrorMessage(final int errorCode, final String message) {
        return String.format("DatabaseType-%05d: %s", errorCode, message);
    }
}
