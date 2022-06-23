package com.complone.hiveparser.type;

import com.complone.hiveparser.exeption.ParseTypeNotFoundException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.Properties;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeSPIRegistry {
    
    public static Optional<DatabaseType> findRegisteredService(final Class<DatabaseType> spiClass, final String type){
        for (DatabaseType each : DatabaseTypeServiceLoader.getServiceInstances(spiClass)) {
            if (matchesType(type, each)) {
                return Optional.of(each);
            }
        }
        return Optional.empty();
    }
    
    private static boolean matchesType(final String type, final DatabaseType instance) {
        return instance.getType().equalsIgnoreCase(type) || instance.getTypeAliases().contains(type);
    }
    
    private static Properties convertToStringTypedProperties(final Properties props) {
        if (null == props) {
            return new Properties();
        }
        Properties result = new Properties();
        props.forEach((key, value) -> result.setProperty(key.toString(), null == value ? null : value.toString()));
        return result;
    }
    
    
    /**
     * Get registered service.
     *
     * @param spiClass typed SPI class
     * @param type type
     * @return registered service
     */
    public static DatabaseType getRegisteredService(final Class<DatabaseType> spiClass, final String type) {
        Optional<DatabaseType> result = findRegisteredService(spiClass, type);
        if (result.isPresent()) {
            return result.get();
        }
        throw new ParseTypeNotFoundException(spiClass, type);
    }
}
