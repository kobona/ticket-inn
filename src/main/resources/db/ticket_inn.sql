# https://www.sqlite.org/lang_createtable.html

CREATE TABLE account (
    uid VARCHAR(50) PRIMARY KEY NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL,
    salt VARCHAR(50) NOT NULL,
    state INTEGER NOT NULL,
    roles VARCHAR(500) NOT NULL,
    permissions VARCHAR(500) NOT NULL,
    create_date INTEGER NOT NULL,
    update_date INTEGER NOT NULL
);

CREATE TABLE show (
    show_id     INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    show_name   VARCHAR(50) NOT NULL,
    start_time  INTEGER NOT NULL,
    create_date INTEGER NOT NULL,
    update_date INTEGER NOT NULL
);

CREATE TABLE ticket (
    ticket_id     INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    show_id       INTEGER NOT NULL,
    ticket_name   VARCHAR(50) NOT NULL,
    ticket_state  INTEGER NOT NULL,
    create_date INTEGER NOT NULL,
    update_date INTEGER NOT NULL
);

CREATE TABLE ticket_stamp (
    uid         VARCHAR(50) NOT NULL,
    ticket_id   INTEGER KEY NOT NULL,
    ticket_name VARCHAR(50) NOT NULL,
    create_date INTEGER NOT NULL,
    update_date INTEGER NOT NULL,
    UNIQUE(uid, ticket_id)
);

CREATE TABLE object_slot (
	obj_id    INTEGER PRIMARY KEY NOT NULL,
	obj_name  VARCHAR(50) NOT NULL,
	obj_mime  VARCHAR(50) NOT NULL,
	create_date INTEGER NOT NULL,
    update_date INTEGER NOT NULL
);

CREATE TABLE history (
    uid    VARCHAR(50) NOT NULL,
    ref_id INTEGER DEFAULT NULL,
    op_log VARCHAR(500) NOT NULL,
    create_date INTEGER NOT NULL
);

