create table notifications (
    id            uuid         not null,
    recipient_id  uuid         not null,
    type          varchar(40)  not null,
    title         varchar(120) not null,
    body          varchar(500) not null,
    reference_id  uuid,
    event_id      uuid         not null,
    created_at    timestamp with time zone not null,
    read_at       timestamp with time zone,
    constraint pk_notifications primary key (id)
);

create index idx_notifications_recipient_created on notifications (recipient_id, created_at desc);
create index idx_notifications_recipient_unread on notifications (recipient_id) where read_at is null;

create table processed_events (
    event_id      uuid        not null,
    event_type    varchar(60) not null,
    processed_at  timestamp with time zone not null,
    constraint pk_processed_events primary key (event_id)
);

create table devices (
    id            uuid         not null,
    user_id       uuid         not null,
    push_token    varchar(200) not null,
    platform      varchar(20)  not null,
    updated_at    timestamp with time zone not null,
    constraint pk_devices primary key (id),
    constraint uk_devices_push_token unique (push_token)
);

create index idx_devices_user on devices (user_id);
