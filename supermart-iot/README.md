# Supermart IoT Temperature Monitoring API

A **Spring Boot 3** REST API for Supermart's IoT Temperature Monitoring platform, managing ~3,000 stores, equipment units, IoT devices, real-time telemetry, incident tracking, and HVAC technician assignment workflows.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.3 |
| Language | Java 21 |
| Build | Maven |
| Database | H2 (in-memory, dev) / pluggable JPA |
| Security | Spring Security 6 + JWT (jjwt 0.12.5) |
| Validation | Jakarta Bean Validation |
| Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Mapping | MapStruct 1.5.5 |
| Utilities | Lombok |

---

## Project Structure

```
src/main/java/com/supermart/iot/
├── SupermartIotApplication.java        # Entry point
├── config/
│   ├── OpenApiConfig.java              # Swagger / SpringDoc config
│   └── SecurityConfig.java            # Spring Security 6 config
├── controller/
│   ├── AuthController.java            # POST /auth/login, /auth/refresh
│   ├── StoreController.java           # GET /stores, /stores/{id}, /stores/{id}/units
│   ├── DeviceController.java          # GET /devices, /devices/{id}, /devices/{id}/telemetry
│   ├── TelemetryController.java       # POST /telemetry
│   ├── DashboardController.java       # GET /dashboard/summary, /dashboard/alerts
│   ├── IncidentController.java        # CRUD /incidents + assign
│   └── TechnicianController.java      # GET /technicians
├── dto/
│   ├── request/                       # LoginRequest, TelemetryIngestRequest, etc.
│   └── response/                      # ApiResponse<T>, PagedResponse<T>, etc.
├── entity/                            # JPA entities
├── enums/                             # DeviceStatus, IncidentType, etc.
├── exception/                         # Custom exceptions + GlobalExceptionHandler
├── repository/                        # Spring Data JPA repositories
├── security/
│   ├── JwtService.java                # JWT generation & validation
│   ├── JwtAuthenticationFilter.java   # Bearer token filter
│   └── DeviceKeyAuthFilter.java       # X-Device-Key header filter
└── service/impl/                      # Business logic services
```

---

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.9+

### Run

```bash
# Clone / unzip the project
cd supermart-iot

# Build
mvn clean install -DskipTests

# Run
mvn spring-boot:run
```

The API starts at: `http://localhost:8080/api`

### Swagger UI
```
http://localhost:8080/api/swagger-ui.html
```

### H2 Console (dev)
```
http://localhost:8080/api/h2-console
JDBC URL: jdbc:h2:mem:supermartdb
Username: sa  |  Password: (empty)
```

---

## Authentication

### Dashboard Users (JWT)

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@supermart.com",
  "password": "S3cur3P@ss!"
}
```

Returns `accessToken` — pass as `Authorization: Bearer <token>` on all subsequent requests.

### IoT Devices (API Key)

```http
POST /api/telemetry
X-Device-Key: key-dev-9001
Content-Type: application/json

{
  "deviceId": 9001,
  "temperature": -10.2,
  "recordedAt": "2026-02-24T10:29:00"
}
```

---

## API Endpoints Summary

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/login` | Login & get JWT |
| `POST` | `/auth/refresh` | Refresh access token |
| `GET` | `/stores` | List stores (paginated, filterable) |
| `GET` | `/stores/{id}` | Get store by ID |
| `GET` | `/stores/{id}/units` | List equipment units in store |
| `GET` | `/devices` | List IoT devices (paginated) |
| `GET` | `/devices/{id}` | Get device detail |
| `GET` | `/devices/{id}/telemetry` | Telemetry history with date range |
| `POST` | `/telemetry` | Ingest telemetry (device auth) |
| `GET` | `/dashboard/summary` | KPI summary |
| `GET` | `/dashboard/alerts` | Active alerts/faults |
| `GET` | `/incidents` | List incidents |
| `POST` | `/incidents` | Create incident manually |
| `GET` | `/incidents/{id}` | Get incident detail |
| `PUT` | `/incidents/{id}/status` | Update incident status |
| `POST` | `/incidents/{id}/assign` | Assign HVAC technician |
| `GET` | `/technicians` | List technicians |

---

## Key Business Logic

### Automatic Incident Creation
When a device posts telemetry that breaches its configured `minTempThreshold` / `maxTempThreshold`, the API:
1. Marks `isAlert = true` on the telemetry record
2. Sets device status to `FAULT`
3. Auto-creates an `OPEN` incident (if no open incident already exists for that device)

### Rate Limiting
IoT devices are limited to **2 telemetry submissions per minute**. Exceeding this returns `HTTP 429`.

### Incident Status Flow
```
OPEN → ASSIGNED → RESOLVED
```
Reverse transitions from RESOLVED are rejected with `HTTP 400`.

---

## Seed Data

The app seeds the following on startup (H2 / `data.sql`):

- **3 stores**: Dallas TX, Houston TX, LA CA
- **5 equipment units**: freezers and refrigerators
- **5 IoT devices**: one in FAULT state (device 9001), one INACTIVE
- **3 technicians**: Texas South, Texas North, California South
- **Sample telemetry**: including one alert reading
- **1 open incident**: TEMP_EXCEEDED for device 9001

---

## Configuration

Key properties in `application.properties`:

```properties
app.jwt.secret=...                          # Base64 HMAC-SHA key
app.jwt.access-token-expiration-ms=3600000  # 1 hour
app.jwt.refresh-token-expiration-ms=86400000 # 24 hours
app.telemetry.rate-limit-per-minute=2
```
