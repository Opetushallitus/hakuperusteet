package fi.vm.sade.hakuperusteet

import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.tarjonta.Tarjonta
import org.json4s._
import org.json4s.jackson.Serialization._
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

import scala.util.{Failure, Success, Try}

class TarjontaServlet(tarjonta: Tarjonta) extends ScalatraServlet with JacksonJsonSupport with LazyLogging {
  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  get("/:hakukohdeoid") {
    Try { tarjonta.getApplicationObject(params("hakukohdeoid")) } match {
      case Success(ao) =>
        Try { tarjonta.enrichHakukohdeWithHaku(ao) } match {
          case Success(combined) => write(combined)
          case Failure(f) => logAndHalt(f, params("hakukohdeoid"), Some(ao.hakuOid))
        }
      case Failure(f) => logAndHalt(f, params("hakukohdeoid"), None)
    }
  }

  def logAndHalt(f: Throwable, hakukohdeOid: String, hakuOid: Option[String]) = {
    logger.error(s"TarjontaServlet error with hakukohdeOid: $hakukohdeOid and hakuOid: $hakuOid", f)
    halt(500)
  }

  error { case e: Throwable => logger.error("uncaught exception", e) }
}
