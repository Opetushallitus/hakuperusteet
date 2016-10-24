#!/usr/bin/env bash
eval "$(docker-machine env dockerVM)"
docker run -d -p 5432:5432 postgres
while ! echo exit | nc hakuperusteetdb 5432; do sleep 10; done
psql -hhakuperusteetdb -p5432 -Upostgres postgres -c "CREATE ROLE OPH;"
psql -hhakuperusteetdb -p5432 -Upostgres postgres -c "CREATE DATABASE hakuperusteet;"
psql -hhakuperusteetdb -p5432 -Upostgres postgres -c "CREATE DATABASE hakuperusteettest;"
