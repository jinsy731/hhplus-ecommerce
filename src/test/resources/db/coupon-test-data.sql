-- Coupon, UserCoupon 테스트 데이터
-- 테이블 초기화
DELETE FROM user_coupons;
DELETE FROM coupons;
DELETE FROM discount_policy;
DELETE FROM discount_type;
DELETE FROM discount_condition;

-- Auto increment 재설정 (MySQL 기준)
ALTER TABLE discount_condition AUTO_INCREMENT = 1;
ALTER TABLE discount_type AUTO_INCREMENT = 1;
ALTER TABLE discount_policy AUTO_INCREMENT = 1;
ALTER TABLE coupons AUTO_INCREMENT = 1;
ALTER TABLE user_coupons AUTO_INCREMENT = 1;

-- 할인 조건 데이터 삽입
INSERT INTO discount_condition (id, condition_type, min_amount) VALUES (1, 'MIN_ORDER_AMOUNT', 10000.00);
INSERT INTO discount_condition (id, condition_type) VALUES (2, 'ALL_PRODUCT');

-- 할인 유형 데이터 삽입
INSERT INTO discount_type (id, type, discount_amount) VALUES (1, 'FIXED_AMOUNT_TOTAL', 5000.00);
INSERT INTO discount_type (id, type, discount_amount) VALUES (2, 'FIXED_AMOUNT_PER_ITEM', 2000.00);
INSERT INTO discount_type (id, type, discount_rate, max_discount_amount) VALUES (3, 'RATE', 0.1, 10000.00);

-- 할인 정책 데이터 삽입
INSERT INTO discount_policy (id, name, discount_type_id, discount_condition_id) VALUES (1, '상품 전체 5,000원 할인', 1, 1);
INSERT INTO discount_policy (id, name, discount_type_id, discount_condition_id) VALUES (2, '모든 상품 2,000원 할인', 2, 2);
INSERT INTO discount_policy (id, name, discount_type_id, discount_condition_id) VALUES (3, '10% 할인(최대 10,000원)', 3, 1);

-- 쿠폰 데이터 삽입
INSERT INTO coupons (id, name, description, discount_policy_id, is_active, max_issue_limit, issued_count, start_at, end_at, valid_days, created_at, updated_at) 
VALUES (1, '신규 회원 할인 쿠폰', '회원가입 후 7일 이내 사용 가능한 5,000원 할인 쿠폰', 1, true, 100, 0, '2024-01-01 00:00:00', '2025-12-31 23:59:59', 7, '2024-01-01 00:00:00', '2024-01-01 00:00:00');

INSERT INTO coupons (id, name, description, discount_policy_id, is_active, max_issue_limit, issued_count, start_at, end_at, valid_days, created_at, updated_at) 
VALUES (2, '첫 구매 할인 쿠폰', '첫 구매 시 사용 가능한 2,000원 할인 쿠폰', 2, true, 100, 0, '2024-01-01 00:00:00', '2025-12-31 23:59:59', 30, '2024-01-01 00:00:00', '2024-01-01 00:00:00');

INSERT INTO coupons (id, name, description, discount_policy_id, is_active, max_issue_limit, issued_count, start_at, end_at, valid_days, created_at, updated_at) 
VALUES (3, '시즌 할인 쿠폰', '10% 할인 쿠폰 (최대 10,000원)', 3, true, 100, 0, '2024-01-01 00:00:00', '2025-12-31 23:59:59', 15, '2024-01-01 00:00:00', '2024-01-01 00:00:00');

-- 사용자 쿠폰 데이터 (샘플용 - 테스트에서는 발급 API를 통해 생성)
INSERT INTO user_coupons (id, user_id, coupon_id, issued_at, expired_at, used_at, status) 
VALUES (1, 1, 1, '2024-01-01 00:00:00', '2024-01-08 00:00:00', NULL, 'UNUSED');

INSERT INTO user_coupons (id, user_id, coupon_id, issued_at, expired_at, used_at, status) 
VALUES (2, 1, 2, '2024-01-01 00:00:00', '2024-01-31 00:00:00', '2024-01-15 00:00:00', 'USED');

INSERT INTO user_coupons (id, user_id, coupon_id, issued_at, expired_at, used_at, status) 
VALUES (3, 2, 1, '2024-01-01 00:00:00', '2024-01-08 00:00:00', NULL, 'UNUSED');
