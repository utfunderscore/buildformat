create table buildmeta
(
    id serial primary key,
    name varchar(32) not null,
    description varchar(128) not null
);

create unique index UK_build_meta_name on buildmeta(name);