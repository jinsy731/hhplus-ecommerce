-- 외래키 무시하고 전체 테이블 TRUNCATE
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE composite_condition_mapping;
TRUNCATE TABLE coupons;
TRUNCATE TABLE discount_condition;
TRUNCATE TABLE discount_lines;
TRUNCATE TABLE discount_policy;
TRUNCATE TABLE discount_type;
TRUNCATE TABLE option_specs;
TRUNCATE TABLE option_values;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE p_sales_agg_day;
TRUNCATE TABLE payment_item_details;
TRUNCATE TABLE payment_methods;
TRUNCATE TABLE payments;
TRUNCATE TABLE popular_products_daily;
TRUNCATE TABLE product_sales_aggregation_daily_checkpoint;
TRUNCATE TABLE product_sales_log;
TRUNCATE TABLE product_variants;
TRUNCATE TABLE products;
TRUNCATE TABLE user_coupons;
TRUNCATE TABLE user_point;
TRUNCATE TABLE user_point_history;

SET FOREIGN_KEY_CHECKS = 1;
