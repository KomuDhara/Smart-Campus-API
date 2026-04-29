package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * All exception mappers in one place.
 * Each inner class is public so SmartCampusApplication can register them explicitly.
 */
public class ExceptionMappers {

    // =========================================================================
    // Part 5.1 – 409 Conflict: room still has sensors assigned
    // =========================================================================
    @Provider
    public static class RoomNotEmptyExceptionMapper
            implements ExceptionMapper<RoomNotEmptyException> {

        @Override
        public Response toResponse(RoomNotEmptyException ex) {
            return Response.status(Response.Status.CONFLICT)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(body(409, "Conflict", ex.getMessage()))
                    .build();
        }
    }

    // =========================================================================
    // Part 5.2 – 422 Unprocessable Entity: roomId reference does not exist
    // =========================================================================
    @Provider
    public static class LinkedResourceNotFoundExceptionMapper
            implements ExceptionMapper<LinkedResourceNotFoundException> {

        @Override
        public Response toResponse(LinkedResourceNotFoundException ex) {
            return Response.status(422)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(body(422, "Unprocessable Entity", ex.getMessage()))
                    .build();
        }
    }

    // =========================================================================
    // Part 5.3 – 403 Forbidden: sensor is under MAINTENANCE
    // =========================================================================
    @Provider
    public static class SensorUnavailableExceptionMapper
            implements ExceptionMapper<SensorUnavailableException> {

        @Override
        public Response toResponse(SensorUnavailableException ex) {
            return Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(body(403, "Forbidden", ex.getMessage()))
                    .build();
        }
    }

    // =========================================================================
    // Part 5.4 – 500 Global safety net: catch any unexpected Throwable
    // =========================================================================
    @Provider
    public static class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

        private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class.getName());

        @Override
        public Response toResponse(Throwable ex) {
            LOG.severe("Unhandled exception: " + ex.getClass().getName() + " – " + ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(body(500, "Internal Server Error",
                            "An unexpected error occurred. Please contact support."))
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // Shared helper – use LinkedHashMap so key order is stable in JSON output
    // -------------------------------------------------------------------------
    private static Map<String, Object> body(int status, String error, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", status);
        map.put("error", error);
        map.put("message", message);
        return map;
    }
}
