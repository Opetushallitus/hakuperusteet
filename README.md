# hakuperusteet

Hakemuksen käsittelymaksun maksumuuri. Jos hakijan pohjakoulutus on muun kuin EU- ja ETA-maiden tai Sveitsin
koulutusjärjestelmän mukainen hakijan pitää maksaa käsittelymaksu jotta hakemus käsitellään.

Projekti sisältää hakijan maksu-käyttöliittymän ja ylläpitäjän hallintakäyttöliittymän.

## Setup

### Requirements
* JDK 1.8
* node

### Local Postgres setup

MacOS users install docker with command `brew cask install dockertoolbox`.

1. Create new docker-machine `docker-machine create --driver virtualbox dockerVM`
2. Run `docker-machine env dockerVM` and check DOCKER_HOST variable
3. Edit /etc/hosts. Add line `<docker-host-ip-goes-here> hakuperusteetdb`
4. `. init_docker_postgres.sh`

To start docker again (e.g. after boot), run the following commands:

1. `docker-machine start dockerVM`
2. `. init_docker_postgres.sh`

### Run

To start hakuperusteet after Postgres is up, run the following commands:

1. create local conf file. containing line: hakuperusteet.password="addpwd"
2. `npm install`
3. `npm run dev-build`
4. `./sbt -v -Dhakuperusteet.properties=pathtolocalconf run`
5. Access hakuperusteet at [https://localhost:18080/hakuperusteet/](https://localhost:18080/hakuperusteet/)
6. `npm run watch` on separate console to enable front auto compile

By default hakuperusteet uses services from QA-environment.

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

Mac users may have to install phantomjs locally and point the scripts use local phantom:
`mocha-phantomjs -p /usr/local/bin/phantomjs --ignore-resource-errors --setting webSecurityEnabled=false http://localhost:8091/hakuperusteetadmin/spec/testRunner.html`

## Configuration

This project has multiple configuration files, which are used for following purposes.

### src/main/resources/reference.conf

 - Development time configuration file, which uses QA-environment

### src/main/resources/oph-configuration/hakuperusteet.properties.template

 - This file is the configuration template used with real environments.

### src/test/resources/reference.conf

 - This file is used during unit and UI-tests, uses mock server and Postgres. Both mock server and Posgres has different ports
   than in reference.conf above. Unit tests do not use mock server, hence their port numbers are irrelevant.

## Build

To create assembly jars (app and admin), run

    make package

## Postgres client classes for Slick

Currently we store generated Postgres-client classes in git, and hence it is not necessary to run this normally.
During development, after schema changes you must regenerate db-classes with command:

`./sbt "run-main fi.vm.sade.hakuperusteet.db.CodeGenerator"`

## UI-tests

You must have run `npm install` under `/mockserver` before running the UI tests and `npm run dev-build` in project root.

1. `./sbt "test:run-main fi.vm.sade.hakuperusteet.HakuperusteetTestServer"`
2. `npm run test-ui`
3. `npm run admin:test-ui`

To run tests in browser, open following url when HakuperusteetTestServer is running

1. [http://localhost:8081/hakuperusteet/spec/testRunner.html](http://localhost:8081/hakuperusteet/spec/testRunner.html)
2. [http://localhost:8091/hakuperusteetadmin/spec/testRunner.html](http://localhost:8091/hakuperusteetadmin/spec/testRunner.html)

## Test urls

* http://localhost:8081/hakuperusteet/ao/1.2.246.562.20.31077988074# <- payment page for AMK
