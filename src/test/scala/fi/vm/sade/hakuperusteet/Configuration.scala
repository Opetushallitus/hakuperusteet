package fi.vm.sade.hakuperusteet

import java.io.File

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging


/*
 * This file is used during tests (sbt/scala feature/hack/kludge) and it overriders actual Configuration.scala
 */
object Configuration extends LazyLogging {

  val conffile = File.createTempFile("reference",".conf")

  logger.info("Using Configuration from test classpath!")

  lazy val props = ConfigFactory
    .parseFile(conffile)
    .withFallback(ConfigFactory.parseResources("test.conf"))
    .resolve
}
