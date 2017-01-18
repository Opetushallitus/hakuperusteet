package fi.vm.sade.hakuperusteet.admin

import java.util.concurrent.Executors
import fi.vm.sade.hakuperusteet.HakumaksukausiService
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.PaymentState.PaymentState
import fi.vm.sade.hakuperusteet.domain.PaymentStatus.PaymentStatus
import fi.vm.sade.hakuperusteet.domain.{PaymentUpdate, _}
import fi.vm.sade.hakuperusteet.hakuapp.HakuAppClient
import fi.vm.sade.hakuperusteet.koodisto.Countries
import fi.vm.sade.hakuperusteet.redirect.RedirectCreator._
import fi.vm.sade.hakuperusteet.rsa.RSASigner
import fi.vm.sade.hakuperusteet.tarjonta.{ApplicationSystem, Tarjonta}
import fi.vm.sade.hakuperusteet.util.{HttpUtil, PaymentUtil}
import org.apache.http.client.fluent.{Request, Response}
import org.apache.http.entity.ContentType
import org.http4s
import org.json4s.jackson.Serialization._

import scala.concurrent.duration._
import scala.util.control.Exception._
import scala.util.{Failure, Success, Try}

class Synchronization(config: Config, db: HakuperusteetDatabase, tarjonta: Tarjonta, countries: Countries, signer: RSASigner, hakumaksukausiService: HakumaksukausiService) extends LazyLogging {

  import fi.vm.sade.hakuperusteet._

  val hakuAppClient = HakuAppClient.init(config)
  val scheduler = Executors.newScheduledThreadPool(1)

  def start = scheduler.scheduleWithFixedDelay(checkTodoSynchronizations, 1, config.getDuration("admin.synchronization.interval", SECONDS), SECONDS)

  def checkTodoSynchronizations = SynchronizationUtil.asSimpleRunnable { () =>
    logger.info("Starting synchronization, fetching next sync id...")
    db.fetchNextSyncIds.foreach(checkSynchronizationForId)
  }

  protected def checkSynchronizationForId(id: Int): Unit = {
    logger.info(s"Found synchronization id $id, fetching details...")
    db.findSynchronizationRequest(id).foreach {
      case Some(sync: HakuAppSyncRequest) => synchronizePaymentRow(sync)
      case Some(sync: ApplicationObjectSyncRequest) => synchronizeUserRow(sync)
      case _ => logger.error("Unexpected sync request")
    }
  }

  private def synchronizePaymentRow(row: HakuAppSyncRequest) = {
    val sync = row.hakuOid match {
      case Some(s) => tarjonta.getApplicationSystem(s).sync
      case None => false
    }

    val hakumaksukausi = hakumaksukausiService.getHakumaksukausiForHakemus(row.hakemusOid)
    val payments = PaymentUtil.sortPaymentsByStatus(db.findUserByOid(row.henkiloOid).map(db.findPayments)
      .map(_.filter(payment => Some(payment.kausi).equals(hakumaksukausi))).getOrElse(Seq())).headOption
    (payments match {
      case Some(payment) => paymentStatusToPaymentState(payment.status)
      case _ => None
    }) match {
      case Some(state) => Try {
        logger.info(s"Synching row id ${row.id} with Haku-App, matching fake operation: " + createCurl(hakuAppClient.url(row.hakemusOid), write(PaymentUpdate(state))))
        hakuAppClient.updateHakemusWithPaymentState(row.hakemusOid, state) } match {
        case Success(r) => handleHakuAppPostSuccess(row, state, r, sync)
        case Failure(f) => if (sync) { handleSyncError(row.id, s"Synchronization to Haku-App throws with $row", Some(f)) } else { handleSyncExpired(row.id, "Synchronization expired", None) }
      }
      case None =>
        // TODO: What to do when payment has no state that requires update?
        db.markSyncDone(row.id)
    }
  }

  private def paymentStatusToPaymentState(status: PaymentStatus) =
  status match {
    case PaymentStatus.ok => Some(PaymentState.OK)
    case PaymentStatus.cancel => Some(PaymentState.NOTIFIED)
    case PaymentStatus.error => Some(PaymentState.NOT_OK)
    case _ => None
  }

  private def handleHakuAppPostSuccess(row: HakuAppSyncRequest, state: PaymentState, response: http4s.Response, sync: Boolean): Unit = {
    val statusCode = response.status.code
    statusCode match {
      case 200 | 204 =>
        logger.info(s"Synced row id ${row.id} with Haku-App, henkiloOid ${row.henkiloOid}, hakemusOid ${row.hakemusOid} and payment state $state")
        db.markSyncDone(row.id)
      case 403 =>
        logger.warn(s"Tried to sync row id ${row.id} with Haku-App, henkiloOid ${row.henkiloOid}, hakemusOid ${row.hakemusOid} and payment state $state but the user is no longer liable for payment.")
        db.markSyncDone(row.id)
      case _ =>
        logger.error(s"Synchronization error with statuscode $statusCode")
        if (sync) { db.markSyncError(row.id) } else { db.markSyncExpired(row.id) }
    }
  }

  protected def synchronizeUserRow(row: ApplicationObjectSyncRequest) =
    Try { tarjonta.getApplicationSystem(row.hakuOid) } match {
      case Success(as) => continueWithTarjontaData(row, as)
      case Failure(f) => handleSyncError(row.id, s"Synchronization Tarjonta application system throws $row", Some(f))
    }

  private def continueWithTarjontaData(row: ApplicationObjectSyncRequest, as: ApplicationSystem) =
    db.findUserByOid(row.henkiloOid).foreach { (u) =>
      u match {
        case u:User =>
          db.run(db.findApplicationObjectByHakukohdeOid(u, row.hakukohdeOid))
            .foreach(synchronizeWithData(row, as, u, db.findPayments(u).filter(_.kausi == as.hakumaksukausi)))
        case u: PartialUser =>
          logger.error("PartialUser in user sync loop!")
      }
    }

  private def synchronizeWithData(row: ApplicationObjectSyncRequest, as: ApplicationSystem, u: User, payments: Seq[Payment])(ao: ApplicationObject) {
    val shouldPay = countries.shouldPay(ao.educationCountry, ao.educationLevel)
    val hasPaid = PaymentUtil.hasPaid(payments)
    as.formUrl match {
      case Some(formUrl) =>
        val body = generatePostBody(generateParamMap(signer, u, ao, shouldPay, hasPaid, admin = true))
        logger.info(s"Synching row id ${row.id}, matching fake operation: " + createCurl(formUrl, body))
        Try { doPost(formUrl, body) } match {
          case Success(response) => handlePostSuccess(row, response)
          case Failure(f) => if (as.sync) { handleSyncError(row.id, "Synchronization POST throws", Some(f)) } else { handleSyncExpired(row.id, "Synchronization expired", Some(f)) }
        }
      case None =>
        if (as.sync) { handleSyncError(row.id, "Synchronization failed because of missing form URL (hakulomake URI)", None) } else { handleSyncExpired(row.id, "Synchronization expired", None) }
    }
  }

  private def handlePostSuccess(row: ApplicationObjectSyncRequest, response: Response): Unit = {
    val statusCode = response.returnResponse().getStatusLine.getStatusCode
    if (statusCode == 200 || statusCode == 204) {
      logger.info(s"Synced row id ${row.id}, henkiloOid ${row.henkiloOid}, hakukohdeoid ${row.hakukohdeOid}")
      db.markSyncDone(row.id)
    } else {
      logger.error(s"Synchronization error with statuscode $statusCode, message was " + allCatch.opt(response.returnContent().asString()))
      db.markSyncError(row.id)
    }
  }

  private def doPost(formUrl: String, body: String) = HttpUtil.addHeaders(Request.Post(formUrl))
    .bodyString(body, ContentType.create("application/x-www-form-urlencoded")).execute()

  private def createCurl(formUrl: String, body: String) = "curl -i -X POST --data \"" + body + "\" " + formUrl

  private def handleSyncError(id: Int, errorMsg: String, f: Option[Throwable]) = {
    db.markSyncError(id)
    if (f.isDefined) logger.error(errorMsg, f.get) else logger.error(errorMsg)
  }

  private def handleSyncExpired(id: Int, errorMsg: String, f: Option[Throwable]) = {
    db.markSyncExpired(id)
    if (f.isDefined) logger.error(errorMsg, f.get) else logger.error(errorMsg)
  }
}

object Synchronization {
  def apply(config: Config, db: HakuperusteetDatabase, tarjonta: Tarjonta, countries: Countries, signer: RSASigner, hakumaksukausiService: HakumaksukausiService) = new Synchronization(config, db, tarjonta, countries, signer, hakumaksukausiService)
}

object SynchronizationStatus extends Enumeration {
  type SynchronizationStatus = Value
  val todo, active, done, error, expired = Value
}

object SynchronizationUtil extends LazyLogging {
  def asSimpleRunnable(f: () => Unit) = new Runnable() {
    override def run() {
      Try(f()).recoverWith { case e =>
        logger.error("synchronization runnable got exception, which is ignored", e)
        Success()
      }
    }
  }
}