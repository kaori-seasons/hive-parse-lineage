package com.complone.hiveparser.type;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DatabaseTypeFactory {
    
    static {
        DatabaseTypeServiceLoader.register(DatabaseType.class);
    }
    
    public static DatabaseType getInstance(final String name){
        return TypeSPIRegistry.getRegisteredService(DatabaseType.class, name);
    }
}
