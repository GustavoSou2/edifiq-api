INSERT INTO plans (name, max_users, max_suppliers, max_orders_per_month, has_analytics, has_api_access, price_monthly)
VALUES
    ('free', 3, 20, 30, false, false, 0.00),
    ('starter', 10, 100, 300, false, true, 199.00),
    ('pro', 50, 500, 2000, true, true, 599.00),
    ('enterprise', 500, 5000, 99999, true, true, 1999.00)
ON DUPLICATE KEY UPDATE
    max_users = VALUES(max_users),
    max_suppliers = VALUES(max_suppliers),
    max_orders_per_month = VALUES(max_orders_per_month),
    has_analytics = VALUES(has_analytics),
    has_api_access = VALUES(has_api_access),
    price_monthly = VALUES(price_monthly);

