INSERT INTO tariffs (service_type, vehicle_type, base_fare, per_km_fare, min_fare, active_from)
VALUES ('INSTANT',  'MOTOR',  8000, 2500, 10000, TIMESTAMP '2026-01-01 00:00:00 +07:00');
INSERT INTO tariffs (service_type, vehicle_type, base_fare, per_km_fare, min_fare, active_from)
VALUES ('INSTANT',  'CAR',   20000, 4500, 25000, TIMESTAMP '2026-01-01 00:00:00 +07:00');
INSERT INTO tariffs (service_type, vehicle_type, base_fare, per_km_fare, min_fare, active_from)
VALUES ('SAME_DAY', 'MOTOR',  6000, 1800,  8000, TIMESTAMP '2026-01-01 00:00:00 +07:00');
INSERT INTO tariffs (service_type, vehicle_type, base_fare, per_km_fare, min_fare, active_from)
VALUES ('SAME_DAY', 'CAR',   15000, 3500, 20000, TIMESTAMP '2026-01-01 00:00:00 +07:00');