create table discount_condition
(
    condition_type varchar(31)    not null,
    id             bigint auto_increment
        primary key,
    min_amount     decimal(38, 2) null,
    product_id     varchar(255)   null
);

create table composite_condition_mapping
(
    composite_condition_id bigint not null,
    condition_id           bigint not null,
    constraint FK76mb4oi73rf4m3itwc556oba1
        foreign key (condition_id) references discount_condition (id),
    constraint FKiw44f6eg6tgyktr6pydpbu3mu
        foreign key (composite_condition_id) references discount_condition (id)
);

create table discount_lines
(
    id            bigint auto_increment
        primary key,
    amount        decimal(38, 2)               not null,
    created_at    datetime(6)                  not null,
    order_item_id bigint                       not null,
    source_id     bigint                       null,
    type          enum ('COUPON', 'PROMOTION') not null
);

create table discount_type
(
    type                varchar(31)    not null,
    id                  bigint auto_increment
        primary key,
    discount_amount     decimal(38, 2) null,
    max_discount_amount decimal(38, 2) null,
    discount_rate       decimal(38, 2) null
);

create table discount_policy
(
    id                    bigint auto_increment
        primary key,
    name                  varchar(255) not null,
    discount_condition_id bigint       not null,
    discount_type_id      bigint       not null,
    constraint FK4wlpncf47eceuexgv91uvrx3j
        foreign key (discount_type_id) references discount_type (id),
    constraint FKp6k8vo6h09kbm7kmmfggbg9r3
        foreign key (discount_condition_id) references discount_condition (id)
);

create table coupons
(
    id                 bigint auto_increment
        primary key,
    created_at         datetime(6)  not null,
    description        varchar(255) not null,
    end_at             datetime(6)  not null,
    is_active          bit          not null,
    issued_count       int          not null,
    max_issue_limit    int          not null,
    name               varchar(255) not null,
    start_at           datetime(6)  not null,
    updated_at         datetime(6)  not null,
    valid_days         int          not null,
    discount_policy_id bigint       not null,
    constraint FK52p9s2chetade6tjgdj63ssd6
        foreign key (discount_policy_id) references discount_policy (id)
);

create table orders
(
    id                bigint auto_increment
        primary key,
    created_at        datetime(6)                                                               not null,
    discounted_amount decimal(38, 2)                                                            not null,
    original_total    decimal(38, 2)                                                            not null,
    status            enum ('CANCELED', 'CREATED', 'DELIVERED', 'PAID', 'REFUNDED', 'SHIPPING') not null,
    updated_at        datetime(6)                                                               not null,
    user_id           bigint                                                                    not null
);

create table order_items
(
    id              bigint auto_increment
        primary key,
    discount_amount decimal(38, 2)                                                    not null,
    product_id      bigint                                                            not null,
    quantity        int                                                               not null,
    status          enum ('CANCELED', 'DELIVERED', 'ORDERED', 'REFUNDED', 'SHIPPING') not null,
    unit_price      decimal(38, 2)                                                    not null,
    variant_id      bigint                                                            not null,
    order_id        bigint                                                            not null,
    constraint FKbioxgbv59vetrxe0ejfubep1w
        foreign key (order_id) references orders (id)
);

create table p_sales_agg_day
(
    product_id  bigint not null,
    sales_day   date   not null,
    sales_count bigint not null,
    primary key (product_id, sales_day)
);

create table payments
(
    id                bigint auto_increment
        primary key,
    created_at        datetime(6)                                                          not null,
    updated_at        datetime(6)                                                          not null,
    discounted_amount decimal(38, 2)                                                       not null,
    order_id          bigint                                                               not null,
    original_amount   decimal(38, 2)                                                       not null,
    status            enum ('FAILED', 'PAID', 'PARTIALLY_REFUNDED', 'PENDING', 'REFUNDED') not null,
    timestamp         datetime(6)                                                          null
);

create table payment_item_details
(
    id                bigint auto_increment
        primary key,
    created_at        datetime(6)    not null,
    updated_at        datetime(6)    not null,
    discounted_amount decimal(38, 2) not null,
    order_item_id     bigint         not null,
    original_amount   decimal(38, 2) not null,
    refunded          bit            not null,
    payment_id        bigint         null,
    constraint FKhnvhb1x7mhq8eepnecow5rfcv
        foreign key (payment_id) references payments (id)
);

create table payment_methods
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6)                                                                                                            not null,
    updated_at datetime(6)                                                                                                            not null,
    amount     decimal(38, 2)                                                                                                         not null,
    identifier varchar(255)                                                                                                           null,
    metadata   varchar(255)                                                                                                           null,
    type       enum ('BANK_TRANSFER', 'COUPON', 'CREDIT_CARD', 'DEBIT_CARD', 'DEPOSIT', 'MOBILE_PAYMENT', 'POINT', 'VIRTUAL_ACCOUNT') not null,
    payment_id bigint                                                                                                                 null,
    constraint FKdj7mp2wm87oheypc98jkvojy0
        foreign key (payment_id) references payments (id)
);

create table product_sales_aggregation_daily_checkpoint
(
    id                     bigint auto_increment
        primary key,
    last_aggregated_at     datetime(6) not null,
    last_aggregated_log_id bigint      not null
);

create table product_sales_log
(
    product_sales_log_id bigint auto_increment
        primary key,
    order_id             bigint                  not null,
    product_id           bigint                  not null,
    quantity             bigint                  not null,
    timestamp            datetime(6)             not null,
    type                 enum ('RETURN', 'SOLD') not null,
    variant_id           bigint                  not null
);

create table products
(
    product_id bigint auto_increment
        primary key,
    created_at datetime(6)                                                                                   not null,
    updated_at datetime(6)                                                                                   not null,
    base_price decimal(38, 2)                                                                                not null,
    name       varchar(255)                                                                                  not null,
    status     enum ('DISCONTINUED', 'DRAFT', 'HIDDEN', 'ON_SALE', 'OUT_OF_STOCK', 'PARTIALLY_OUT_OF_STOCK') not null
);

create table option_specs
(
    option_spec_id bigint auto_increment
        primary key,
    created_at     datetime(6)  not null,
    updated_at     datetime(6)  not null,
    display_order  int          not null,
    name           varchar(255) not null,
    product_id     bigint       not null,
    constraint FKl3goqw00kjhw6rwxamaxdbvfa
        foreign key (product_id) references products (product_id)
);

create table option_values
(
    option_value_id bigint auto_increment
        primary key,
    created_at      datetime(6)  not null,
    updated_at      datetime(6)  not null,
    value           varchar(255) not null,
    option_spec_id  bigint       not null,
    constraint FK2t4r3k42u5gbxts169qg1dced
        foreign key (option_spec_id) references option_specs (option_spec_id)
);

create table product_variants
(
    variant_id       bigint auto_increment
        primary key,
    created_at       datetime(6)                                                          not null,
    updated_at       datetime(6)                                                          not null,
    additional_price decimal(38, 2)                                                       not null,
    option_values    varchar(255)                                                         not null,
    status           enum ('ACTIVE', 'DELETED', 'DISCONTINUED', 'HIDDEN', 'OUT_OF_STOCK') not null,
    stock            int                                                                  not null,
    product_id       bigint                                                               not null,
    constraint FKosqitn4s405cynmhb87lkvuau
        foreign key (product_id) references products (product_id)
);

create table user_coupons
(
    id         bigint auto_increment
        primary key,
    expired_at datetime(6)                        not null,
    issued_at  datetime(6)                        not null,
    status     enum ('EXPIRED', 'UNUSED', 'USED') not null,
    used_at    datetime(6)                        null,
    user_id    bigint                             not null,
    coupon_id  bigint                             not null,
    constraint FK9oi3p5xyfe4j32xs54nn7mi20
        foreign key (coupon_id) references coupons (id)
);

create table user_point
(
    id         bigint auto_increment
        primary key,
    balance    decimal(38, 2) not null,
    created_at datetime(6)    not null,
    updated_at datetime(6)    not null,
    user_id    bigint         not null
);

create table user_point_history
(
    id               bigint auto_increment
        primary key,
    amount           decimal(38, 2)         not null,
    created_at       datetime(6)            not null,
    transaction_type enum ('CHARGE', 'USE') not null,
    user_id          bigint                 not null
);
