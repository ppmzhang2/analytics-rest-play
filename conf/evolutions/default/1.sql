# --- !Ups

create table "user" (
  "id" bigint generated by default as identity(start with 1) not null primary key,
  "ntid" varchar not null,
  "name" varchar not null,
  "title" varchar not null
);

create table "sql" (
  "id" bigint generated by default as identity(start with 1) not null primary key,
  "usrid" bigint not null,
  "desc" varchar not null,
  "content" varchar not null,
  "stars" int not null,
  "execution" int not null,
  "exportation" int not null,
  "updated" date,
  foreign key ("usrid") references "user"("id") on delete CASCADE
);

create table "star" (
  "sqlid" bigint not null,
  "usrid" bigint not null,
  foreign key ("sqlid") references "sql"("id") on delete CASCADE,
  foreign key ("usrid") references "user"("id") on delete CASCADE,
  constraint "pkid" primary key ("sqlid", "usrid")
);

create table "comment" (
  "id" bigint generated by default as identity(start with 1) not null primary key,
  "reid" bigint not null,
  "usrid" bigint not null,
  "reusrid" bigint not null,
  "sqlid" bigint not null,
  "content" varchar not null,
  "created" date,
  foreign key ("sqlid") references "sql"("id") on delete CASCADE
);

create table "report" (
  "id" bigint generated by default as identity(start with 1) not null primary key,
  "sqlid" bigint not null,
  "data" varchar,
  "chart" varchar,
  foreign key ("sqlid") references "sql"("id") on delete CASCADE
);

CREATE ALIAS IF NOT EXISTS FT_INIT FOR "org.h2.fulltext.FullText.init";
CALL FT_INIT();
CALL FT_CREATE_INDEX('PUBLIC', 'sql', 'desc,content');

# --- !Downs

CALL FT_DROP_INDEX('PUBLIC', 'sql');
drop table if exists "report" cascade;
drop table if exists "comment" cascade;
drop table if exists "star" cascade;
drop table if exists "sql" cascade;
drop table if exists "user" cascade;
