CREATE TABLE category(id BIGINT PRIMARY KEY, name VARCHAR(64), type INT, status INT);
CREATE TABLE dish(
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(64) NOT NULL, category_id BIGINT NOT NULL,
 price DECIMAL(10,2) NOT NULL, image VARCHAR(255), description VARCHAR(255),
 status INT NOT NULL, stock INT NOT NULL, is_fresh INT NOT NULL, is_groupon INT NOT NULL,
 groupon_price DECIMAL(10,2), groupon_size INT, groupon_valid_hours INT,
 unit VARCHAR(64), origin VARCHAR(64), create_time TIMESTAMP, update_time TIMESTAMP,
 create_user BIGINT, update_user BIGINT
);
CREATE TABLE dish_flavor(id BIGINT AUTO_INCREMENT PRIMARY KEY, dish_id BIGINT, name VARCHAR(32) NOT NULL, value VARCHAR(128) NOT NULL);
CREATE TABLE setmeal_dish(id BIGINT AUTO_INCREMENT PRIMARY KEY, setmeal_id BIGINT, dish_id BIGINT, name VARCHAR(64), price DECIMAL(10,2), copies INT);
CREATE TABLE orders(id BIGINT PRIMARY KEY, status INT);
CREATE TABLE order_stock(order_id BIGINT NOT NULL, dish_id BIGINT NOT NULL, number INT NOT NULL, PRIMARY KEY(order_id,dish_id));
