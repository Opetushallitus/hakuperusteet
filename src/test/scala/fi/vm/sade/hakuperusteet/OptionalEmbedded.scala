package fi.vm.sade.hakuperusteet

import fi.vm.sade.hakuperusteet.db.{GlobalExecutionContext, HakuperusteetDatabase}
import fi.vm.sade.hakuperusteet.util.ConfigUtil

object OptionalEmbeddedDB {
  val embeddedDB = initEmbedded
  def initEmbedded = {
    if(HakuperusteetTestServer.isEmbeddedConfig) {
      ConfigUtil.writeConfigFile(EmbeddedPostgreSql.configAsMap)
      EmbeddedPostgreSql.startEmbeddedPostgreSql
      val config = Configuration.props
      HakuperusteetDatabase.init(config, GlobalExecutionContext.asyncExecutor)
    }
  }
}

trait OptionalEmbeddedDB {
  OptionalEmbeddedDB.embeddedDB
}