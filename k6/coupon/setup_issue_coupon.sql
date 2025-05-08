SET SESSION cte_max_recursion_depth = 10000000;

-- COUPON
INSERT INTO discount_condition (condition_type, min_amount, product_id)
VALUES ('MIN_ORDER_AMOUNT', 5000.00, NULL);
INSERT INTO discount_type (type, discount_amount, max_discount_amount, discount_rate)
VALUES ('RATE', NULL, 10000.00, 10.00);
-- 조건 ID = 1, 타입 ID = 1 이라고 가정
INSERT INTO discount_policy (id, name, discount_condition_id, discount_type_id)
VALUES (1, '10% 할인 정책', 1, 1);
INSERT INTO coupons (
    created_at, description, end_at, is_active,
    issued_count, max_issue_limit, name,
    start_at, updated_at, valid_days, discount_policy_id
)
VALUES (
    NOW(), '10% 할인 쿠폰입니다', '2025-12-31 23:59:59', 1,
    0, 100000, '연말 10% 할인 쿠폰',
    '2025-01-01 00:00:00', NOW(), 30, 1
);