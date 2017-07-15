FROM postgres:latest
ENV POSTGRES_PASSWORD myFooBarPassword
COPY 1_create_db.sql /docker-entrypoint-initdb.d/
COPY 2_schema.sql /docker-entrypoint-initdb.d/
COPY 3_init_data.sql /docker-entrypoint-initdb.d/
