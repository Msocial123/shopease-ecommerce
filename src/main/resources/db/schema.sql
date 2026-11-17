-- =====================================================================
--  ShopEase schema (MySQL 8.x / 9.x, InnoDB, utf8mb4)
--  Idempotent: safe to run on every start-up.
-- =====================================================================

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(120)  NOT NULL,
    email         VARCHAR(160)  NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(20)   NOT NULL DEFAULT 'CUSTOMER',
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    slug        VARCHAR(80)  NOT NULL,
    description VARCHAR(500),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT        NOT NULL,
    sku         VARCHAR(40)   NOT NULL,
    name        VARCHAR(160)  NOT NULL,
    brand       VARCHAR(80),
    description TEXT,
    price       DECIMAL(12,2) NOT NULL,
    mrp         DECIMAL(12,2) NOT NULL,
    stock       INT           NOT NULL DEFAULT 0,
    image_url   VARCHAR(500),
    rating      DECIMAL(2,1)  NOT NULL DEFAULT 0.0,
    featured    BOOLEAN       NOT NULL DEFAULT FALSE,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_products_stock CHECK (stock >= 0),
    INDEX idx_products_category (category_id),
    INDEX idx_products_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cart_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    product_id BIGINT    NOT NULL,
    quantity   INT       NOT NULL,
    added_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orders (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number   VARCHAR(30)   NOT NULL,
    user_id        BIGINT        NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    subtotal       DECIMAL(12,2) NOT NULL,
    tax            DECIMAL(12,2) NOT NULL,
    shipping       DECIMAL(12,2) NOT NULL,
    total          DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(20)   NOT NULL,
    ship_name      VARCHAR(120)  NOT NULL,
    ship_phone     VARCHAR(20)   NOT NULL,
    ship_line1     VARCHAR(200)  NOT NULL,
    ship_line2     VARCHAR(200),
    ship_city      VARCHAR(80)   NOT NULL,
    ship_state     VARCHAR(80)   NOT NULL,
    ship_zip       VARCHAR(12)   NOT NULL,
    notes          VARCHAR(500),
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_number UNIQUE (order_number),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    product_name VARCHAR(160)  NOT NULL,
    unit_price   DECIMAL(12,2) NOT NULL,
    quantity     INT           NOT NULL,
    line_total   DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT        NOT NULL,
    transaction_ref VARCHAR(40)   NOT NULL,
    method          VARCHAR(20)   NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    card_last4      VARCHAR(4),
    card_brand      VARCHAR(20),
    upi_id          VARCHAR(80),
    message         VARCHAR(255),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payments_txn UNIQUE (transaction_ref),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    INDEX idx_payments_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
