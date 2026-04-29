package com.smartcampus;

import com.smartcampus.exception.ExceptionMappers;
import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application bootstrap.
 *
 * Lifecycle note: By default JAX-RS creates a NEW resource instance per request.
 * That is why we use a shared singleton DataStore — instance fields on resource
 * classes would be reset each request, losing all in-memory data.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // Resources
        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        classes.add(SensorResource.class);

        // Exception Mappers
        classes.add(ExceptionMappers.RoomNotEmptyExceptionMapper.class);
        classes.add(ExceptionMappers.LinkedResourceNotFoundExceptionMapper.class);
        classes.add(ExceptionMappers.SensorUnavailableExceptionMapper.class);
        classes.add(ExceptionMappers.GlobalExceptionMapper.class);

        // Filters
        classes.add(LoggingFilter.class);

        return classes;
    }
}
