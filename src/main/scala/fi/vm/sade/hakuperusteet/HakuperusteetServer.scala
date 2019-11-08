package fi.vm.sade.hakuperusteet

import fi.vm.sade.hakuperusteet.Configuration._
import fi.vm.sade.hakuperusteet.HakuperusteetServer._
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.util.JettyUtil
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.util.resource.ResourceCollection
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.slf4j.LoggerFactory

class HakuperusteetServer {
  def portHttp = props.getInt("hakuperusteet.port.http")
  def secureSessionCookie = true

  def runServer() {
    val dbUrl = props.getString("hakuperusteet.db.url")
    val user = props.getString("hakuperusteet.db.user")
    val password = props.getString("hakuperusteet.db.password")
    HakuperusteetDatabase(props)
    val context: WebAppContext = createContext
    val server = JettyUtil.createServerWithContext(portHttp, context, dbUrl, user, password, secureSessionCookie)
    server.start()
    server.join()
    logger.info(s"Using ports $portHttp")
  }

  def createContext = {
    val context = new WebAppContext()
    val resources = new ResourceCollection(Array(
      getClass.getClassLoader.getResource("webapp-common").toExternalForm,
      getClass.getClassLoader.getResource("webapp").toExternalForm
    ))
    context.setContextPath("/hakuperusteet/")
    context.setBaseResource(resources)
    context.setInitParameter(ScalatraListener.LifeCycleKey, classOf[ScalatraBootstrap].getCanonicalName)
    context.setInitParameter(org.scalatra.EnvironmentKey, "production")
    context.setInitParameter(org.scalatra.CorsSupport.EnableKey, "false")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context
  }
}

object HakuperusteetServer {
  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val s = new HakuperusteetServer
    s.runServer()
    logger.info("Started HakuperusteetServer")
  }
}
