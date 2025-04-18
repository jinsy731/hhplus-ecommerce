-- 사용자 포인트 테스트 데이터
-- 테이블 초기화
DELETE FROM USER_POINT_HISTORY;
DELETE FROM USER_POINT;

-- Auto increment 재설정 (MySQL 기준)
ALTER TABLE USER_POINT AUTO_INCREMENT = 1;
ALTER TABLE USER_POINT_HISTORY AUTO_INCREMENT = 1;

-- 사용자 포인트 데이터 삽입
INSERT INTO USER_POINT (id, user_id, balance, created_at, updated_at) 
VALUES (1, 1, 10000.00, '2024-01-01 00:00:00', '2024-01-01 00:00:00');

INSERT INTO USER_POINT (id, user_id, balance, created_at, updated_at) 
VALUES (2, 2, 5000.00, '2024-01-01 00:00:00', '2024-01-01 00:00:00');

-- 사용자 포인트 히스토리 데이터 삽입
INSERT INTO USER_POINT_HISTORY (id, user_id, transaction_type, amount, created_at) 
VALUES (1, 1, 'CHARGE', 10000.00, '2024-01-01 00:00:00');

INSERT INTO USER_POINT_HISTORY (id, user_id, transaction_type, amount, created_at) 
VALUES (2, 2, 'CHARGE', 5000.00, '2024-01-01 00:00:00');

-- 사용자 2의 포인트 사용 기록
INSERT INTO USER_POINT_HISTORY (id, user_id, transaction_type, amount, created_at) 
VALUES (3, 2, 'USE', 1000.00, '2024-01-02 00:00:00');
