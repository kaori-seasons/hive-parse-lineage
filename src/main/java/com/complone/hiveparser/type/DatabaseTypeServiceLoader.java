package com.complone.hiveparser.type;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DatabaseTypeServiceLoader {
    
    private static final Map<Class<?>, Collection<Object>> SERVICES = new ConcurrentHashMap<>();
    
    
    public static void register(final Class<?> serviceInterface){
        if (!SERVICES.containsKey(serviceInterface)){
            SERVICES.put(serviceInterface, load(serviceInterface));
        }
    }
    
    private static <T> Collection<Object> load(final Class<T> serviceInterface) {
        Collection<Object> result = new LinkedList<>();
        for (T each : ServiceLoader.load(serviceInterface)) {
            result.add(each);
        }
        return result;
    }
    
    /**
     * Get service instances.
     *
     * @param serviceInterface service interface
     * @param <T> type of service
     * @return service instances
     */
    public static <T> Collection<T> getServiceInstances(final Class<T> serviceInterface) {
        return null == serviceInterface.getAnnotation(SingletonSPI.class) ? createNewServiceInstances(serviceInterface) : getSingletonServiceInstances(serviceInterface);
    }
    
    @SneakyThrows(ReflectiveOperationException.class)
    @SuppressWarnings("unchecked")
    private static <T> Collection<T> createNewServiceInstances(final Class<T> serviceInterface) {
        if (!SERVICES.containsKey(serviceInterface)) {
            return Collections.emptyList();
        }
        Collection<Object> services = SERVICES.get(serviceInterface);
        if (services.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<T> result = new LinkedList<>();
        for (Object each : services) {
            result.add((T) each.getClass().getDeclaredConstructor().newInstance());
        }
        return result;
    }
    
    private static <T> Collection<T> getSingletonServiceInstances(final Class<T> serviceInterface) {
        return (Collection<T>) SERVICES.getOrDefault(serviceInterface, Collections.emptyList());
    }
    
}
