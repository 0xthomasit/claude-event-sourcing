-- Seed 20 sample products for development linked to categories
INSERT INTO products (id, sku, name, description, price, currency, category_id, status, image_url) VALUES
    -- Điện thoại (11111111-0000-0000-0001-000000000001)
    (gen_random_uuid(), 'IPHONE15PM', 'iPhone 15 Pro Max 256GB', 'Điện thoại di động Apple iPhone 15 Pro Max thế hệ mới với vỏ titan siêu bền.', 29990000.00, 'VND', '11111111-0000-0000-0001-000000000001', 'ACTIVE', 'https://images.unsplash.com/photo-1695048133142-1a20484d2569'),
    (gen_random_uuid(), 'S24ULTRA', 'Samsung Galaxy S24 Ultra 256GB', 'Điện thoại Samsung S24 Ultra tích hợp AI thông minh thế hệ mới, camera 200MP.', 27990000.00, 'VND', '11111111-0000-0000-0001-000000000001', 'ACTIVE', 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf'),
    (gen_random_uuid(), 'XIAOMI14', 'Xiaomi 14 12GB/256GB', 'Xiaomi 14 nổi bật với cụm camera Leica thế hệ mới và chip Snapdragon 8 Gen 3.', 18990000.00, 'VND', '11111111-0000-0000-0001-000000000001', 'ACTIVE', ''),

    -- Laptop (11111111-0000-0000-0001-000000000002)
    (gen_random_uuid(), 'MACBOOKPRO14M3', 'MacBook Pro 14 M3 8GB/512GB', 'Apple MacBook Pro 14 inch sử dụng chip Apple M3 hiệu năng vượt trội, màn hình Liquid Retina XDR.', 39990000.00, 'VND', '11111111-0000-0000-0001-000000000002', 'ACTIVE', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8'),
    (gen_random_uuid(), 'DELLXPS15', 'Dell XPS 15 9530', 'Laptop Dell XPS 15 trang bị màn hình OLED 3.5K, CPU Intel Core i7 và card đồ họa rời RTX 4050.', 45990000.00, 'VND', '11111111-0000-0000-0001-000000000002', 'ACTIVE', 'https://images.unsplash.com/photo-1593642632823-8f785ba67e45'),
    (gen_random_uuid(), 'ASUSROG14', 'Asus ROG Zephyrus G14', 'Laptop gaming mỏng nhẹ Asus ROG Zephyrus G14 thế hệ mới, CPU Ryzen 9 và GPU RTX 4060.', 37490000.00, 'VND', '11111111-0000-0000-0001-000000000002', 'ACTIVE', ''),

    -- Phụ kiện (11111111-0000-0000-0001-000000000003)
    (gen_random_uuid(), 'AIRPODSPRO2', 'Apple AirPods Pro Gen 2 Type-C', 'Tai nghe không dây chống ồn chủ động AirPods Pro thế hệ 2, cổng sạc USB Type-C mới.', 5790000.00, 'VND', '11111111-0000-0000-0001-000000000003', 'ACTIVE', 'https://images.unsplash.com/photo-1588449668365-d15e397f6787'),
    (gen_random_uuid(), 'KEYCHRONK2', 'Bàn phím cơ Keychron K2 V2', 'Bàn phím cơ không dây Keychron K2, switch Gateron, đèn nền RGB sang trọng.', 1890000.00, 'VND', '11111111-0000-0000-0001-000000000003', 'ACTIVE', 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef'),
    (gen_random_uuid(), 'ANKER65W', 'Sạc Anker Nano II 65W GaN', 'Củ sạc siêu nhanh nhỏ gọn Anker GaN II 65W, trang bị công nghệ PowerIQ 3.0.', 790000.00, 'VND', '11111111-0000-0000-0001-000000000003', 'ACTIVE', ''),

    -- Thời trang (11111111-0000-0000-0000-000000000002)
    (gen_random_uuid(), 'POLOTSHIRT', 'Áo thun nam Polo Basic', 'Áo thun nam có cổ Polo chất liệu cotton 100% thoáng mát, form dáng thanh lịch.', 250000.00, 'VND', '11111111-0000-0000-0000-000000000002', 'ACTIVE', 'https://images.unsplash.com/photo-1581655353564-df123a1eb820'),
    (gen_random_uuid(), 'JEANSLIMFIT', 'Quần Jean Nam Slimfit Co Giãn', 'Quần jeans nam dáng ôm trẻ trung, chất vải cao cấp mềm mịn và bền màu.', 450000.00, 'VND', '11111111-0000-0000-0000-000000000002', 'ACTIVE', 'https://images.unsplash.com/photo-1542272604-787c3835535d'),
    (gen_random_uuid(), 'NIKEAF1', 'Giày Sneaker Nike Air Force 1', 'Giày thể thao thời trang nam nữ Nike Air Force 1 All-White cổ điển.', 2950000.00, 'VND', '11111111-0000-0000-0000-000000000002', 'ACTIVE', 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a'),

    -- Nhà cửa (11111111-0000-0000-0000-000000000003)
    (gen_random_uuid(), 'PHILIPSFRIER', 'Nồi chiên không dầu Philips HD9252', 'Nồi chiên không dầu Philips dung tích 4.1L sử dụng công nghệ Rapid Air giảm 90% dầu mỡ.', 2390000.00, 'VND', '11111111-0000-0000-0000-000000000003', 'ACTIVE', 'https://images.unsplash.com/photo-1621972750749-0fbb1abb7736'),
    (gen_random_uuid(), 'ECOVACSVAC', 'Robot hút bụi lau nhà Ecovacs N8', 'Robot hút bụi Ecovacs lau nhà kết hợp lực hút mạnh 2300Pa, lập bản đồ thông minh.', 5490000.00, 'VND', '11111111-0000-0000-0000-000000000003', 'ACTIVE', 'https://images.unsplash.com/photo-1572347324580-785557d2c749'),
    (gen_random_uuid(), 'SUNHOUSEPOTS', 'Bộ nồi inox Sunhouse 3 đáy SH781', 'Bộ nồi inox 3 đáy cao cấp Sunhouse gồm 3 kích cỡ 16cm, 20cm và 24cm.', 690000.00, 'VND', '11111111-0000-0000-0000-000000000003', 'ACTIVE', ''),

    -- Sách (11111111-0000-0000-0000-000000000004)
    (gen_random_uuid(), 'DACNHANTAM', 'Sách Đắc Nhân Tâm (Khổ Nhỏ)', 'Cuốn sách nghệ thuật ứng xử nổi tiếng nhất mọi thời đại của tác giả Dale Carnegie.', 86000.00, 'VND', '11111111-0000-0000-0000-000000000004', 'ACTIVE', 'https://images.unsplash.com/photo-1544947950-fa07a98d237f'),
    (gen_random_uuid(), 'NHAGIAKIM', 'Sách Nhà Giả Kim', 'Tiểu thuyết triết lý nổi tiếng của nhà văn Paulo Coelho về hành trình đi tìm giấc mơ.', 79000.00, 'VND', '11111111-0000-0000-0000-000000000004', 'ACTIVE', 'https://images.unsplash.com/photo-1512820790803-83ca734da794'),

    -- Thể thao (11111111-0000-0000-0000-000000000005)
    (gen_random_uuid(), 'ADIDASYOGAMAT', 'Thảm tập Yoga Adidas 8mm', 'Thảm tập Yoga chính hãng Adidas dày 8mm chống trượt tốt, chất liệu êm ái bảo vệ khớp.', 650000.00, 'VND', '11111111-0000-0000-0000-000000000005', 'ACTIVE', 'https://images.unsplash.com/photo-1592432678016-e910b452f9a2'),
    (gen_random_uuid(), 'NIKEBALL', 'Quả bóng đá Nike Pitch Team', 'Bóng đá Nike thiết kế bền bỉ, đường may tỉ mỉ, độ nảy tốt phù hợp luyện tập.', 450000.00, 'VND', '11111111-0000-0000-0000-000000000005', 'ACTIVE', 'https://images.unsplash.com/photo-1508098682722-e99c43a406b2'),
    (gen_random_uuid(), 'LOCKSPORTSBOTTLE', 'Bình nước thể thao Lock&Lock 700ml', 'Bình nước thể thao Lock&Lock chất liệu nhựa Tritan an toàn, thiết kế năng động.', 150000.00, 'VND', '11111111-0000-0000-0000-000000000005', 'ACTIVE', '')
ON CONFLICT (sku) DO NOTHING;
