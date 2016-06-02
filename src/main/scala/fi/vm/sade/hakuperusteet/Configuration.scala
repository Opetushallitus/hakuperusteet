package fi.vm.sade.hakuperusteet

import java.io.File
import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.scalaproperties.OphProperties

// note: There is also Configuration object in test. if you add a field here your tests might fail
object Configuration extends LazyLogging {

  val props = ConfigFactory
    .parseFile(new File(sys.props.getOrElse("hakuperusteet.properties", "")))
    .withFallback(ConfigFactory.parseResources("reference.conf"))
    .resolve
}


object Urls {
  val urls = initUrls()

  def initUrls() = {
    new OphProperties()
      .addFiles("/hakuperusteet-oph.properties")
      .addOptionalFiles(Paths.get(sys.props.getOrElse("user.home", ""), "/oph-configuration/common.properties").toString)
      .addDefault("baseUrl", Configuration.props.getString("host.lb"))
  }
}