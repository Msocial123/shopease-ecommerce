-- =====================================================================
--  ShopEase sample catalogue (loaded only when the categories table is empty)
--  image_url is left NULL: ProductImageServlet renders an SVG placeholder.
-- =====================================================================

INSERT INTO categories (id, name, slug, description) VALUES
 (1, 'Electronics',     'electronics', 'Phones, laptops, audio and smart gadgets'),
 (2, 'Fashion',         'fashion',     'Clothing, footwear and accessories'),
 (3, 'Home & Kitchen',  'home-kitchen','Cookware, appliances and decor'),
 (4, 'Books',           'books',       'Fiction, non-fiction and technical books'),
 (5, 'Sports & Fitness','sports',      'Gear for training, outdoors and games'),
 (6, 'Beauty & Care',   'beauty',      'Skincare, grooming and wellness');

INSERT INTO products (category_id, sku, name, brand, description, price, mrp, stock, rating, featured) VALUES
 (1, 'EL-1001', 'Nova X5 Smartphone 128GB', 'Nova', '6.5-inch AMOLED display, 50MP triple camera, 5000mAh battery with 33W fast charging and 5G connectivity.', 18999.00, 22999.00, 40, 4.4, TRUE),
 (1, 'EL-1002', 'AeroBook 14 Laptop (i5, 16GB, 512GB SSD)', 'Aero', 'Thin and light 14-inch FHD laptop with backlit keyboard, Wi-Fi 6 and all-day battery life.', 58990.00, 69990.00, 15, 4.5, TRUE),
 (1, 'EL-1003', 'PulseBuds Pro Wireless Earbuds', 'Pulse', 'Active noise cancellation, 30-hour battery with case, IPX5 sweat resistance and low-latency gaming mode.', 2999.00, 5999.00, 120, 4.2, TRUE),
 (1, 'EL-1004', 'Orbit Smartwatch S2', 'Orbit', '1.8-inch display, heart-rate and SpO2 tracking, 100+ sports modes, Bluetooth calling.', 3499.00, 6999.00, 75, 4.0, FALSE),
 (1, 'EL-1005', 'BoomBox 20W Bluetooth Speaker', 'Boom', 'Portable speaker with deep bass, 12-hour playback and waterproof IPX7 body.', 1799.00, 2999.00, 60, 4.3, FALSE),
 (1, 'EL-1006', 'ViewMax 27" 4K Monitor', 'ViewMax', '27-inch IPS 4K UHD monitor with HDR10, USB-C 65W and height-adjustable stand.', 24999.00, 31999.00, 12, 4.6, FALSE),
 (2, 'FA-2001', 'Classic Cotton Crew T-Shirt', 'Urbane', '100% combed cotton, regular fit, pre-shrunk. Available in multiple colours.', 499.00, 899.00, 200, 4.1, TRUE),
 (2, 'FA-2002', 'Slim Fit Stretch Denim Jeans', 'Urbane', 'Mid-rise slim fit jeans with 2% elastane for all-day comfort.', 1299.00, 2499.00, 90, 4.2, FALSE),
 (2, 'FA-2003', 'StrideRun Running Shoes', 'Stride', 'Lightweight mesh upper, cushioned midsole and durable rubber outsole.', 2499.00, 3999.00, 70, 4.4, TRUE),
 (2, 'FA-2004', 'Leather Bi-Fold Wallet', 'Hide & Co', 'Genuine leather wallet with RFID protection, 6 card slots and coin pocket.', 799.00, 1499.00, 150, 4.3, FALSE),
 (2, 'FA-2005', 'Aviator Polarized Sunglasses', 'Solaris', 'UV400 polarized lenses with lightweight metal frame and case.', 999.00, 1999.00, 85, 4.0, FALSE),
 (3, 'HK-3001', 'Non-Stick Cookware Set (5 pcs)', 'ChefLine', 'Granite-coated non-stick kadai, fry pan, tawa and saucepan with glass lids. Induction ready.', 2899.00, 4999.00, 35, 4.3, TRUE),
 (3, 'HK-3002', 'PowerBlend 750W Mixer Grinder', 'PowerBlend', '3 stainless steel jars, overload protection and 5-year motor warranty.', 3299.00, 4799.00, 28, 4.4, FALSE),
 (3, 'HK-3003', 'Air Fryer 4.5L Digital', 'CrispIt', 'Cook with up to 90% less oil. 8 presets, touch controls and dishwasher-safe basket.', 5499.00, 8999.00, 22, 4.5, TRUE),
 (3, 'HK-3004', 'Cotton Double Bedsheet Set', 'Dreamz', '210 TC pure cotton bedsheet with 2 pillow covers. Fade resistant.', 899.00, 1599.00, 100, 4.1, FALSE),
 (3, 'HK-3005', 'Stainless Steel Water Bottle 1L', 'HydroMate', 'Double-wall vacuum insulated. Keeps cold 24h / hot 12h. Leak proof.', 649.00, 999.00, 180, 4.6, FALSE),
 (4, 'BK-4001', 'Clean Code', 'Prentice Hall', 'A handbook of agile software craftsmanship by Robert C. Martin.', 699.00, 999.00, 50, 4.7, TRUE),
 (4, 'BK-4002', 'Effective Java (3rd Edition)', 'Addison-Wesley', 'Best practices for the Java platform by Joshua Bloch.', 899.00, 1299.00, 45, 4.8, TRUE),
 (4, 'BK-4003', 'Spring in Action', 'Manning', 'Hands-on guide to the Spring Framework and Spring Boot.', 1099.00, 1499.00, 30, 4.5, FALSE),
 (4, 'BK-4004', 'The Pragmatic Programmer', 'Addison-Wesley', 'Your journey to mastery, 20th anniversary edition.', 749.00, 1099.00, 40, 4.7, FALSE),
 (4, 'BK-4005', 'Atomic Habits', 'Penguin', 'An easy and proven way to build good habits and break bad ones.', 399.00, 799.00, 150, 4.8, FALSE),
 (5, 'SP-5001', 'Anti-Slip Yoga Mat 6mm', 'FlexFit', 'High-density TPE mat with alignment lines and carry strap.', 799.00, 1499.00, 110, 4.3, TRUE),
 (5, 'SP-5002', 'Adjustable Dumbbell Set 20kg', 'IronCore', 'Pair of adjustable dumbbells with chrome handles and secure spin-lock collars.', 2499.00, 3999.00, 25, 4.4, FALSE),
 (5, 'SP-5003', 'English Willow Cricket Bat', 'Striker', 'Grade 3 English willow, full size, with premium grip and cover.', 4999.00, 7499.00, 18, 4.5, FALSE),
 (5, 'SP-5004', 'Pro Football Size 5', 'Kickoff', 'Hand-stitched PU football for training and matches.', 699.00, 1199.00, 90, 4.2, FALSE),
 (5, 'SP-5005', 'Resistance Bands Set (5 levels)', 'FlexFit', 'Latex loop bands for strength training, physio and stretching.', 449.00, 899.00, 160, 4.1, FALSE),
 (6, 'BC-6001', 'Vitamin C Face Serum 30ml', 'GlowLab', '10% Vitamin C with hyaluronic acid for brighter, hydrated skin.', 549.00, 899.00, 140, 4.2, TRUE),
 (6, 'BC-6002', 'Cordless Beard Trimmer', 'Groomo', '40 length settings, 90-min runtime and USB-C fast charge.', 1299.00, 2199.00, 65, 4.3, FALSE),
 (6, 'BC-6003', 'Herbal Shampoo 650ml', 'Naturelle', 'Sulphate-free shampoo with aloe vera and hibiscus.', 349.00, 499.00, 200, 4.0, FALSE),
 (6, 'BC-6004', 'Sunscreen SPF 50 PA++++', 'GlowLab', 'Lightweight, non-greasy gel sunscreen for all skin types.', 449.00, 699.00, 130, 4.4, FALSE);
