create table application_user(
  id SERIAL NOT NULL PRIMARY KEY,
  name varchar(254) not null,
  email varchar(254) not null,
  password varchar(127) not null,
  enabled boolean not null default false,
  failed_login_attempts int not null default 0
);

ALTER TABLE application_user ADD CONSTRAINT users_email UNIQUE(email);

create type user_role_type as enum('admin','resource_manager');

create table application_role_membership(
   user_id int not null REFERENCES application_user(id),
   role_type user_role_type not null
);

ALTER TABLE application_role_membership ADD CONSTRAINT users_role_membership UNIQUE(user_id,role_type);

