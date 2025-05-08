-- 높은 재귀(반복) 횟수를 허용하도록 설정
SET SESSION cte_max_recursion_depth = 10000000;

-- USER POINT
INSERT INTO user_point (id, balance, created_at, updated_at, user_id, version)
WITH RECURSIVE cte (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM cte WHERE n < 100000 -- 원하는 레코드 수 조절 가능
)
SELECT
    n AS id,
    ROUND(1000000 + (RAND() * 9000000), 2) AS balance, -- 최소 100만 ~ 최대 1천만
    FROM_UNIXTIME(UNIX_TIMESTAMP(NOW()) - FLOOR(RAND() * 31536000)) AS created_at,
    FROM_UNIXTIME(UNIX_TIMESTAMP(NOW()) - FLOOR(RAND() * 31536000)) AS updated_at,
    n AS user_id
    0
FROM cte;


-- PRODUCT
-- Products
INSERT INTO products (product_id, name, base_price, created_at, updated_at, status)
WITH RECURSIVE cte (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM cte WHERE n < 100000 -- 👈 원하는 수량
),
product_data AS (
    SELECT
        n AS product_id,
        CONCAT(
            ELT(FLOOR(1 + (RAND() * 10)), '프리미엄', '클래식', '럭셔리', '컴포트', '에코', '하이퍼', '울트라', '어반', '다이나믹', '슈퍼'),
            ' ',
            ELT(FLOOR(1 + (RAND() * 10)), '셔츠', '티셔츠', '맨투맨', '후디', '자켓', '바지', '청바지', '슬랙스', '코트', '점퍼'),
            ' ',
            LPAD(n, 6, '0')
        ) AS name,
        ROUND(10000 + (RAND() * 40000), 2) AS base_price,
        FROM_UNIXTIME(UNIX_TIMESTAMP(NOW()) - FLOOR(RAND() * 31536000)) AS created_at,
        FROM_UNIXTIME(UNIX_TIMESTAMP(NOW()) - FLOOR(RAND() * 31536000)) AS updated_at,
        'ON_SALE' AS status
    FROM cte
)
SELECT * FROM product_data;


-- Option Specs
INSERT INTO option_specs (option_spec_id, created_at, updated_at, display_order, name, product_id)
WITH RECURSIVE spec_cte (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM spec_cte WHERE n < 200000
)
SELECT
    n,
    NOW(),
    NOW(),
    IF(n % 2 = 1, 1, 2),
    IF(n % 2 = 1, '색상', '사이즈'),
    CEIL(n / 2)
FROM spec_cte;

-- Option Values
INSERT INTO option_values (option_value_id, created_at, updated_at, value, option_spec_id)
WITH RECURSIVE val_cte (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM val_cte WHERE n < 600000
)
SELECT
    n,
    NOW(),
    NOW(),
    ELT((n - 1) % 3 + 1, '빨강', '파랑', '검정'),
    CEIL(n / 3)
FROM val_cte;

-- Product Variants
INSERT INTO product_variants (created_at, updated_at, additional_price, option_values, status, stock, product_id)
WITH RECURSIVE var_cte (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM var_cte WHERE n < 300000
)
SELECT
    NOW(),
    NOW(),
    ROUND(RAND() * 2000, 2),
    CONCAT(
        ELT(n % 3 + 1, '빨강', '파랑', '검정'),
        '/',
        ELT(n % 3 + 1, 'M', 'L', 'XL')
    ),
    'ACTIVE',
    FLOOR(1 + (RAND() * 20)),
    CEIL(n / 3)
FROM var_cte;

-- aggregation
-- 🔹 Day -3


-- 🔹 Day -2
INSERT INTO p_sales_agg_day (product_id, sales_day, sales_count)
WITH RECURSIVE product_cte (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1
    FROM product_cte
    WHERE n < 99999
)
SELECT
    n AS product_id,
    (CURDATE() - INTERVAL 2 DAY) AS sales_day ,
    (FLOOR(1000 + (RAND() * 4001))) AS sales_count
FROM product_cte;

-- 🔹 Day -1
INSERT INTO p_sales_agg_day (product_id, sales_day, sales_count)
WITH RECURSIVE product_cte (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1
    FROM product_cte
    WHERE n < 99999
)
SELECT
    n AS product_id,
    (CURDATE() - INTERVAL 1 DAY) AS sales_day ,
    (FLOOR(1000 + (RAND() * 4001))) AS sales_count
FROM product_cte;

-- 🔹 오늘
INSERT INTO p_sales_agg_day (product_id, sales_day, sales_count)
WITH RECURSIVE product_cte (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1
    FROM product_cte
    WHERE n < 99999
)
SELECT
    n AS product_id,
    (CURDATE()) AS sales_day ,
    (FLOOR(1000 + (RAND() * 4001))) AS sales_count
FROM product_cte;


INSERT INTO discount_condition (condition_type, min_amount, product_id)
VALUES ('MINIMUM_AMOUNT', 5000.00, NULL);
INSERT INTO discount_type (type, discount_amount, max_discount_amount, discount_rate)
VALUES ('RATE', NULL, 10000.00, 10.00);
-- 조건 ID = 1, 타입 ID = 1 이라고 가정
INSERT INTO discount_policy (name, discount_condition_id, discount_type_id)
VALUES ('10% 할인 정책', 1, 1);
INSERT INTO coupons (
    created_at, description, end_at, is_active,
    issued_count, max_issue_limit, name,
    start_at, updated_at, valid_days, discount_policy_id
)
VALUES (
    NOW(), '10% 할인 쿠폰입니다', '2025-12-31 23:59:59', 1,
    0, 1000, '연말 10% 할인 쿠폰',
    '2025-01-01 00:00:00', NOW(), 30, 1
);



