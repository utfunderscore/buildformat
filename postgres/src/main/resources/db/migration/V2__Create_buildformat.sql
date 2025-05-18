create table buildmeta_format(
    id serial primary key,
    buildmeta_id int not null,
    name varchar(16) not null,
    checksum varchar(64) not null,
    FOREIGN KEY (buildmeta_id) REFERENCES buildmeta(id)
)