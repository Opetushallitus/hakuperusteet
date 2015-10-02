# hakuperusteet

## Configuration

As default, the application runs in the port `8080`. This can be changed by
setting the environment variable `PORT`.

## Standalone JAR building
1. `npm install` (first time)
2. `sbt assembly`
or
3. `sbt admin:assembly` (for admin app build)

## Test API

The application serves a simple test form in `/test_form.html`. The form can
be used to call the `/api/v1/test` endpoint to create a redirect to a user
given URL with signed parameters. See the specification of this redirect in
SPEC.md.

The test API uses a RSA keypair to sign the parameters. These keys are
included in the repository in the `testkey.pub.pem` and
`src/main/resources/testkey.pem` files. New keypair can be generated using
openssl as follows.

1. Generate the keypair: `openssl genrsa <number of bits> > key.pem`
2. Create a copy of the keypair in PKCS8 format for easy use from Java:  
   `cat key.pem | openssl pkcs8 -topk8 -inform PEM -outform DER -nocrypt > key.der`
3. Extract the public key from the keypair in PEM and DER formats:  
   `cat key.pem | openssl rsa -pubout -inform PEM -outform PEM > key.pub.pem`  
   `cat key.pem | openssl rsa -pubout -inform PEM -outform DER > key.pub.der`

## Local Postgres setup

MacOS users install docker with command `rew cask install dockertoolbox`. 

1. Create new docker-machine `docker-machine create —-driver virtualbox dockerVM`
2. `eval "$(docker-machine env dockerVM)"`
3. Check DOCKER_HOST variable
4. Edit /etc/hosts. Add line `<docker-host-ip-goes-here> hakuperusteetdb`
5. `docker run -p -d 5432:5432 postgres`
6. `psql -hhakuperusteetdb -p5432 -Upostgres postgres -c "CREATE ROLE OPH;"`
7. `psql -hhakuperusteetdb -p5432 -Upostgres postgres -c "CREATE DATABASE hakuperusteet;"`
8. `psql -hhakuperusteetdb -p5432 -Upostgres postgres -c "CREATE DATABASE hakuperusteettest;"`

To start docker again (e.g. after boot), run the following command and continue from step 2 above. 

1. `docker-machine start dockerVM`

## Create slick-classes

During development, after schema changes you must regenerate db-classes with command:

`./sbt "run-main fi.vm.sade.hakuperusteet.db.CodeGenerator"`

Currently we store generated code in git, and hence it is not necessary to run this normally.

## Run using mock configuration

`sbt run -J-Dmock=true` starts server using mock configuration

## Start mock server

`cd mockserver`
`npm install`
`node server.js`
or use nodemon to auto reload on changes
`npm install -g nodemon`
`nodemon server.js`
