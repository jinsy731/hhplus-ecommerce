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

-- COUPON
-- 1. 무조건 사용 가능한 discount_condition 하나 만들기
insert into discount_condition (condition_type, min_amount, product_id)
values ('ALL_PRODUCT', null, null);

-- 2. discount_type (할인 방식 정의, 예를 들면 정액 1000원 할인)
insert into discount_type (type, discount_amount, max_discount_amount, discount_rate)
values ('FIXED_AMOUNT_TOTAL', 1000, null, null);

-- 3. discount_policy (위의 condition과 type을 연결) ID=2
insert into discount_policy (id, name, discount_condition_id, discount_type_id)
values (2, 'No Condition Policy',
        (select id from discount_condition where condition_type = 'ALL_PRODUCT' order by id desc limit 1),
        (select id from discount_type where type = 'FIXED_AMOUNT_TOTAL' order by id desc limit 1)
);


-- 5. UserCoupon 생성
insert into user_coupons (
    expired_at, issued_at, status, used_at, user_id, coupon_id
)
with recursive user_coupon_cte as (
    select 1 as seq
    union all
    select seq + 1
    from user_coupon_cte
    where seq < 100000
)
select
    date_add(now(), interval 30 day),
    now(),
    'UNUSED',
    null,
    seq,  -- user_id = seq (1~100000)
    (select min(id) from coupons) + seq - 1  -- coupon_id도 생성 순서대로 매칭
from user_coupon_cte;