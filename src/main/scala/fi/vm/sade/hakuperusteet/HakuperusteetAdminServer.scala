package fi.vm.sade.hakuperusteet

import fi.vm.sade.hakuperusteet.Configuration._
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.util.resource.ResourceCollection
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.slf4j.LoggerFactory

class HakuperusteetAdminServer extends HakuperusteetServer {

  override def portHttp = props.getInt("hakuperusteetadmin.port.http")
  override def portHttps = Option(props.getInt("hakuperusteetadmin.port.https")).find(_ != -1)

  override def createContext = {
    val resources = new ResourceCollection(Array(
      getClass.getClassLoader.getResource("webapp-common").toExternalForm,
      getClass.getClassLoader.getResource("webapp-admin").toExternalForm,
      getClass.getClassLoader.getResource("META-INF/resources/webjars").toExternalForm
      ))

    val context = new WebAppContext()
    context.setContextPath("/hakuperusteetadmin")
    context.setBaseResource(resources)
    context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[ScalatraAdminBootstrap].getCanonicalName)
    context.setInitParameter(org.scalatra.EnvironmentKey, "production")
    context.setInitParameter(org.scalatra.CorsSupport.EnableKey, "false")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context
  }
}

/**
 * ./sbt "run-main fi.vm.sade.hakuperusteet.HakuperusteetAdminServer"
 */
object HakuperusteetAdminServer {
  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {
    val s = new HakuperusteetAdminServer
    s.runServer()
    logger.info("Started HakuperusteetAdminServer")
  }
}
