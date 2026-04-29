package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Part 2 - Room Management
 *
 * GET    /api/v1/rooms           → list all rooms
 * POST   /api/v1/rooms           → create a room
 * GET    /api/v1/rooms/{roomId}  → get single room
 * PUT    /api/v1/rooms/{roomId}  → update a room
 * DELETE /api/v1/rooms/{roomId}  → delete a room (fails if sensors assigned)
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final Map<String, Room> rooms = DataStore.getInstance().getRooms();

    // ----------------------------------------------------------------
    // GET /api/v1/rooms  →  list all rooms
    // ----------------------------------------------------------------
    @GET
    public Response getAllRooms() {
        Collection<Room> allRooms = rooms.values();
        return Response.ok(allRooms).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/rooms  →  create a new room
    // ----------------------------------------------------------------
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room ID is required."))
                    .build();
        }
        if (rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("Room with id '" + room.getId() + "' already exists."))
                    .build();
        }
        rooms.put(room.getId(), room);
        return Response.created(URI.create("/api/v1/rooms/" + room.getId()))
                .entity(room)
                .build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/rooms/{roomId}  →  get a specific room
    // ----------------------------------------------------------------
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ----------------------------------------------------------------
    // PUT /api/v1/rooms/{roomId}  →  update an existing room
    // ----------------------------------------------------------------
    @PUT
    @Path("/{roomId}")
    public Response updateRoom(@PathParam("roomId") String roomId, Room updated) {
        Room existing = rooms.get(roomId);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }
        if (updated.getName() != null)      existing.setName(updated.getName());
        if (updated.getCapacity() > 0)      existing.setCapacity(updated.getCapacity());
        return Response.ok(existing).build();
    }

    // ----------------------------------------------------------------
    // DELETE /api/v1/rooms/{roomId}  →  delete a room
    // Business rule: cannot delete if sensors are still assigned
    // ----------------------------------------------------------------
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            // Idempotent: already gone → 204 No Content
            return Response.noContent().build();
        }
        if (!room.getSensorIds().isEmpty()) {
            // Throws → caught by RoomNotEmptyExceptionMapper → 409
            throw new RoomNotEmptyException(
                    "Cannot delete room '" + roomId + "': it still has " +
                    room.getSensorIds().size() + " sensor(s) assigned.");
        }
        rooms.remove(roomId);
        return Response.noContent().build();
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------
    private Map<String, String> errorBody(String message) {
        return Map.of("error", message);
    }
}
