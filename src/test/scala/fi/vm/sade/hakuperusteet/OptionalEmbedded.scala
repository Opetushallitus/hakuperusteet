package fi.vm.sade.hakuperusteet

import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.util.ConfigUtil

object CleanShutdown {
  private var embedded=false;
  private val scheduled = addShutdownHook

  private def shutdown = {
    HakuperusteetDatabase.close
    if(embedded) {
      EmbeddedPostgreSql.stop
    }
  }

  def useEmbeddedPostgres() = {
    embedded = true
  }

  def scheduleShutdown() = {
    scheduled
  }

  private def addShutdownHook = {
    sys addShutdownHook {
      shutdown
    }
  }
}

object DBSupport {
  private val embeddedDB = initEmbedded
  private def initEmbedded = {
    if(HakuperusteetTestServer.isEmbeddedConfig) {
      ConfigUtil.writeConfigFile(EmbeddedPostgreSql.configAsMap)
      CleanShutdown.useEmbeddedPostgres()
      val config = Configuration.props
      HakuperusteetDatabase(config)
    }
    true
  }
  def ensureEmbeddedIsStartedIfNeeded() = embeddedDB
}

trait DBSupport {
  CleanShutdown.scheduleShutdown()
  DBSupport.ensureEmbeddedIsStartedIfNeeded()
}
