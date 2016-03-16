package fi.vm.sade.hakuperusteet

import java.io.File

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

// note: There is also Configuration object in test. if you add a field here your tests might fail
object Configuration extends LazyLogging {

  val props = ConfigFactory
    .parseFile(new File(sys.props.getOrElse("hakuperusteet.properties","")))
    .withFallback(ConfigFactory.parseResources("reference.conf"))
    .resolve
}
