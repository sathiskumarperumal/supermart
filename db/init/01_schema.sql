-- ═══════════════════════════════════════════════════════════════════════════════
-- Supermart IoT – MySQL Schema
-- Matches Spring Boot 3.2.3 / Hibernate 6 entity mappings exactly.
--
-- Execution order matters – tables with FKs come after their referenced tables:
--   1. users
--   2. stores
--   3. technicians
--   4. equipment_units   (FK → stores)
--   5. iot_devices        (FK → equipment_units)
--   6. telemetry_records  (FK → iot_devices)
--   7. incidents          (FK → iot_devices)
--   8. technician_assignments (FK → incidents, technicians)
-- ═══════════════════════════════════════════════════════════════════════════════

USE supermartdb;

-- ─── 1. users ─────────────────────────────────────────────────────────────────
-- Maps to: com.supermart.iot.entity.User
-- id auto-generated via @GeneratedValue(IDENTITY)
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(10)  NOT NULL,          -- UserRole enum: ADMIN | MANAGER
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ─── 2. stores ────────────────────────────────────────────────────────────────
-- Maps to: com.supermart.iot.entity.Store
-- store_id is assigned manually (no @GeneratedValue on entity)
CREATE TABLE IF NOT EXISTS stores (
    store_id   BIGINT       NOT NULL,
    store_code VARCHAR(20)  NOT NULL,
    store_name VARCHAR(255) NOT NULL,
    address    VARCHAR(255) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    state      VARCHAR(2)   NOT NULL,
    zip_code   VARCHAR(10)  DEFAULT NULL,
    created_at DATETIME(6)  NOT NULL,             -- LocalDateTime maps to DATETIME(6) in Hibernate 6
    PRIMARY KEY (store_id),
    UNIQUE KEY uk_stores_store_code (store_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ─── 3. technicians ───────────────────────────────────────────────────────────
-- Maps to: com.supermart.iot.entity.Technician
-- technician_id is assigned manually (no @GeneratedValue on entity)
CREATE TABLE IF NOT EXISTS technicians (
    technician_id BIGINT       NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(20)  DEFAULT NULL,
    region        VARCHAR(255) NOT NULL,
    PRIMARY KEY (technician_id),
    UNIQUE KEY uk_technicians_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ─── 4. equipment_units ───────────────────────────────────────────────────────
-- Maps to: com.supermart.iot.entity.EquipmentUnit
-- unit_id is assigned manually (no @GeneratedValue on entity)
CREATE TABLE IF NOT EXISTS equipment_units (
    unit_id       BIGINT       NOT NULL,
    store_id      BIGINT       NOT NULL,
    unit_type     VARCHAR(20)  NOT NULL,          -- EquipmentType enum: FREEZER | REFRIGERATOR
    unit_name     VARCHAR(255) NOT NULL,
    location_desc VARCHAR(255) DEFAULT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (unit_id),
    CONSTRAINT fk_eq_units_store
        FOREIGN KEY (store_id) REFERENCES stores (store_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ─── 5. iot_devices ───────────────────────────────────────────────────────────
-- Maps to: com.supermart.iot.entity.IotDevice
-- device_id is assigned manually (no @GeneratedValue on entity)
-- unit_id is UNIQUE because IotDevice ↔ EquipmentUnit is a OneToOne relationship
CREATE TABLE IF NOT EXISTS iot_devices (
    device_id         BIGINT       NOT NULL,
    unit_id           BIGINT       NOT NULL,
    device_serial     VARCHAR(255) NOT NULL,
    device_key        VARCHAR(255) NOT NULL,
    min_temp_threshold DOUBLE      NOT NULL,
    max_temp_threshold DOUBLE      NOT NULL,
    status            VARCHAR(10)  NOT NULL,      -- DeviceStatus enum: ACTIVE | INACTIVE | FAULT
    last_seen_at      DATETIME(6)  DEFAULT NULL,
    PRIMARY KEY (device_id),
    UNIQUE KEY uk_iot_devices_unit_id       (unit_id),
    UNIQUE KEY uk_iot_devices_device_serial (device_serial),
    UNIQUE KEY uk_iot_devices_device_key    (device_key),
    CONSTRAINT fk_iot_devices_unit
        FOREIGN KEY (unit_id) REFERENCES equipment_units (unit_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ─── 6. telemetry_records ─────────────────────────────────────────────────────
-- Maps to: com.supermart.iot.entity.TelemetryRecord
-- telemetry_id auto-generated via @GeneratedValue(IDENTITY)
CREATE TABLE IF NOT EXISTS telemetry_records (
    telemetry_id BIGINT      NOT NULL AUTO_INCREMENT,
    device_id    BIGINT      NOT NULL,
    temperature  DOUBLE      NOT NULL,
    recorded_at  DATETIME(6) NOT NULL,
    is_alert     TINYINT(1)  NOT NULL,            -- Boolean maps to TINYINT(1): 0=false, 1=true
    PRIMARY KEY (telemetry_id),
    CONSTRAINT fk_telemetry_device
        FOREIGN KEY (device_id) REFERENCES iot_devices (device_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ─── 7. incidents ─────────────────────────────────────────────────────────────
-- Maps to: com.supermart.iot.entity.Incident
-- incident_id auto-generated via @GeneratedValue(IDENTITY)
CREATE TABLE IF NOT EXISTS incidents (
    incident_id   BIGINT      NOT NULL AUTO_INCREMENT,
    device_id     BIGINT      NOT NULL,
    incident_type VARCHAR(20) NOT NULL,           -- IncidentType enum: TEMP_EXCEEDED | DEVICE_FAULT
    status        VARCHAR(10) NOT NULL,           -- IncidentStatus enum: OPEN | ASSIGNED | RESOLVED
    description   TEXT        DEFAULT NULL,
    created_at    DATETIME(6) NOT NULL,
    resolved_at   DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (incident_id),
    CONSTRAINT fk_incidents_device
        FOREIGN KEY (device_id) REFERENCES iot_devices (device_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ─── 8. technician_assignments ────────────────────────────────────────────────
-- Maps to: com.supermart.iot.entity.TechnicianAssignment
-- assignment_id auto-generated via @GeneratedValue(IDENTITY)
CREATE TABLE IF NOT EXISTS technician_assignments (
    assignment_id BIGINT      NOT NULL AUTO_INCREMENT,
    incident_id   BIGINT      NOT NULL,
    technician_id BIGINT      NOT NULL,
    assigned_at   DATETIME(6) NOT NULL,
    notes         TEXT        DEFAULT NULL,
    PRIMARY KEY (assignment_id),
    CONSTRAINT fk_assignments_incident
        FOREIGN KEY (incident_id)   REFERENCES incidents    (incident_id),
    CONSTRAINT fk_assignments_technician
        FOREIGN KEY (technician_id) REFERENCES technicians  (technician_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
