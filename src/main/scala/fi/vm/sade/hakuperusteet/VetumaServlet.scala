package fi.vm.sade.hakuperusteet

import java.util.Date

import com.typesafe.config.Config
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.Hakumaksukausi.Hakumaksukausi
import fi.vm.sade.hakuperusteet.domain.PaymentStatus
import fi.vm.sade.hakuperusteet.domain.PaymentStatus._
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.email.{ReceiptValues, EmailTemplate, EmailSender}
import fi.vm.sade.hakuperusteet.google.GoogleVerifier
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.tarjonta.Tarjonta
import fi.vm.sade.hakuperusteet.util.{Translate, AuditLog}
import fi.vm.sade.hakuperusteet.vetuma.Vetuma
import org.json4s.jackson.Serialization._

import scala.util.{Failure, Success, Try}

class VetumaServlet(config: Config, db: HakuperusteetDatabase, oppijanTunnistus: OppijanTunnistus, verifier: GoogleVerifier, emailSender: EmailSender, tarjonta: Tarjonta, hakumaksukausiService: HakumaksukausiService) extends HakuperusteetServlet(config, db, oppijanTunnistus, verifier) {

  get("/openvetuma/hakemus/:hakemusoid") {
    failUnlessAuthenticated()
    val hakemusOidOption = params.get("hakemusoid")
    val hakemusOidParam = hakemusOidOption.map(app => s"&app=$app")

    createVetumaWithHref(getHref, hakemusOidParam, params.get("hakemusoid"),
      hakumaksukausiService.getHakumaksukausiForHakemus(hakemusOidOption.get).get)
  }

  get("/openvetuma/hakumaksukausi/:hakumaksukausi") {
    failUnlessAuthenticated()
    createVetumaWithHref(getHref, None, None, Hakumaksukausi.withName(params.get("hakumaksukausi").get))
  }

  get("/openvetuma/hakukohde/:hakukohdeoid") {
    failUnlessAuthenticated()
    val hakukohdeOidOption = params.get("hakukohdeoid")
    val hakukohdeOidParam = hakukohdeOidOption.map(ao => s"&ao=$ao")

    createVetumaWithHref(getHref, hakukohdeOidParam, None,
      hakumaksukausiService.getHakumaksukausiForHakukohde(hakukohdeOidOption.get).get)
  }

  private def getHref = params.get("href").getOrElse(halt(409))

  private def createVetumaWithHref(href: String, params: Option[String], hakemusOid: Option[String], kausi: Hakumaksukausi) = {
    val userData = userDataFromSession
    val language = userData.lang
    val referenceNumber = referenceNumberFromPersonOid(userData.personOid.getOrElse(halt(500)))
    val orderNro = referenceNumber + db.nextOrderNumber()
    val paymCallId = "PCID" + orderNro
    val paymentWithId = db.upsertPayment(Payment(None, userData.personOid.get, new Date(), referenceNumber, orderNro, paymCallId, PaymentStatus.started, kausi, hakemusOid)).getOrElse(halt(500))
    val vetuma = Vetuma(config, paymentWithId, language, href, params.getOrElse("")).toParams
    AuditLog.auditPayment(userData, paymentWithId)
    write(Map("url" -> config.getString("vetuma.host"), "params" -> vetuma))
  }

  post("/return/ok") {
    handle("#/effect/VetumaResultOk", params.get("href").getOrElse(halt(500)), PaymentStatus.ok)
  }

  post("/return/cancel") {
    handle("#/effect/VetumaResultCancel", params.get("href").getOrElse(halt(500)), PaymentStatus.cancel)
  }

  post("/return/error") {
    handle("#/effect/VetumaResultError", params.get("href").getOrElse(halt(500)), PaymentStatus.error)
  }

  private def handle(hash: String, href:String, status: PaymentStatus) = {
    val handler = handleHakuAppPaymentOrHakuperusteetPayment(params.get("ao"), params.get("app"))
    handleReturn(hash,href, status, handler)
  }

  private def handleHakuAppPaymentOrHakuperusteetPayment(hakukohdeOid: Option[String], hakemusOid: Option[String]) = {
    (hakukohdeOid, hakemusOid) match {
      case (None, Some(hakemusOid)) => handleHakuAppPayment(hakemusOid)_
      case _ => handlePayment(hakukohdeOid)_
    }
  }

  private def handlePaymentWhenThereIsNoReferenceToPayment(hash: String, href:String) = {
    // todo: handle case when payment completes (with some status) but there's no reference to that payment in DB
    halt(status = 303, headers = Map("Location" -> createUrl(href, hash, None)))
  }
  private def handleHakuAppPayment(hakemusOid: String)(hash: String, href: String, userData: AbstractUser, p: Payment, status: PaymentStatus) = {
    val paymentWithSomeStatus = p.copy(status = status, hakemusOid = Some(hakemusOid))
    db.upsertPayment(paymentWithSomeStatus)
    AuditLog.auditPayment(userData, paymentWithSomeStatus)
    if(status == PaymentStatus.ok) {
      emailSender.sendReceipt(userData, paymentWithSomeStatus)
    }
    db.insertPaymentSyncRequest(userData, paymentWithSomeStatus)
    val url = href + s"app/$hakemusOid$hash"
    halt(status = 303, headers = Map("Location" -> url))
  }
  private def handlePayment(hakukohdeOid: Option[String])(hash: String, href: String, userData: AbstractUser, p: Payment, status: PaymentStatus) = {
    val paymentWithSomeStatus = p.copy(status = status)
    db.upsertPayment(paymentWithSomeStatus)
    AuditLog.auditPayment(userData, paymentWithSomeStatus )
    if (status == PaymentStatus.ok) {
      emailSender.sendReceipt(userData, paymentWithSomeStatus)
    }
    halt(status = 303, headers = Map("Location" -> createUrl(href, hash, hakukohdeOid)))
  }

  def referenceNumberFromPersonOid(personOid: String) = personOid.split("\\.").toList.last

  private def handleReturn(href: String, hash: String, status: PaymentStatus, // hakukohdeOid: Option[String], hakemusOid: Option[String], status: PaymentStatus,
                           handlePayment: (String, String, AbstractUser, Payment, PaymentStatus) => Unit) = {
    val macParams = createMacParams
    val expectedMac = params.getOrElse("MAC", "")
    if (!Vetuma.verifyReturnMac(config.getString("vetuma.shared.secret"), macParams, expectedMac)) halt(403)

    val userData = userDataFromSession
    db.findPaymentByOrderNumber(userData.personOid.get, params.getOrElse("ORDNR", "")) match {
      case Some(p) => handlePayment(href, hash,userData, p, status)
      case None => handlePaymentWhenThereIsNoReferenceToPayment(href, hash)
    }
  }

  private def createUrl(href: String, hash: String, hakukohdeOid: Option[String]) = href + hakukohdeOid.map(ao => s"ao/$ao").getOrElse("") + hash

  private def createMacParams = {
    def p(name: String) = params.getOrElse(name, "")
    List(p("RCVID"), p("TIMESTMP"), p("SO"), p("LG"), p("RETURL"), p("CANURL"), p("ERRURL"), p("PAYID"), p("REF"), p("ORDNR"), p("PAID"), p("STATUS"))
  }

  private def fetchNameFromTarjonta(hakukohdeOid: String) =
    Try { tarjonta.getApplicationObject(hakukohdeOid) } match {
      case Success(ao) => Some(ao.name.en.get) // TODO APO what if en not defined?
      case Failure(f) =>  None
    }
}
