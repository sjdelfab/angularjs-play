create user myapp with password 'myapp';
create tablespace myapptb owner myapp location '/var/lib/postgresql/data';
create database myapp owner=myapp tablespace=myapptb;
