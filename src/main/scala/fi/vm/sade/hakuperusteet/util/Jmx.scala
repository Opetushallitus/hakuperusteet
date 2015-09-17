package fi.vm.sade.hakuperusteet.util

import java.lang.management.ManagementFactory
import java.rmi.registry.LocateRegistry
import javax.management.remote.{JMXConnectorServerFactory, JMXServiceURL}

import com.typesafe.config.Config

import scala.collection.JavaConversions._

class Jmx(port: Int) {
  LocateRegistry.createRegistry(port)
  val env = Map.empty[String, Any]
  val s = JMXConnectorServerFactory.newJMXConnectorServer(
    new JMXServiceURL("rmi", null, port, s"/jndi/rmi://localhost:$port/jmxrmi"),
    env,
    ManagementFactory.getPlatformMBeanServer)
  s.start()
}

object Jmx {
  def init(c: Config) = new Jmx(c.getInt("jmx.port"))
}
