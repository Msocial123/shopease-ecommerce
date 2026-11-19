# ShopEase - E-Commerce on Tomcat 11

A complete online store built with the classic Java web stack, packaged as a WAR for **Apache Tomcat 11**.

| Layer | Technology |
|---|---|
| Container | Apache Tomcat 11 (Jakarta EE 11, **Servlet 6.1**, JSP 4.0, EL 6.0) |
| Web framework | Spring MVC 6.2 (Java config, no Spring Boot) |
| Views | JSP + JSTL 3.0 (`jakarta.tags.*`) + custom tag files |
| Plain servlets | `HealthServlet`, `ProductImageServlet`, filters and a listener (all `jakarta.servlet.*`) |
| Data | MySQL 8/9 via Spring `JdbcTemplate` + HikariCP |
| Front end | HTML5, CSS3 (responsive, no framework), vanilla JavaScript (AJAX cart) |
| Build | Maven 3, Java 17+ |

## Features

**Storefront**
- Home page with categories, featured deals and new arrivals
- Catalogue with keyword search, category filter, price range, in-stock filter, 6 sort orders and pagination
- Product pages with discount and stock indicators and related products
- Cart: a session cart for guests, kept in the database for logged-in users, and merged into the user's cart at login. AJAX add-to-cart, quantity steppers, free-shipping progress
- Checkout: shipping address (pre-filled from your last order), GST and shipping calculation
- Payment: card (Luhn, expiry and CVV checks, card brand detection), UPI, net banking and cash on delivery, through a **simulated gateway**
- Orders: history, detail with a progress tracker, payment records, and cancellation before shipping (stock goes back on the shelf and paid orders are refunded)
- Accounts: register, log in, log out, edit profile, change password

**Admin panel** (`/admin`)
- Dashboard: revenue, orders by status, customers, active sessions, recent orders, low-stock alerts
- Product add, edit and hide/publish; category create, edit and delete
- Order search and filter; mark as shipped or delivered (COD is marked collected on delivery); cancel with restock and refund
- Customer list with enable/disable

**REST API** (JSON)
```
GET    /api/categories
GET    /api/products?q=&categoryId=&sort=&page=&size=
GET    /api/products/{id}
GET    /api/cart
POST   /api/cart/items          {"productId":3,"quantity":1}     (X-CSRF-Token header)
PUT    /api/cart/items/{id}     {"quantity":2}
DELETE /api/cart/items/{id}
GET    /api/orders              (login required)
GET    /api/orders/{orderNumber}
GET    /health                  (plain servlet: DB status, session count)
```

**Security:** BCrypt passwords, CSRF tokens on every POST, session-fixation protection, the auth/role filter, open-redirect protection, output escaping with `<c:out>`, parameterised SQL and whitelisted sort orders, atomic stock reservation, `SELECT ... FOR UPDATE` on payment and cancel, and security headers. Full card numbers and CVVs are never stored; only the brand and last 4 digits are kept.

## Architecture

```
Browser ──> Tomcat 11
             ├─ EncodingFilter -> CsrfFilter -> AuthFilter          (web.xml, ordered)
             ├─ HealthServlet  /health                              (@WebServlet)
             ├─ ProductImageServlet /images/product/{id}.svg        (@WebServlet)
             ├─ SessionTrackingListener                             (@WebListener)
             └─ Spring DispatcherServlet "/"                        (AppInitializer, no XML)
                   ├─ Controllers (Home, Catalog, Cart, Checkout, Payment, Order, Auth, Account, Admin, Api)
                   ├─ Services  ── CatalogService | CartService | OrderService | PaymentService | UserService | PricingService
                   ├─ DAOs      ── JdbcTemplate (Product, Category, Cart, Order, Payment, User)
                   └─ JSP views /WEB-INF/views/**  + tag files /WEB-INF/tags/*
                                      │
                                    MySQL
```

```
src/main/java/com/shopease
 ├─ config/      AppInitializer, RootConfig, WebConfig, DatabaseInitializer
 ├─ model/       Product, Category, Cart, CartItem, Order, OrderItem, Payment, User, enums...
 ├─ dao/         *Dao (JdbcTemplate)
 ├─ service/     Catalog, Cart, Order, Payment, Pricing, User services + StoreSettings
 ├─ exception/   NotFoundException, BusinessException
 └─ web/         controller/, filter/, servlet/, listener/
src/main/resources  app.properties, db/schema.sql, db/data.sql
src/main/webapp     WEB-INF/web.xml, WEB-INF/views/**.jsp, WEB-INF/tags/*.tag, assets/{css,js,img}
```

## 1. Prerequisites

- **JDK 17 or newer** (Tomcat 11 needs Java 17+). Make sure `JAVA_HOME` points at it when you run Maven.
- **Maven 3.9+**
- **MySQL 8.0.16+** (or 9.x)
- **Apache Tomcat 11.0.x**

## 2. Database

The app creates its tables and loads the sample catalogue (6 categories, 30 products) on first start-up. You only need a MySQL user it can log in with:

```sql
CREATE DATABASE shopease CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'shopease'@'localhost' IDENTIFIED BY 'ChangeMe#123';
GRANT ALL PRIVILEGES ON shopease.* TO 'shopease'@'localhost';
```

To create the schema yourself instead, run `src/main/resources/db/schema.sql` and then `data.sql`.

### Ubuntu server: one-step MySQL + JDBC setup

The MySQL JDBC driver (`mysql-connector-j`) is already bundled in the WAR, so nothing has to be copied into Tomcat's `lib`.
On the Ubuntu machine that runs Tomcat, [`deploy/setup-mysql-jdbc.sh`](deploy/setup-mysql-jdbc.sh) installs MySQL, creates the database and user, tests the login and writes the JDBC settings into Tomcat's `bin/setenv.sh`:

```bash
chmod +x deploy/setup-mysql-jdbc.sh
sudo ./deploy/setup-mysql-jdbc.sh                     # random DB password, saved in /root/shopease-db.credentials
sudo DB_PASSWORD='MyStr0ngPass' ./deploy/setup-mysql-jdbc.sh --restart   # restart Tomcat + check /shopease/health
sudo CATALINA_BASE=/opt/tomcat ./deploy/setup-mysql-jdbc.sh             # if Tomcat isn't auto-detected
```

It is safe to re-run. Run `--help` for all the options, including a remote MySQL host.

## 3. Configure

Defaults live in `src/main/resources/app.properties` (`root` / `root` on `localhost:3306/shopease`). **Override them without rebuilding**, either with environment variables or with JVM properties in Tomcat's `bin/setenv.bat` / `setenv.sh`:

```bat
rem  %CATALINA_HOME%\bin\setenv.bat
set "CATALINA_OPTS=%CATALINA_OPTS% -Ddb.url=jdbc:mysql://localhost:3306/shopease?useSSL=false^&allowPublicKeyRetrieval=true^&serverTimezone=UTC -Ddb.username=shopease -Ddb.password=ChangeMe#123"
```

```bash
# $CATALINA_HOME/bin/setenv.sh   (or export DB_URL / DB_USERNAME / DB_PASSWORD)
export CATALINA_OPTS="$CATALINA_OPTS -Ddb.username=shopease -Ddb.password=ChangeMe#123"
```

Other settings: `app.tax.rate` (default 0.18), `app.shipping.freeThreshold` (999), `app.shipping.flatFee` (49), `app.currency.symbol` (₹), `app.admin.email`, `app.admin.password`, and `app.db.init` (set it to `false` once you manage the schema yourself).

### Option: connection details in Tomcat's `server.xml` (JNDI DataSource)

If Tomcat defines a global `jdbc/ShopEaseDB` resource, the app uses it ahead of the `db.*` settings. `META-INF/context.xml` in the WAR links it automatically.

1. Copy the MySQL driver into Tomcat's `lib` (Tomcat creates the pool itself, so it needs the driver there):
   ```bash
   sudo cp ~/.m2/repository/com/mysql/mysql-connector-j/9.4.0/mysql-connector-j-9.4.0.jar /opt/tomcat/lib/
   ```
2. Inside `<GlobalNamingResources>` in `/opt/tomcat/conf/server.xml` add (`&` in the URL must be written `&amp;`):
   ```xml
   <Resource name="jdbc/ShopEaseDB" auth="Container" type="javax.sql.DataSource"
             factory="org.apache.tomcat.dbcp.dbcp2.BasicDataSourceFactory"
             driverClassName="com.mysql.cj.jdbc.Driver"
             url="jdbc:mysql://localhost:3306/shopease?useSSL=false&amp;allowPublicKeyRetrieval=true&amp;serverTimezone=UTC&amp;characterEncoding=UTF-8"
             username="shopease" password="CHANGE_ME"
             maxTotal="20" maxIdle="5" minIdle="2" maxWaitMillis="10000"
             validationQuery="SELECT 1" testOnBorrow="true"/>
   ```
3. Restart Tomcat. `/shopease/health` then shows `"dataSource":"jndi:java:comp/env/jdbc/ShopEaseDB"`.

## 4. Build

```bash
mvn clean package
```

This produces **`target/shopease.war`** and runs the unit tests.

## 5. Deploy

Copy `target/shopease.war` into `%CATALINA_HOME%\webapps\` and start Tomcat (`bin\startup.bat` or `bin/startup.sh`), then open:

- Store: http://localhost:8080/shopease/
- Admin: http://localhost:8080/shopease/admin
- Health: http://localhost:8080/shopease/health

**Default admin login:** `admin@shopease.com` / `Admin@123`. It is created on first start; change it with `app.admin.*` before going live.

### Option: containers with Docker Compose (Tomcat + PostgreSQL)

```bash
cp .env.example .env   # set the passwords
docker compose up -d --build        # or: podman-compose up -d --build
```

Store at http://localhost:8081/. Uses named volumes (PostgreSQL data, uploaded images), a bind mount
(Tomcat logs in `./logs/tomcat`) and tmpfs (Tomcat temp/work), with backup and restore scripts in
`docker/volumes/`. Full walkthrough: [docker/COMPOSE.md](docker/COMPOSE.md).

## Test payment data (simulated gateway)

| Method | Succeeds | Declined |
|---|---|---|
| Card | `4111 1111 1111 1111`, any future MM/YY, CVV `123` | any card ending `0002`, e.g. `4000 0000 0000 0002` |
| UPI | `anyone@okbank` | `fail@upi` |
| Net banking | any listed bank | - |
| Cash on delivery | orders up to ₹50,000 | - |

To use a real gateway (Razorpay, Stripe, PayU, ...), replace `PaymentService#gatewayCharge` with the provider's SDK call and webhook handling.

## Order life-cycle

```
PENDING_PAYMENT ──pay──> PAID ──ship──> SHIPPED ──deliver──> DELIVERED
       │          └─COD─> PLACED ─┘
       └──────── cancel (before SHIPPED): restock + refund ──> CANCELLED
```
