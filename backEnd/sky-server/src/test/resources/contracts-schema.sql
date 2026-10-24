CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    number VARCHAR(64) NOT NULL,
    status INT,
    user_id BIGINT,
    address_book_id BIGINT,
    order_time TIMESTAMP,
    checkout_time TIMESTAMP,
    pay_method INT,
    pay_status INT,
    amount DECIMAL(10,2),
    remark VARCHAR(255),
    user_name VARCHAR(64),
    phone VARCHAR(32),
    address VARCHAR(255),
    consignee VARCHAR(64),
    cancel_reason VARCHAR(255),
    rejection_reason VARCHAR(255),
    cancel_time TIMESTAMP,
    estimated_delivery_time TIMESTAMP,
    delivery_status INT,
    delivery_time TIMESTAMP,
    pack_amount INT,
    tableware_number INT,
    tableware_status INT,
    pickup_point_id BIGINT,
    pickup_point_name VARCHAR(64),
    is_groupon INT DEFAULT 0 NOT NULL,
    groupon_id BIGINT,
    pickup_code VARCHAR(16),
    pickup_time_slot VARCHAR(64),
    pickup_person_name VARCHAR(64),
    pickup_person_phone VARCHAR(32),
    verify_status INT DEFAULT 0 NOT NULL,
    verify_time TIMESTAMP
);

CREATE TABLE order_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64), image VARCHAR(255), order_id BIGINT,
    dish_id BIGINT, setmeal_id BIGINT, dish_flavor VARCHAR(255),
    number INT, amount DECIMAL(10,2)
);

CREATE TABLE pickup_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(64),
    address VARCHAR(255), phone VARCHAR(32), open_time VARCHAR(64), status INT
);

CREATE TABLE address_book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT,
    consignee VARCHAR(64), phone VARCHAR(32), sex VARCHAR(1),
    province_code VARCHAR(16), province_name VARCHAR(32),
    city_code VARCHAR(16), city_name VARCHAR(32),
    district_code VARCHAR(16), district_name VARCHAR(32),
    detail VARCHAR(255), label VARCHAR(32), is_default INT
);

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, openid VARCHAR(64), name VARCHAR(64),
    phone VARCHAR(32), sex VARCHAR(1), id_number VARCHAR(32), avatar VARCHAR(255),
    create_time TIMESTAMP, password VARCHAR(255), balance DECIMAL(10,2)
);
