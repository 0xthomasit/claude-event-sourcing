-- Seed root categories for development
INSERT INTO categories (id, name, slug) VALUES
    ('11111111-0000-0000-0000-000000000001', 'Điện tử',       'dien-tu'),
    ('11111111-0000-0000-0000-000000000002', 'Thời trang',    'thoi-trang'),
    ('11111111-0000-0000-0000-000000000003', 'Nhà cửa',       'nha-cua'),
    ('11111111-0000-0000-0000-000000000004', 'Sách',          'sach'),
    ('11111111-0000-0000-0000-000000000005', 'Thể thao',      'the-thao')
ON CONFLICT (slug) DO NOTHING;

-- Sub-categories of Điện tử
INSERT INTO categories (id, name, slug, parent_id) VALUES
    ('11111111-0000-0000-0001-000000000001', 'Điện thoại',    'dien-thoai',   '11111111-0000-0000-0000-000000000001'),
    ('11111111-0000-0000-0001-000000000002', 'Laptop',        'laptop',       '11111111-0000-0000-0000-000000000001'),
    ('11111111-0000-0000-0001-000000000003', 'Phụ kiện',      'phu-kien',     '11111111-0000-0000-0000-000000000001')
ON CONFLICT (slug) DO NOTHING;