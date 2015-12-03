# hakuperusteet

## Setup

### Requirements
* JDK 1.8
* node

### Local Postgres setup

MacOS users install docker with command `brew cask install dockertoolbox`.

1. Create new docker-machine `docker-machine create --driver virtualbox dockerVM`
2. Run `docker-machine env dockerVM` and check DOCKER_HOST variable
4. Edit /etc/hosts. Add line `<docker-host-ip-goes-here> hakuperusteetdb`
5. `eval "$(docker-machine env dockerVM)"`
6. `docker run -d -p 5432:5432 postgres`
7. `psql -hhakuperusteetdb -p5432 -Upostgres postgres -c "CREATE ROLE OPH;"`
8. `psql -hhakuperusteetdb -p5432 -Upostgres postgres -c "CREATE DATABASE hakuperusteet;"`
9. `psql -hhakuperusteetdb -p5432 -Upostgres postgres -c "CREATE DATABASE hakuperusteettest;"`

To start docker again (e.g. after boot), run the following command and continue from step 2 above.

1. `docker-machine start dockerVM`

### Run

To start hakuperusteet after Postgres is up, run the following commands:

1. `npm install`
2. `./sbt run`
3. Access hakuperusteet at [https://localhost:18080/hakuperusteet/](https://localhost:18080/hakuperusteet/)
4. `npm run watch` on separate console to enable front auto compile

By default hakuperusteet uses services from Luokka-environment.

You can proceed to the form page by signing in with Google (the blue button).

To start hakuperusteet-admin, use test configuration below.

### Run using test configuration

Test configuration does not have any external dependencies (except Google Authentication, but email one can be used).
This setup needs a running mock server, which should be installed with following commands first:

1. `cd mockserver`
2. `npm install`

To run hakuperusteet or hakuperusteet-admin, run the following commands:

`./sbt "test:run-main fi.vm.sade.hakuperusteet.HakuperusteetTestServer"`

Test servers can be accessed from urls:

1. Access hakuperusteet at [https://localhost:18081/hakuperusteet/](https://localhost:18081/hakuperusteet/)
2. Access hakuperusteet-admin at [https://localhost:18091/hakuperusteetadmin/](https://localhost:18091/hakuperusteetadmin/)

By default test setup uses database from Docker. Embedded Postgres can be used with embedded=true env variable. 

Docker database is empty at start, if needed, create test users by running at project root:

`npm run admin:test-ui`

## Configuration

This project has multiple configuration files, which are used for following purposes.

### src/main/resources/reference.conf

 - Development time configuration file, which uses luokka-environment

### src/main/resources/oph-configuration/hakuperusteet.properties.template

 - This file is the configuration template used with real environments.

### src/test/resources/reference.conf

 - This file is used during unit and UI-tests, uses mock server and Postgres. Both mock server and Posgres has different ports
   than in reference.conf above. Unit tests do not use mock server, hence their port numbers are irrelevant.

## Build

To create assembly jars (app and admin), run the following commands

1. `npm install`
2. `./sbt assembly`
3. `./sbt admin:assembly`

## Postgres client classes for Slick

Currently we store generated Postgres-client classes in git, and hence it is not necessary to run this normally.
During development, after schema changes you must regenerate db-classes with command:

`./sbt "run-main fi.vm.sade.hakuperusteet.db.CodeGenerator"`

## UI-tests

1. `./sbt "test:run-main fi.vm.sade.hakuperusteet.HakuperusteetTestServer"`
2. `npm run test-ui`
3. `npm run admin:test-ui`

To run tests in browser, open following url when HakuperusteetTestServer is running

1. [http://localhost:8081/hakuperusteet/spec/testRunner.html](http://localhost:8081/hakuperusteet/spec/testRunner.html)
2. [http://localhost:8091/hakuperusteetadmin/spec/testRunner.html](http://localhost:8091/hakuperusteetadmin/spec/testRunner.html)

## Test urls

* http://localhost:8081/hakuperusteet/ao/1.2.246.562.20.31077988074# <- payment page for AMK