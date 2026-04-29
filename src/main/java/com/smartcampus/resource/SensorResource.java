package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Part 3 - Sensor Operations & Linking
 * Part 4 - Sub-Resource Locator for readings
 *
 * GET    /api/v1/sensors              → list all sensors (optional ?type=CO2)
 * POST   /api/v1/sensors              → register a new sensor
 * GET    /api/v1/sensors/{sensorId}   → get a specific sensor
 * PUT    /api/v1/sensors/{sensorId}   → update sensor
 * DELETE /api/v1/sensors/{sensorId}   → remove sensor
 * ANY    /api/v1/sensors/{sensorId}/readings → delegated to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final Map<String, Sensor> sensors = DataStore.getInstance().getSensors();
    private final Map<String, Room>   rooms   = DataStore.getInstance().getRooms();

    // ----------------------------------------------------------------
    // GET /api/v1/sensors?type=CO2  →  list sensors, optionally filtered
    // ----------------------------------------------------------------
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = sensors.values();
        if (type != null && !type.isBlank()) {
            List<Sensor> filtered = all.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
            return Response.ok(filtered).build();
        }
        return Response.ok(all).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/sensors  →  register a sensor (validates roomId exists)
    // ----------------------------------------------------------------
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Sensor ID is required."))
                    .build();
        }
        if (sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(error("Sensor '" + sensor.getId() + "' already exists."))
                    .build();
        }
        // Validate that the referenced room exists → 422 if not
        if (sensor.getRoomId() == null || !rooms.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Room '" + sensor.getRoomId() + "' does not exist. Cannot register sensor.");
        }

        sensors.put(sensor.getId(), sensor);

        // Link sensor to the room
        rooms.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        // Initialise an empty reading list for this sensor
        DataStore.getInstance().getReadings().put(sensor.getId(), new ArrayList<>());

        return Response.created(URI.create("/api/v1/sensors/" + sensor.getId()))
                .entity(sensor)
                .build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}  →  fetch a single sensor
    // ----------------------------------------------------------------
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // ----------------------------------------------------------------
    // PUT /api/v1/sensors/{sensorId}  →  update sensor fields
    // ----------------------------------------------------------------
    @PUT
    @Path("/{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updated) {
        Sensor existing = sensors.get(sensorId);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error("Sensor '" + sensorId + "' not found."))
                    .build();
        }
        if (updated.getType()   != null) existing.setType(updated.getType());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        if (updated.getCurrentValue() != 0) existing.setCurrentValue(updated.getCurrentValue());
        return Response.ok(existing).build();
    }

    // ----------------------------------------------------------------
    // DELETE /api/v1/sensors/{sensorId}  →  remove sensor
    // ----------------------------------------------------------------
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            return Response.noContent().build(); // idempotent
        }
        // Unlink from room
        Room room = rooms.get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(sensorId);
        }
        sensors.remove(sensorId);
        DataStore.getInstance().getReadings().remove(sensorId);
        return Response.noContent().build();
    }

    // ----------------------------------------------------------------
    // Part 4 - Sub-Resource Locator
    // ANY /api/v1/sensors/{sensorId}/readings  →  SensorReadingResource
    // ----------------------------------------------------------------
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' not found.");
        }
        return new SensorReadingResource(sensorId);
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------
    private Map<String, String> error(String message) {
        return Map.of("error", message);
    }
}
