package fi.vm.sade.hakuperusteet.vetuma

import java.io.Serializable
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.CyclicBarrier

import com.netaporter.uri.Uri._
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import com.typesafe.config.{ConfigFactory, Config}
import fi.vm.sade.hakuperusteet.Configuration
import fi.vm.sade.hakuperusteet.domain.{PaymentStatus, Payment}
import org.scalatest.{Matchers, FlatSpec}
import scala.collection.JavaConversions._

class VetumaSpec extends FlatSpec with Matchers {

  behavior of "Vetuma"

  val port = scala.util.Random.nextInt(20000) + 20000

  val config = ConfigFactory.parseMap(Map(
    "vetuma.shared.secret" -> "TESTIASIAKAS11-873C992B8C4C01EC8355500CAA709B37EA43BC2E591ABF29FEE5EAFE4DCBFA35",
    "vetuma.shared.ap" -> "TESTIASIAKAS1", "vetuma.shared.rcvid" -> "TESTIASIAKAS11",
    "vetuma.host" -> s"http://localhost:$port/")).resolve

  it should "calculate return mac properly" in {
    val params = List("TESTIASIAKAS11", "20061218154432445", "P2", "fi", "https://localhost/ShowPayment.asp", "https://localhost/ShowCancel.asp",
      "https://localhost/ShowError.asp", "166449462440200", "1234561", "123", "06122588INWX0000", "SUCCESSFUL")

    val sharedSecret = "TESTIASIAKAS11-873C992B8C4C01EC8355500CAA709B37EA43BC2E591ABF29FEE5EAFE4DCBFA35"

    val result = Vetuma.verifyReturnMac(sharedSecret, params, "61397453AAF0A93C2242EFD8813436C58B069B3F38041FC19CD900974B8E9984")
    result shouldEqual true
  }
  it should "return correct and only correct parameters" in {
    val timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").parse("20100915121357210")
    val paymCallId = "1082624993"
    val hrefOk = "https://localhost/ShowPayment.asp"
    val hrefCancel = "https://localhost/ShowCancel.asp"
    val hrefError = "https://localhost/ShowError.asp"
    val query = Vetuma.query(config,paymCallId,"fi", hrefOk, hrefCancel, hrefError, timestamp, "P", Some("1082624993"), "TESTAPP")
    query.plainText shouldEqual "TESTIASIAKAS11&TESTAPP&20100915121357210&&P&PAYMENT&CHECK&fi&https://localhost/ShowPayment.asp&https://localhost/ShowCancel.asp&https://localhost/ShowError.asp&TESTIASIAKAS1&1082624993&1082624993&TESTIASIAKAS11-873C992B8C4C01EC8355500CAA709B37EA43BC2E591ABF29FEE5EAFE4DCBFA35&"

    val params = query.toParams
    params.get("MAC") shouldEqual Some("6D55BF6B78C937FD6D32E7E0B5CCF5AA8CAAFC8D3C0FC7AB48159A0EB3E85A8D")
    params.get("RCVID") shouldEqual Some("TESTIASIAKAS11")
    params.get("APPID") shouldEqual Some("TESTAPP")
    params.get("TIMESTMP") shouldEqual Some("20100915121357210")
    params.get("SO") shouldEqual Some("")
    params.get("SOLIST") shouldEqual Some("P")
    params.get("TYPE") shouldEqual Some("PAYMENT")
    params.get("AU") shouldEqual Some("CHECK")
    params.get("LG") shouldEqual Some("fi")
    params.get("RETURL") shouldEqual Some("https://localhost/ShowPayment.asp")
    params.get("CANURL") shouldEqual Some("https://localhost/ShowCancel.asp")
    params.get("ERRURL") shouldEqual Some("https://localhost/ShowError.asp")
    params.get("AP") shouldEqual Some("TESTIASIAKAS1")
    params.get("PAYM_CALL_ID") shouldEqual Some("1082624993")
    params.get("TRID") shouldEqual Some("1082624993")

    val validKey = Set("MAC","RCVID","APPID","TIMESTMP","SO","SOLIST","TYPE","AU","LG","RETURL","CANURL","ERRURL","AP","PAYM_CALL_ID","TRID")
    params.filterKeys(k => !validKey.contains(k)) shouldEqual Map()
  }

  it should "send right things in right format to Vetuma service" in {
    val vetumaCheck = new VetumaCheck(config, "TESTAPP", "P")
    val timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").parse("20100915121357210")
    val paymCallId = "1082624993"
    val hrefOk = "https://localhost/ShowPayment.asp"
    val hrefCancel = "https://localhost/ShowCancel.asp"
    val hrefError = "https://localhost/ShowError.asp"
    val server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/Query", new HttpHandler() {
      override def handle(t: HttpExchange) = {
        val body = scala.io.Source.fromInputStream(t.getRequestBody).mkString
        val query = parse(s"?$body").query.params.map(entry => {
          val (key, value) = entry
          key -> value
        }).toMap
        val mac = query.get("MAC").flatten.get
        val timestamp = query.get("TIMESTMP").flatten.get
        val response = s"MAC=$mac&TIMESTMP=$timestamp&STATUS=ERROR&PAYM_STATUS=UNKNOWN_PAYMENT"
        t.sendResponseHeaders(200, response.length())
        val os = t.getResponseBody()
        os.write(response.getBytes())
        os.close()
      }
    });
    server.setExecutor(null); // creates a default executor
    server.start();

    val check = vetumaCheck.doVetumaCheck(paymCallId, timestamp, "fi", Some("1082624993"))
    server.stop(0)

    val t: Date = new SimpleDateFormat("yyyyMMddHHmmssSSS").parse("20100915121357210")
    check shouldEqual Some(CheckResponse("UNKNOWN_PAYMENT", "ERROR", Some("6D55BF6B78C937FD6D32E7E0B5CCF5AA8CAAFC8D3C0FC7AB48159A0EB3E85A8D"), Some(t), None,None,None,None,None,None,None,None,None,None))
  }
}
