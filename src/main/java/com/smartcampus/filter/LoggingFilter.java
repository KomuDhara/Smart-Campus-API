package com.smartcampus.filter;

import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 - API Request & Response Logging Filter
 * Implements both ContainerRequestFilter and ContainerResponseFilter
 * to log every incoming request and every outgoing response status.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("[REQUEST]  %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format("[RESPONSE] %s %s → HTTP %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()));
    }
}
