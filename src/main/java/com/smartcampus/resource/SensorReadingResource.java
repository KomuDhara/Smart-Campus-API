package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Part 4 - Historical Data Management (Sub-Resource)
 *
 * GET  /api/v1/sensors/{sensorId}/readings      → fetch all readings
 * POST /api/v1/sensors/{sensorId}/readings      → append a new reading
 * GET  /api/v1/sensors/{sensorId}/readings/{id} → fetch a specific reading
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final Map<String, Sensor>       sensors  = DataStore.getInstance().getSensors();
    private final Map<String, List<SensorReading>> readings = DataStore.getInstance().getReadings();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings
    // ----------------------------------------------------------------
    @GET
    public Response getAllReadings() {
        List<SensorReading> history = readings.getOrDefault(sensorId, List.of());
        return Response.ok(history).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/sensors/{sensorId}/readings
    // Blocked if sensor is in MAINTENANCE status → 403
    // Side-effect: updates sensor.currentValue
    // ----------------------------------------------------------------
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = sensors.get(sensorId);

        // State constraint: MAINTENANCE sensors cannot receive new readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is under MAINTENANCE and cannot accept readings.");
        }

        // Assign metadata if not supplied by client
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(java.util.UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Store reading
        readings.computeIfAbsent(sensorId, k -> new java.util.ArrayList<>()).add(reading);

        // Side-effect: update parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        return Response.created(URI.create("/api/v1/sensors/" + sensorId + "/readings/" + reading.getId()))
                .entity(reading)
                .build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings/{readingId}
    // ----------------------------------------------------------------
    @GET
    @Path("/{readingId}")
    public Response getReading(@PathParam("readingId") String readingId) {
        List<SensorReading> history = readings.getOrDefault(sensorId, List.of());
        return history.stream()
                .filter(r -> r.getId().equals(readingId))
                .findFirst()
                .map(r -> Response.ok(r).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Reading '" + readingId + "' not found."))
                        .build());
    }
}
