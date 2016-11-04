#!/usr/bin/env bash

set -e

function killTestServer {
  if [ -n "$PID" ]; then
    kill -SIGTERM $PID;
    sleep 5
  fi
  PID=""
}

function finish {
  killTestServer
  echo "********************* Finished run_all_tests.sh"
}

trap finish EXIT

echo "********************* Running all hakuperuste tests"

echo "********************* npm install for hakuperusteet"
npm install

echo "********************* npm install for hakuperusteet mockserver"
(cd mockserver && npm install && pwd)

echo "********************* ./sbt test"

./sbt -batch clean compile admin:compile test -J-Dembedded=true

echo "********************* Starting test servers"
./sbt -batch "test:run-main fi.vm.sade.hakuperusteet.HakuperusteetTestServer" -J-Dembedded=true &
PID=$!
while ! nc -z localhost 8081; do
  sleep 1
done

echo "********************* npm run test-ui-junit"
npm run test-ui-junit

echo "********************* npm run admin:test-ui-junit"
npm run admin:test-ui-junit

killTestServer
echo "********************* ALL TESTS OK!"