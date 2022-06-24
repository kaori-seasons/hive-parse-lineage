package com.complone.hiveparser.type.dialect;

import com.complone.hiveparser.type.DatabaseType;

import java.util.Collection;
import java.util.Collections;

public class PostgreSQLDatabaseType implements DatabaseType {
    
    @Override
    public String getType() {
        return "PostgreSQL";
    }
    
    @Override
    public Collection<String> getTypeAliases() {
        return Collections.singleton("ALL_PRIVILEGES_PERMITTED");
    }
}
