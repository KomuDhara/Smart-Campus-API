# Smart Campus Sensor & Room Management API

A JAX-RS RESTful API for managing campus Rooms and IoT Sensors, built with Jersey 2.x on Apache Tomcat 9.

**Base URL:** `http://localhost:8080/smartcampus-api/api/v1`

---

## API Design Overview

The API is structured around two primary resource collections — **Rooms** and **Sensors** — reflecting the physical layout of the campus. A room represents a physical space, and sensors are IoT devices deployed inside rooms. Sensor readings are managed as a sub-resource nested under each sensor, following a logical hierarchy:

```
/api/v1
├── /rooms
│   ├── GET    /              → List all rooms
│   ├── POST   /              → Create a room
│   ├── GET    /{roomId}      → Get a specific room
│   ├── PUT    /{roomId}      → Update a room
│   └── DELETE /{roomId}      → Delete a room (blocked if sensors assigned → 409)
└── /sensors
    ├── GET    /              → List all sensors (optional ?type= filter)
    ├── POST   /              → Register a sensor (validates roomId → 422 if missing)
    ├── GET    /{sensorId}    → Get a specific sensor
    ├── PUT    /{sensorId}    → Update a sensor
    ├── DELETE /{sensorId}    → Remove a sensor
    └── /{sensorId}/readings
        ├── GET  /            → List all readings for sensor
        └── POST /            → Add a reading (blocked if MAINTENANCE → 403)
```

**Key design decisions:**

- All data is stored in-memory using `ConcurrentHashMap` (no database).
- A singleton `DataStore` class manages shared state safely across request-scoped JAX-RS resource instances.
- All responses are `application/json`.
- Every error returns a structured JSON body — never a raw stack trace.

---

## Tech Stack

| Component       | Version                        |
|-----------------|-------------------------------|
| Apache Tomcat   | 9.0.x (Servlet 4.0 / javax.*) |
| Jersey (JAX-RS) | 2.41 (javax.ws.rs.*)          |
| Java            | 11                             |
| Build Tool      | Maven 3.x                     |

---

## Build & Deploy

### Prerequisites

- Java 11 — verify with `java -version`
- Apache Tomcat 9.0.x
- Maven 3.x

### 1. Build the WAR

```bash
mvn clean package
```

This produces `target/smartcampus-api.war`.

### 2. Deploy to Tomcat 9

Copy the WAR into Tomcat's webapps folder:

```
apache-tomcat-9.0.x/webapps/smartcampus-api.war
```

### 3. Start Tomcat

```bash
# Windows
bin\startup.bat

# Linux / macOS
bin/startup.sh
```

### 4. Verify the deployment

```bash
curl http://localhost:8080/smartcampus-api/api/v1
```

You should receive a JSON response with API metadata and resource links.

> **Troubleshooting:** If the app fails to start, check `logs/catalina.out` (Linux/macOS) or `logs/catalina.YYYY-MM-DD.log` (Windows). If redeploying, delete both `webapps/smartcampus-api.war` and the expanded `webapps/smartcampus-api/` folder before copying the new WAR.

---

## Sample curl Commands

### 1. Discovery — API metadata and resource links

```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1
```

### 2. Create a Room

```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-01","name":"Main Hall","capacity":200}'
```

### 3. Register a Sensor (linked to a room)

```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-999","type":"CO2","status":"ACTIVE","currentValue":380.0,"roomId":"HALL-01"}'
```

### 4. Add a Sensor Reading

```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors/CO2-999/readings \
  -H "Content-Type: application/json" \
  -d '{"value":412.5}'
```

### 5. Filter Sensors by Type

```bash
curl -X GET "http://localhost:8080/smartcampus-api/api/v1/sensors?type=CO2"
```

### 6. Get All Readings for a Sensor

```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1/sensors/CO2-999/readings
```

### 7. Update a Room

```bash
curl -X PUT http://localhost:8080/smartcampus-api/api/v1/rooms/HALL-01 \
  -H "Content-Type: application/json" \
  -d '{"name":"Main Hall Updated","capacity":250}'
```

### 8. Delete a Room — fails with 409 if sensors are still assigned

```bash
curl -X DELETE http://localhost:8080/smartcampus-api/api/v1/rooms/LIB-301
```

### 9. Delete a Sensor

```bash
curl -X DELETE http://localhost:8080/smartcampus-api/api/v1/sensors/CO2-999
```

### 10. Post Reading to a MAINTENANCE Sensor — returns 403 Forbidden

```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":5.0}'
```

---

## Report - Questions and Answers

### Part 1 — Service Architecture & Setup

#### Q1.1: JAX-RS Resource Class Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request scope). This means no state is shared between requests through instance fields of the resource class itself.

This design decision has direct consequences for in-memory data management. Because each request gets a fresh resource object, you cannot store application data (such as the rooms map or sensors map) as instance fields inside the resource class — those fields would be destroyed at the end of each request. To safely share state across all requests, this API uses a **singleton `DataStore` class** that is instantiated once and referenced statically. Internally, `ConcurrentHashMap` is used instead of a plain `HashMap` because multiple requests may execute concurrently on separate threads; `ConcurrentHashMap` provides thread-safe read and write operations without requiring manual synchronisation blocks, preventing data loss or race conditions under concurrent load.

#### Q1.2: HATEOAS and Hypermedia in REST

HATEOAS (Hypermedia as the Engine of Application State) is considered a hallmark of advanced RESTful design because it makes an API **self-describing and navigable at runtime**, rather than requiring clients to have prior knowledge of URL structures.

When a response includes hypermedia links (e.g., a newly created room response that embeds a link to its sensors endpoint), client developers do not need to hardcode or guess URLs — they simply follow the links provided. This brings several practical benefits:

- **Reduced coupling:** The server can change URL structures without breaking clients, because clients discover URLs dynamically from responses rather than from static documentation.
- **Discoverability:** A new client can start at the root `/api/v1` discovery endpoint and navigate the entire API by following links, much like browsing the web.
- **Self-documentation:** Responses communicate not just data but also the available next actions, reducing reliance on external documentation staying up to date.

In contrast, without hypermedia, clients must be rebuilt or reconfigured whenever the server's URL structure changes, creating tight coupling between client and server.

---

### Part 2 — Room Management

#### Q2.1: Returning Only IDs vs Full Room Objects

When returning a list of rooms, there is a trade-off between two approaches:

**Returning only IDs** (e.g., `["LIB-301", "HALL-01"]`):
- Minimal payload size — very efficient when the client only needs to reference rooms or display a count.
- However, the client must make **N additional HTTP requests** to fetch details for each room it wants to display, causing the "N+1 request problem" and significant latency under high load.

**Returning full room objects:**
- A single request delivers all the data the client needs to render a complete room list, eliminating follow-up requests.
- The payload is larger, which costs more bandwidth — especially significant on mobile networks or when returning thousands of rooms.
- However, for typical use cases (displaying a paginated list with name and capacity), returning full objects is almost always the better choice because it avoids the latency of multiple round-trips.

This API returns full room objects on the list endpoint, which is the standard practice for REST APIs unless the collection is extremely large, in which case pagination with summary fields is preferred.

#### Q2.2: Is DELETE Idempotent?

**Yes, the DELETE operation is idempotent in this implementation** — with an important nuance.

Idempotency means that making the same request multiple times produces the same server-side outcome as making it once. In this API:

- The **first** `DELETE /rooms/LIB-301` request finds the room, verifies it has no sensors, removes it from the data store, and returns `204 No Content`.
- The **second** `DELETE /rooms/LIB-301` request finds no room with that ID and returns `404 Not Found`.

The server state after both calls is identical — the room does not exist. The HTTP response code differs (204 vs 404), but the underlying resource state is the same. This is the standard and correct interpretation of idempotency: it refers to the **effect on server state**, not necessarily to receiving the same response code every time. HTTP/1.1 explicitly acknowledges that a 404 on a repeated DELETE is a valid and acceptable outcome.

---

### Part 3 — Sensor Operations & Linking

#### Q3.1: Consequences of a Mismatched `@Consumes` Content-Type

The `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method tells the JAX-RS runtime that this method **only accepts requests with a `Content-Type: application/json` header**.

If a client sends a request with a different content type — such as `text/plain` or `application/xml` — JAX-RS will reject the request **before it even reaches the method body**. The runtime returns an automatic **HTTP 415 Unsupported Media Type** response, indicating that the server cannot process the entity format supplied by the client.

This is enforced entirely by the framework's content negotiation mechanism. The resource method is never invoked, meaning no partial processing or data corruption can occur. This behaviour protects the API from malformed input and provides a clear, standards-compliant error signal to the client developer.

#### Q3.2: `@QueryParam` Filtering vs Path-Based Type Filtering

The `@QueryParam("type")` approach (`GET /api/v1/sensors?type=CO2`) is generally considered superior to embedding the type in the path (`GET /api/v1/sensors/type/CO2`) for the following reasons:

- **Semantic clarity:** A path segment like `/sensors/CO2` implies that `CO2` is a resource identifier — a unique entity. A query parameter correctly signals that `type=CO2` is a **filter criterion** being applied to the collection, not a resource address.
- **Optional by nature:** Query parameters are inherently optional. The same endpoint `GET /api/v1/sensors` works without the parameter and returns all sensors, making the API more flexible without needing a separate route.
- **Multiple filters:** Query parameters compose naturally — e.g., `?type=CO2&status=ACTIVE`. Path-based filtering becomes awkward and ambiguous with multiple criteria.
- **REST conventions:** REST best practice treats path segments as resource identifiers and query strings as filtering, sorting, or pagination modifiers. Following this convention makes the API immediately intuitive to developers familiar with REST.

---

### Part 4 — Deep Nesting with Sub-Resources

#### Q4.1: Architectural Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator pattern — where a method in `SensorResource` returns an instance of `SensorReadingResource` rather than handling readings inline — provides several important architectural benefits:

- **Separation of concerns:** Each resource class has a single, well-defined responsibility. `SensorResource` manages sensor CRUD, while `SensorReadingResource` exclusively handles reading logic. This mirrors the Single Responsibility Principle.
- **Manageability:** In a large API, placing every nested path handler in one controller class would result in an unwieldy class with hundreds of methods. Sub-resource locators allow the codebase to scale horizontally — new nested resources can be added as new classes without modifying existing ones.
- **Testability:** Smaller, focused classes are easier to unit test in isolation. `SensorReadingResource` can be tested independently by injecting a sensor context, without needing the full `SensorResource` setup.
- **Reusability:** A sub-resource class can potentially be reused in multiple parent contexts if the business logic warrants it.
- **Contextual injection:** When the locator method instantiates `SensorReadingResource`, it can pass the validated sensor object directly, ensuring the sub-resource always operates on a verified context rather than re-fetching and re-validating it.

---

### Part 5 — Advanced Error Handling, Exception Mapping & Logging

#### Q5.2: Why HTTP 422 is More Semantically Accurate Than 404

When a client POSTs a new sensor with a `roomId` that does not exist in the system, returning **HTTP 422 Unprocessable Entity** is more semantically accurate than 404 for the following reason:

- **404 Not Found** communicates that the **requested resource URL** could not be found on the server. It refers to the target of the HTTP request itself — in this case, `POST /api/v1/sensors` — which is a perfectly valid and existing endpoint.
- **422 Unprocessable Entity** communicates that the server understood the request, the URL is valid, the content type is correct, but the **payload contains a semantic error** — specifically, a reference to a resource (`roomId`) that does not exist.

The distinction is important: a 404 would mislead the client into thinking the endpoint itself is broken or wrong. A 422 correctly tells the client "your request arrived fine, but the data inside it references something that doesn't exist." This gives the client developer the precise signal needed to fix the payload, rather than the URL.

#### Q5.4: Cybersecurity Risks of Exposing Java Stack Traces

Exposing raw Java stack traces in API error responses poses significant security risks:

- **Technology fingerprinting:** A stack trace reveals the exact framework, library names, and version numbers in use (e.g., `org.glassfish.jersey`, `com.fasterxml.jackson`). An attacker can cross-reference these against known CVE databases to find exploitable vulnerabilities in those specific versions.
- **Internal architecture disclosure:** Package and class names (e.g., `com.smartcampus.data.DataStore`) reveal the internal structure of the application, making it easier to craft targeted attacks or understand the data model.
- **File path exposure:** Stack traces often include absolute file paths on the server (e.g., `/home/ubuntu/tomcat/webapps/...`), which can reveal the operating system, deployment directory structure, and server username.
- **Logic disclosure:** The sequence of method calls in a trace can expose business logic, reveal which code paths exist, and potentially indicate where injection points or boundary conditions may be exploitable.

A catch-all `ExceptionMapper<Throwable>` addresses all of these risks by intercepting any unexpected error and returning a generic `500 Internal Server Error` with a safe, non-revealing message, while logging the full trace server-side only for developers.

#### Q5.5: Why JAX-RS Filters Are Superior to Manual Logging Statements

Using a `ContainerRequestFilter` / `ContainerResponseFilter` for logging is architecturally superior to inserting `Logger.info()` calls inside every resource method because:

- **Cross-cutting concerns:** Logging applies to every endpoint equally. Filters implement this as a single concern in one place, rather than duplicating code across every method. This is the core principle behind Aspect-Oriented Programming.
- **Consistency:** Manual logging in individual methods is easily forgotten or inconsistently implemented — one developer may log differently than another. A filter guarantees uniform log entries for every request and response, regardless of which resource handles it.
- **Maintainability:** If the log format needs to change (e.g., adding a request ID or timestamp), a filter requires a single edit. Manual logging would require updating every resource method.
- **Separation of concerns:** Resource methods should focus solely on business logic. Mixing in logging statements makes methods harder to read and test. Filters keep the two concerns cleanly separated.
- **Non-invasive:** Filters operate transparently on the request/response lifecycle without requiring any changes to existing resource classes, making them easy to add or remove without risk.
