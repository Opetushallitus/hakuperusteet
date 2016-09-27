npm-install:
	npm install
	(cd mockserver && npm install && pwd)

clean:
	./sbt clean

test: npm-install
	./run_all_tests.sh

package: test
	./sbt compile assembly admin:assembly

source-to-image: npm-install
	./sbt compile assembly admin:assembly
	rm -rf target/streams
	rm -rf target/resolution-cache
