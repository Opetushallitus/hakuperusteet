package fi.vm.sade.hakuperusteet

import com.typesafe.scalalogging.LazyLogging
import ru.yandex.qatools.embed.postgresql.{PostgresStarter}
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig

object EmbeddedPostgreSql extends LazyLogging {

  private val runtime = PostgresStarter.getDefaultInstance()
  private val config = PostgresConfig.defaultWithDbName("test", "oph", "test")
  private val exec = runtime.prepare(config)
  private val process = exec.start()
  private val dbUrl = s"jdbc:postgresql://${config.net().host()}:${config.net().port()}/${config.storage().dbName()}"
  private val user = s"${config.credentials().username()}"
  private val password = s"${config.credentials().password()}"
  def configAsMap = {
    logger.info("Using embedded postgresql: " + dbUrl)
    Map("hakuperusteet.db.url"-> dbUrl, "hakuperusteet.db.user" -> user, "hakuperusteet.db.password" -> password)
  }

  def stop = {
    process.stop
    exec.stop()
  }
}
