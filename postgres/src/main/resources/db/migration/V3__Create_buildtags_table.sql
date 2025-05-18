create table buildmeta_tags
(
    id serial primary key,
    buildmeta_id int not null,
    tag varchar(32) not null,
    FOREIGN KEY (buildmeta_id) REFERENCES buildmeta(id)
);

