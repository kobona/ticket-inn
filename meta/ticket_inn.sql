CREATE TABLE account (
    uid varchar(50) PRIMARY KEY NOT NULL,
    username varchar(50) NOT NULL,
    password varchar(50) NOT NULL,
    salt varchar(50) NOT NULL,
    add_time integer NOT NULL,
    mod_time integer NOT NULL
);
