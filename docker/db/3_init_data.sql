\c myapp;

set role myapp;

insert into application_user(name,email,password,enabled) values ('Simon Smith','simon@email.com','$2a$10$o.mZ7HgHibj8qJNPCIgxtezvt4G93DxlrKLvCtF0b.GrbebkF3JqO',true);

insert into application_role_membership(user_id,role_type) values(1,'admin'); 
