CREATE TABLE IF NOT EXISTS user_account (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    price NUMERIC(19,2) NOT NULL,
    stock_quantity INTEGER NOT NULL CHECK (stock_quantity >= 0),
    image_url VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS cart (
    id BIGSERIAL PRIMARY KEY,
    customer_username VARCHAR(100) NOT NULL UNIQUE,
    CONSTRAINT fk_cart_user FOREIGN KEY (customer_username) REFERENCES user_account(username) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS customer_order (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL UNIQUE,
    customer_username VARCHAR(100) NOT NULL,
    shipping_address VARCHAR(255) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    total_price NUMERIC(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_customer_order_user FOREIGN KEY (customer_username) REFERENCES user_account(username)
);

CREATE TABLE IF NOT EXISTS cart_item (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS order_item (
    id BIGSERIAL PRIMARY KEY,
    customer_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19,2) NOT NULL,
    CONSTRAINT fk_order_item_order FOREIGN KEY (customer_order_id) REFERENCES customer_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS shipment (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    current_location VARCHAR(255) NOT NULL,
    CONSTRAINT fk_shipment_order FOREIGN KEY (order_id) REFERENCES customer_order(id) ON DELETE CASCADE
);
