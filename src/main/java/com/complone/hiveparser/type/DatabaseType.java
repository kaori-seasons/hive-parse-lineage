package com.complone.hiveparser.type;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@SingletonSPI
public interface DatabaseType {
    
    /**
     * Get type.
     *
     * @return type
     */
    default String getType() {
        return "";
    }
    
    /**
     * Get type aliases.
     *
     * @return type aliases
     */
    default Collection<String> getTypeAliases() {
        return Collections.emptyList();
    }
    
}
