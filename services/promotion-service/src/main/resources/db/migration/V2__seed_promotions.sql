-- Sample promotions for development/testing
INSERT INTO promotions (code, name, description, promotion_type, discount_type, discount_value,
                        min_order_amount, max_discount_amount, total_quantity, max_per_customer,
                        start_date, end_date) VALUES
    ('WELCOME10', 'Welcome 10% Off', 'New customer welcome discount',
     'COUPON', 'PERCENTAGE', 10.00, 100000, 500000, 1000, 1,
     NOW(), NOW() + INTERVAL '365 days'),

    ('FREESHIP', 'Free Shipping', 'Free shipping on orders over 200K VND',
     'VOUCHER', 'FREE_SHIPPING', 0, 200000, NULL, 5000, 3,
     NOW(), NOW() + INTERVAL '90 days'),

    ('FLASH50K', 'Flash Sale 50K Off', 'Limited quantity — 50,000 VND off',
     'FLASH_SALE', 'FIXED_AMOUNT', 50000, 300000, 50000, 100, 1,
     NOW(), NOW() + INTERVAL '7 days'),

    ('SUMMER2026', 'Summer Campaign 15%', 'Summer 2026 campaign',
     'CAMPAIGN', 'PERCENTAGE', 15.00, 500000, 1000000, 10000, 2,
     NOW(), NOW() + INTERVAL '60 days');
