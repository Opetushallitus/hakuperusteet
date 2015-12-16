package fi.vm.sade.hakuperusteet

import java.io.File
import java.net.InetSocketAddress
import java.sql.{Connection, DriverManager}

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.{ApplicationObjects, Payments, Users}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.sys.process.Process

class HakuperusteetTestServer extends HakuperusteetServer {
  override def secureSessionCookie = false
}

class HakuperusteetTestAdminServer extends HakuperusteetAdminServer {
  override def secureSessionCookie = false
}

object HakuperusteetTestServer {
  val logger = LoggerFactory.getLogger(this.getClass)
  val isEmbeddedConfig = System.getProperty("embedded", "false") == "true"

  /*
   * ./sbt "test:run-main fi.vm.sade.hakuperusteet.HakuperusteetTestServer"
   */
  def main(args: Array[String]): Unit = {
    startMockServer()
    DBSupport.ensureEmbeddedIsStartedIfNeeded()
    startCommandServer()
    val server: HakuperusteetTestServer = new HakuperusteetTestServer()
    val adminServer: HakuperusteetTestAdminServer = new HakuperusteetTestAdminServer()
    logger.info("Starting HakuperusteetServer (http: " + server.portHttp + " https: " + server.portHttps + ")")
    logger.info("Starting HakuperusteetAdminServer (http: " + adminServer.portHttp + " https: " + adminServer.portHttps + ")")
    new Thread() {
      override def run() = {
        Thread.sleep(2500) // Jetty's JDBC Session creates its database tables automatically. This wait prevents race condition between the two servers
        adminServer.runServer()
      }
    }.start()
    server.runServer()
  }

  def startMockServer() {
    logger.info("Starting mockserver (http: 3001 ldap: 1390)")
    val pb = Process(Seq("node", "server.js"), new File("./mockserver/"), "PORT" -> "3001", "LDAP_PORT" -> "1390")
    val started = pb.run()
    sys addShutdownHook {
      started.destroy()
    }
  }

  private def startCommandServer() {
    logger.info("Starting CommandServer (http: 8000)")
    val server = HttpServer.create(new InetSocketAddress(8000), 0)
    server.createContext("/testoperation/reset", new ResetDatabase())
    server.createContext("/testoperation/resetAdmin", new ResetDatabaseForAdminTests())
    server.setExecutor(null)
    server.start
  }

  private def getTables(jdbcConnection: Connection) = {
    val tables = new ListBuffer[String]()
    val dbmd = jdbcConnection.getMetaData()
    val rs = dbmd.getTables(null, "public", "%", Array("TABLE"))
    while (rs.next()) {
      tables += rs.getString("TABLE_NAME")
    }
    tables.toList
  }

  def cleanDB(): Unit = {
    val config = Configuration.props
    val url = config.getString("hakuperusteet.db.url")
    val user = config.getString("hakuperusteet.db.user")
    val password = config.getString("hakuperusteet.db.password")
    val jdbcConnection = DriverManager.getConnection(url, user, password)
    try {
      val tables = getTables(jdbcConnection)
      Array("synchronization", "application_object", "payment_event", "payment", "user_details", "user" ,"jettysessionids","jettysessions")
        .foreach(name => if(tables.contains(name)){
          jdbcConnection.createStatement.execute("DELETE from \"" + name + "\";")
        })
    } catch {
      case e: Exception => {
        e.printStackTrace()
      }
    } finally {
      jdbcConnection.close()
    }
  }
}

class ResetDatabase() extends HttpHandler {
  override def handle(t: HttpExchange) = {
    HakuperusteetTestServer.cleanDB()
    okResponse(t)
  }

  def okResponse(t: HttpExchange): Unit = {
    val response = "OK"
    t.getResponseHeaders.add("Access-Control-Allow-Origin", "*")
    t.sendResponseHeaders(200, response.length)
    val os = t.getResponseBody
    os.write(response.getBytes)
    os.close()
  }
}

class ResetDatabaseForAdminTests extends ResetDatabase {
  override def handle(t: HttpExchange) = {
    super.handle(t)
    populateUsers
  }

  private def populateUsers: Unit = {
    val db = HakuperusteetDatabase(Configuration.props)
    val userAndApplication = Users.generateUsers.map(u =>
      (u, ApplicationObjects.generateApplicationObject(u), Payments.generatePayments(u, List("Maksuton"))))

    userAndApplication.foreach { case (user, applicationObjects, payments) =>
      val u = db.findUser(user.email)
      if (u.isEmpty) {
        db.upsertUser(user)
        applicationObjects.foreach(ao => db.run(db.upsertApplicationObject(ao), 10 seconds))
        payments.foreach(db.upsertPayment)
      }
    }
  }
}
