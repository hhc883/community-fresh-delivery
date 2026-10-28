CREATE TABLE IF NOT EXISTS dish (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(64), category_id BIGINT,
 price DECIMAL(10,2), image VARCHAR(255), description VARCHAR(255), status INT,
 is_groupon INT, groupon_price DECIMAL(10,2), groupon_size INT, groupon_valid_hours INT,
 stock INT, unit VARCHAR(32), origin VARCHAR(64), is_fresh INT,
 create_time TIMESTAMP, update_time TIMESTAMP, create_user BIGINT, update_user BIGINT
);
CREATE TABLE IF NOT EXISTS groupon (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, dish_id BIGINT, leader_user_id BIGINT,
 groupon_price DECIMAL(10,2), required_size INT, current_size INT, status INT,
 expire_time TIMESTAMP, complete_time TIMESTAMP, pickup_point_id BIGINT,
 create_time TIMESTAMP, update_time TIMESTAMP
);
CREATE TABLE IF NOT EXISTS groupon_member (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, groupon_id BIGINT, user_id BIGINT, order_id BIGINT,
 is_leader INT, number INT, amount DECIMAL(10,2), create_time TIMESTAMP,
 UNIQUE(groupon_id,user_id), UNIQUE(order_id)
);
ALTER TABLE pickup_point ADD COLUMN IF NOT EXISTS community VARCHAR(64);
ALTER TABLE pickup_point ADD COLUMN IF NOT EXISTS contact VARCHAR(64);
