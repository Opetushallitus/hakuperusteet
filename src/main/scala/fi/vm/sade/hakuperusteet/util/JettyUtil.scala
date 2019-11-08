package fi.vm.sade.hakuperusteet.util

import java.security.MessageDigest

import ch.qos.logback.access.jetty.RequestLogImpl
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server._
import org.eclipse.jetty.server.session.{JDBCSessionIdManager, JDBCSessionManager}
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.webapp.WebAppContext

object JettyUtil extends LazyLogging {

  def createServerWithContext(portHttp: Int, context: WebAppContext, dbUrl: String, user: String, password: String, secureSessionCookie: Boolean) = {
    val server = new Server()
    server.setHandler(context)
    server.setConnectors(createConnectors(portHttp, server))
    initRequestLog(server)
    configureJDBCSession(context, dbUrl, user, password, server)
    if(secureSessionCookie) {
      setSecureCookieParams(context)
    }
    server
  }

  def initRequestLog(server: Server): Unit = {
    val requestLog = new RequestLogImpl()
    requestLog.setFileName(sys.props.getOrElse("logback.access", "src/main/resources/logback-access.xml"))
    server.setRequestLog(requestLog)
    requestLog.start()
  }

  private def createConnectors(portHttp: Int, server: Server): Array[Connector] = {
    Array(createHttpConnector(portHttp, server))
  }

  private def createHttpConnector(portHttp: Int, server: Server): Connector = {
    val httpConnector = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration))
    httpConnector.setPort(portHttp)
    httpConnector
  }

  def configureJDBCSession(context: WebAppContext, dbUrl: String, user: String, password: String, server: Server): Unit = {
    val idMgr = new JDBCSessionIdManager(server)
    idMgr.setWorkerName(createWorkerName)
    idMgr.setDriverInfo("org.postgresql.Driver", dbUrl + "?user=" + user + "&password=" + password)
    idMgr.setScavengeInterval(600)
    server.setSessionIdManager(idMgr)
    val jdbcMgr = new JDBCSessionManager()
    context.getSessionHandler.setSessionManager(jdbcMgr)
    jdbcMgr.setSessionIdManager(idMgr)
  }

  private def createWorkerName = {
    val serverName = java.net.InetAddress.getLocalHost.getHostName.split("\\.")(0)
    MessageDigest.getInstance("MD5").digest(serverName.getBytes).map("%02x".format(_)).mkString.slice(0, 10)
  }

  def setSecureCookieParams(context: WebAppContext) {
    val sessionCookieConfig = context.getServletContext.getSessionCookieConfig
    sessionCookieConfig.setHttpOnly(true)
    sessionCookieConfig.setSecure(true)
  }
}
