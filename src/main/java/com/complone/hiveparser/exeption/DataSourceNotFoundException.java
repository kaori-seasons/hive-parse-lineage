package com.complone.hiveparser.exeption;

public class DataSourceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -3044979409127407027L;
    
    public DataSourceNotFoundException(final int errorCode, final String message) {
        super(createErrorMessage(errorCode, message));
    }
    
    private static String createErrorMessage(final int errorCode, final String message) {
        return String.format("DataSource Not Found -%05d: %s", errorCode, message);
    }
}
