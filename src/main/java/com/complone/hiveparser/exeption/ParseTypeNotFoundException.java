package com.complone.hiveparser.exeption;

public class ParseTypeNotFoundException extends ParseException{
    
    private static final long serialVersionUID = -3730257541332863236L;
    
    private static final int ERROR_CODE = 1;
    
    public ParseTypeNotFoundException(final Class<?> clazz) {
        super(ERROR_CODE, String.format("No implementation class load from SPI `%s`.", clazz.getName()));
    }
    
    public ParseTypeNotFoundException(final Class<?> clazz, final String type) {
        super(ERROR_CODE, String.format("No implementation class load from SPI `%s` with type `%s`.", clazz.getName(), type));
    }
    
}
