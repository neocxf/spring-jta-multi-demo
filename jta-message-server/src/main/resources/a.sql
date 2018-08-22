drop table if exists USER;

create table USER (
  id varchar(255) not null unique,
  name varchar(255) not null
);