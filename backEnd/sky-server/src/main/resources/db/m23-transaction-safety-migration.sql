-- Incremental migration. Back up first; execute with the target database selected.
-- Duplicate historical order numbers/codes must be reviewed; no data is deleted here.
-- Remove invented metadata defaults for new rows; preserve all existing operator data.
ALTER TABLE dish ALTER COLUMN good_rate SET DEFAULT NULL;
ALTER TABLE pickup_point ALTER COLUMN distance SET DEFAULT NULL;
ALTER TABLE pickup_point ALTER COLUMN service_tags SET DEFAULT NULL;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='orders' AND column_name='client_request_id')=0,
 'ALTER TABLE orders ADD COLUMN client_request_id VARCHAR(64) NULL', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='orders' AND column_name='request_hash')=0,
 'ALTER TABLE orders ADD COLUMN request_hash CHAR(64) NULL', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='orders' AND index_name='uq_orders_user_request')=0,
 'ALTER TABLE orders ADD UNIQUE KEY uq_orders_user_request (user_id,client_request_id)', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='orders' AND index_name='uq_orders_number')=0,
 'ALTER TABLE orders ADD UNIQUE KEY uq_orders_number (number)', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='orders' AND index_name='uq_orders_pickup_code')=0,
 'ALTER TABLE orders ADD UNIQUE KEY uq_orders_pickup_code (pickup_code)', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
CREATE TABLE IF NOT EXISTS order_stock (
 order_id BIGINT NOT NULL,
 dish_id BIGINT NOT NULL,
 number INT NOT NULL,
 PRIMARY KEY(order_id,dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='groupon_member' AND index_name='idx_groupon_user')=0,
 'ALTER TABLE groupon_member ADD UNIQUE KEY idx_groupon_user (groupon_id,user_id)', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='groupon_member' AND index_name='uq_groupon_member_order')=0,
 'ALTER TABLE groupon_member ADD UNIQUE KEY uq_groupon_member_order (order_id)', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
