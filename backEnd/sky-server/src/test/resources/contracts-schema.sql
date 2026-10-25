CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    number VARCHAR(64) NOT NULL,
    client_request_id VARCHAR(64), request_hash VARCHAR(64),
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
    verify_time TIMESTAMP,
    UNIQUE(user_id, client_request_id), UNIQUE(number), UNIQUE(pickup_code)
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

CREATE TABLE shopping_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(64), image VARCHAR(255),
    user_id BIGINT, dish_id BIGINT, setmeal_id BIGINT, dish_flavor VARCHAR(255),
    number INT, amount DECIMAL(10,2), create_time TIMESTAMP
);

CREATE TABLE dish (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(64), category_id BIGINT,
    price DECIMAL(10,2), image VARCHAR(255), description VARCHAR(255), status INT,
    stock INT, is_groupon INT, groupon_price DECIMAL(10,2), groupon_size INT,
    groupon_valid_hours INT, unit VARCHAR(64), origin VARCHAR(64), is_fresh INT,
    sold_count INT, promo_tag VARCHAR(64), seckill INT, seckill_price DECIMAL(10,2),
    tags VARCHAR(255), limit_buy INT, good_rate VARCHAR(32),
    create_time TIMESTAMP, update_time TIMESTAMP, create_user BIGINT, update_user BIGINT
);

CREATE TABLE order_stock (
    order_id BIGINT NOT NULL, dish_id BIGINT NOT NULL, number INT NOT NULL,
    PRIMARY KEY(order_id, dish_id)
);
CREATE TABLE setmeal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, category_id BIGINT, name VARCHAR(64), price DECIMAL(10,2),
    status INT, description VARCHAR(255), image VARCHAR(255), create_time TIMESTAMP, update_time TIMESTAMP,
    create_user BIGINT, update_user BIGINT
);
CREATE TABLE setmeal_dish (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, setmeal_id BIGINT, dish_id BIGINT,
    name VARCHAR(64), price DECIMAL(10,2), copies INT
);
