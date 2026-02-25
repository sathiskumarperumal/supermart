-- ── Users ────────────────────────────────────────────────────────────────────
-- Password: S3cur3P@ss!  (BCrypt encoded)
INSERT INTO users (id, email, password_hash, role) VALUES
(1, 'admin@supermart.com', '$2b$12$OaFdvg7Az6p3ntY2faZ0keICVz/9og3WkBD2HQoyEeCJMhz55oW/2', 'ADMIN'),
(2, 'manager@supermart.com', '$2b$12$OaFdvg7Az6p3ntY2faZ0keICVz/9og3WkBD2HQoyEeCJMhz55oW/2', 'MANAGER');

-- ── Stores ───────────────────────────────────────────────────────────────────
INSERT INTO stores (store_id, store_code, store_name, address, city, state, zip_code, created_at) VALUES
(1001, 'TX-DAL-042', 'Supermart Dallas North', '1234 Commerce Blvd', 'Dallas', 'TX', '75201', '2024-01-15T08:00:00'),
(1002, 'TX-HOU-011', 'Supermart Houston West', '500 Energy Pkwy', 'Houston', 'TX', '77001', '2024-02-10T08:00:00'),
(1003, 'CA-LAX-007', 'Supermart LA Downtown', '200 Sunset Blvd', 'Los Angeles', 'CA', '90028', '2024-03-01T08:00:00');

-- ── Equipment Units ───────────────────────────────────────────────────────────
INSERT INTO equipment_units (unit_id, store_id, unit_type, unit_name, location_desc, created_at) VALUES
(501, 1001, 'FREEZER', 'Freezer-Aisle-3', 'Aisle 3, near dairy section', '2024-01-15T08:00:00'),
(502, 1001, 'REFRIGERATOR', 'Fridge-Aisle-5', 'Aisle 5, beverages', '2024-01-15T08:00:00'),
(503, 1001, 'FREEZER', 'Freezer-Aisle-7', 'Aisle 7, frozen foods', '2024-01-15T08:00:00'),
(504, 1002, 'FREEZER', 'Freezer-Aisle-1', 'Aisle 1, ice cream', '2024-02-10T08:00:00'),
(505, 1003, 'REFRIGERATOR', 'Fridge-Aisle-2', 'Aisle 2, produce', '2024-03-01T08:00:00');

-- ── IoT Devices ───────────────────────────────────────────────────────────────
INSERT INTO iot_devices (device_id, unit_id, device_serial, device_key, min_temp_threshold, max_temp_threshold, status, last_seen_at) VALUES
(9001, 501, 'DEV-2024-TX-09001', 'key-dev-9001', -25.0, -15.0, 'FAULT', '2026-02-24T10:29:00'),
(9002, 502, 'DEV-2024-TX-09002', 'key-dev-9002', 2.0, 8.0, 'ACTIVE', '2026-02-24T10:29:00'),
(9003, 503, 'DEV-2024-TX-09003', 'key-dev-9003', -25.0, -15.0, 'ACTIVE', '2026-02-24T10:29:00'),
(9004, 504, 'DEV-2024-TX-09004', 'key-dev-9004', -25.0, -15.0, 'ACTIVE', '2026-02-24T10:29:00'),
(9005, 505, 'DEV-2024-CA-09005', 'key-dev-9005', 2.0, 8.0, 'INACTIVE', '2026-02-24T09:00:00');

-- ── Technicians ───────────────────────────────────────────────────────────────
INSERT INTO technicians (technician_id, full_name, email, phone, region) VALUES
(55, 'Carlos Rivera', 'c.rivera@hvac-partners.com', '+1-214-555-0192', 'Texas South'),
(56, 'Maria Santos', 'm.santos@hvac-partners.com', '+1-214-555-0201', 'Texas North'),
(57, 'James Park', 'j.park@hvac-partners.com', '+1-310-555-0345', 'California South');

-- ── Telemetry Records ─────────────────────────────────────────────────────────
INSERT INTO telemetry_records (telemetry_id, device_id, temperature, recorded_at, is_alert) VALUES
(78234441, 9001, -14.8, '2026-02-24T10:28:00', false),
(78234501, 9001, -10.2, '2026-02-24T10:29:00', true),
(78234442, 9002, 5.1,  '2026-02-24T10:28:00', false),
(78234502, 9002, 5.3,  '2026-02-24T10:29:00', false);

-- ── Incidents ─────────────────────────────────────────────────────────────────
INSERT INTO incidents (incident_id, device_id, incident_type, status, description, created_at, resolved_at) VALUES
(3301, 9001, 'TEMP_EXCEEDED', 'OPEN', 'Temperature exceeded max threshold of -15.0°C. Recorded: -10.2°C', '2026-02-24T10:31:00', null);
