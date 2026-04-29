package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Part 1 - Discovery Endpoint
 * GET /api/v1  →  API metadata + resource links (HATEOAS)
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("api", "Smart Campus Sensor & Room Management API");
        meta.put("version", "1.0.0");
        meta.put("contact", "admin@smartcampus.westminster.ac.uk");
        meta.put("description", "RESTful API for managing campus rooms and IoT sensors.");

        Map<String, String> links = new HashMap<>();
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        meta.put("resources", links);

        return Response.ok(meta).build();
    }
}
