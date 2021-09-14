create table balance_sheet
(
    id uuid not null
        constraint balance_sheet_pkey
            primary key
        constraint balance_sheet_unique_id
            unique,
    created_ts timestamp with time zone,
    currency_code varchar(3)
);

comment on column balance_sheet.currency_code is 'Код базовой валюты';

alter table balance_sheet owner to postgres;

create table account
(
    id uuid not null
        constraint account_pkey
            primary key,
    balance_sheet_id uuid not null
        constraint account_balance_sheet_id_fk
            references balance_sheet,
    name varchar(100),
    created_date date,
    type varchar(12),
    is_arc boolean default false not null,
    dtype varchar(31)
);

alter table account owner to postgres;

create index account_balance_sheet_id_fki
    on account (balance_sheet_id);

create index account_balance_sheet_id_type_idx
    on account (balance_sheet_id, type);

create index account_id_type_idx
    on account (id, type);

create table balance
(
    id uuid not null
        constraint balance_pkey
            primary key
        constraint balance_id_fk
            references account,
    reserve_id uuid,
    credit_limit numeric(19),
    min_value numeric(19,2),
    num bigint,
    value numeric(19,2) not null,
    currency_code varchar(3) not null,
    balance_sheet_id uuid
        constraint balance_balance_sheet_id_fk
            references balance_sheet
);

alter table balance owner to postgres;

create index balance_id_fki
    on balance (id);

create index balance_reserve_id_fki
    on balance (reserve_id);

create table exchange_rate
(
    id varchar(6) not null
        constraint exchange_rate_pkey
            primary key,
    name varchar(7) not null,
    date date not null,
    ask numeric(19,4) not null,
    bid numeric(19,4)
);

comment on column exchange_rate.id is 'Идентификатор пары валюты';

comment on column exchange_rate.ask is 'Курс спроса';

comment on column exchange_rate.bid is 'Курс предложения';

alter table exchange_rate owner to postgres;

create index exchange_rate_first_cry_date_idx
    on exchange_rate ("substring"(id::text, 1, 3), date);

create table tag
(
    id uuid not null
        constraint tag_pkey
            primary key,
    name varchar(100),
    balance_sheet_id uuid not null
        constraint tag_balance_sheet_id_fk
            references balance_sheet,
    is_category boolean,
    root_id uuid
        constraint tag_root_id_fk
            references tag,
    is_arc boolean,
    cat_type varchar(16),
    constraint tag_balance_sheet_id_name_unique
        unique (balance_sheet_id, name)
);

alter table tag owner to postgres;

create index tag_root_id_fki
    on tag (root_id);

create table tag2obj
(
    tag_id uuid not null
        constraint tag2obj_tag_id_tag_id_fk
            references tag,
    obj_id uuid not null,
    obj_type varchar(15),
    constraint tag2obj_pkey
        primary key (tag_id, obj_id)
);

comment on column tag2obj.obj_type is 'Тип объекта, заданного obj_id';

alter table tag2obj owner to postgres;

create table money_oper
(
    id uuid not null
        constraint money_oper_pkey
            primary key,
    balance_sheet_id uuid not null
        constraint money_oper_bs_id_fk
            references balance_sheet,
    created_ts timestamp with time zone not null,
    trn_date date,
    date_num integer,
    comment varchar(255),
    parent_id uuid
        constraint money_oper_parent_id_fk
            references money_oper (id),
    period varchar(8) not null,
    status varchar(10) not null,
    recurrence_id uuid
);

comment on column money_oper.status is 'Статус операции: pending - в ожидании, done - выполнен, cancelled - отменен.';

comment on column money_oper.recurrence_id is 'Идентификатор повторяющейся операции';

alter table money_oper owner to postgres;

create index money_oper_bs_id_trn_date_status_idx
    on money_oper (balance_sheet_id, trn_date, status);

create index money_oper_parent_id_fki
    on money_oper (parent_id);

create table money_oper_item
(
    id uuid not null
        constraint money_oper_item_pkey
            primary key,
    oper_id uuid not null
        constraint money_oper_item_oper_id_fk
            references money_oper (id),
    balance_id uuid not null
        constraint money_oper_item_balance_id_fk
            references balance (id),
    value numeric(19,2) not null,
    performed date,
    index integer,
    bs_id uuid not null
        constraint money_oper_item_bs_id_fk
            references balance_sheet (id)
);

comment on column money_oper_item.oper_id is 'Идентификатор операции';

comment on column money_oper_item.balance_id is 'Идентификатор остатка (баланса)';

comment on column money_oper_item.performed is 'Дата изменения';

comment on column money_oper_item.index is 'Порядковый номер';

alter table money_oper_item owner to postgres;

create index money_oper_item_bs_id_fki
    on money_oper_item (bs_id);

create index money_oper_item_oper_id_idx
    on money_oper_item (oper_id);

create index money_oper_item_balance_id_fki
    on money_oper_item (balance_id);

create table recurrence_oper
(
    id uuid not null
        constraint recurrence_oper_pkey
            primary key,
    template_id uuid not null
        constraint recurrence_oper_template_id_fk
            references money_oper,
    next_date date not null,
    balance_sheet_id uuid not null
        constraint recurrence_oper_balance_sheet_id_fk
            references balance_sheet,
    is_arc boolean
);

alter table recurrence_oper owner to postgres;

create index recurrence_oper_balance_sheet_id_fki
    on recurrence_oper (balance_sheet_id);

create index recurrence_template_id_fki
    on recurrence_oper (template_id);

create table reserve
(
    id uuid not null
        constraint reserve_pkey
            primary key
        constraint reserve_id_fk
            references balance,
    target numeric(19,2) not null
);

alter table reserve owner to postgres;

alter table balance
    add constraint balance_reserve_id_fk
        foreign key (reserve_id) references reserve (id);

create index reserve_id_fki
    on reserve (id);

create table app_user
(
    id uuid not null
        constraint app_user_pkey
            primary key,
    email text not null
        constraint app_user_email_unique
            unique,
    pwd_hash text not null,
    balance_sheet_id uuid not null
        constraint app_user_balance_sheet_id_fk
            references balance_sheet
);

alter table app_user owner to postgres;

create index app_user_balance_sheet_id_fki
    on app_user (balance_sheet_id);

create unique index app_user_id_balance_sheet_id_idx
    on app_user (id, balance_sheet_id);

create view v_crnt_saldo_by_base_cry(balance_sheet_id, type, saldo) as
SELECT a.balance_sheet_id,
       a.type,
       round(b.value * COALESCE(er.ask, 1::numeric), 2) AS saldo
FROM account a,
     balance b
         LEFT JOIN exchange_rate er
                   ON "substring"(er.id::text, 1, 3) = b.currency_code::text AND er.date = ((SELECT max(er1.date) AS max
                                                                                             FROM exchange_rate er1
                                                                                             WHERE "substring"(er1.id::text, 1, 3) = b.currency_code::text))
WHERE (a.type::text = ANY
       (ARRAY ['debit'::character varying::text, 'credit'::character varying::text, 'reserve'::character varying::text, 'asset'::character varying::text]))
  AND b.id = a.id;

comment on view v_crnt_saldo_by_base_cry is 'Текущие балансы в базовой валюте';

alter table v_crnt_saldo_by_base_cry owner to postgres;
