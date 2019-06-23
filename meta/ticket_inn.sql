CREATE TABLE account (
    uid varchar(50) PRIMARY KEY NOT NULL,
    username varchar(50) NOT NULL,
    password varchar(50) NOT NULL,
    salt varchar(50) NOT NULL,
    roles varchar(500) NOT NULL,
    permissions varchar(500) NOT NULL,
    create_date integer NOT NULL,
    update_date integer NOT NULL
);

CREATE TABLE show (
    show_id bigint PRIMARY KEY AUTOINCREMENT NOT NULL,
    show_name varchar(50) NOT NULL,
    start_time integer NOT NULL,
    create_date integer NOT NULL,
    update_date integer NOT NULL
);

CREATE TABLE ticket (
    ticket_id bigint PRIMARY KEY AUTOINCREMENT NOT NULL,
    show_id bigint NOT NULL,
    ticket_name varchar(50) NOT NULL,
    ticket_status tinyint NOT NULL,
    create_date integer NOT NULL,
    update_date integer NOT NULL,
);

CREATE TABLE ticket_stamp (
    uid varchar(50) NOT NULL,
    ticket_id bigint NOT NULL,
    ticket_name varchar(50) NOT NULL,
    create_date integer NOT NULL,
    update_date integer NOT NULL,
    UNIQUE(uid, ticket)
);

CREATE TABLE attachment (
	attach_id    bigint        PRIMARY KEY,
	attach_uri   VARCHAR(255)  NOT NULL,
	attach_path  VARCHAR(255)  NOT NULL,
	attach_mime  VARCHAR(50)   NOT NULL,
	attach_code  INT8          NOT NULL
);

CREATE TABLE history (
    uid varchar(50) NOT NULL,
    ref_id bigint DEFAULT NULL,
    op_log varchar(500) NOT NULL,
    create_date integer NOT NULL
);

CREATE TABLE content (
	content_id    INT8    PRIMARY KEY,
	content_text  TEXT    NOT NULL DEFAULT ''
);

CREATE TABLE attribute (
	attr_id    INT8         PRIMARY KEY,
	attr_type  INT2         NOT NULL DEFAULT 0,
	attr_sort  INT2         NOT NULL DEFAULT 0,
	attr_name  VARCHAR(255) NOT NULL
);

CREATE TABLE attr_ref (
	attr_ref_id     INT8  PRIMARY KEY,
	goods_id        INT8  NOT NULL,
	attr_val_id     INT8  NOT NULL DEFAULT 0,
	attr_val_st     INT8  NOT NULL DEFAULT 1
);

CREATE TABLE attr_val (
	attr_val_id      INT8         PRIMARY KEY,
	attr_id          INT8         NOT NULL ,
	attr_val_sort    INT2         NOT NULL DEFAULT 0,
	attr_val_name  VARCHAR(255)   NOT NULL
);
