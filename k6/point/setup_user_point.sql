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
    n AS user_id,
    0
FROM cte;