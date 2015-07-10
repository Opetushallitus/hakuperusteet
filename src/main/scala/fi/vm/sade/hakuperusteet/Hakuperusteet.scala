package fi.vm.sade.hakuperusteet

import com.typesafe.config.ConfigFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener


object Hakuperusteet {
  def main(args: Array[String]) {
    val port = ConfigFactory.load().getInt("port")
    val server = new Server(port)
    val context = new WebAppContext()
    context setContextPath ("/hakuperusteet/")
    context.setResourceBase(getClass.getClassLoader.getResource("webapp").toExternalForm)
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    server.setHandler(context)

    server.start
    server.join
  }
}
