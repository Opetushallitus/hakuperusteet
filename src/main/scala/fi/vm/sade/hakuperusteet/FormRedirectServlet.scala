package fi.vm.sade.hakuperusteet

import com.typesafe.config.Config
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import fi.vm.sade.hakuperusteet.domain.{ApplicationObject, PaymentStatus}
import fi.vm.sade.hakuperusteet.google.GoogleVerifier
import fi.vm.sade.hakuperusteet.koodisto.Countries
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.redirect.RedirectCreator
import fi.vm.sade.hakuperusteet.rsa.RSASigner
import fi.vm.sade.hakuperusteet.tarjonta.{ApplicationSystem, Tarjonta}
import org.json4s.jackson.Serialization.write

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class FormRedirectServlet(config: Config, db: HakuperusteetDatabase, oppijanTunnistus: OppijanTunnistus, verifier: GoogleVerifier, signer: RSASigner, countries: Countries, tarjonta: Tarjonta) extends HakuperusteetServlet(config, db, oppijanTunnistus, verifier) {

  get("/redirect") {
    failUnlessAuthenticated()

    userDataFromSession match {
      case userData: User =>
        val hakukohdeOid = params.get("hakukohdeOid").getOrElse(halt(409))
        val applicationObjectForThisHakukohde = db.run(db.findApplicationObjectByHakukohdeOid(userDataFromSession, hakukohdeOid)).getOrElse(halt(409))
        val educationLevel = Some(applicationObjectForThisHakukohde.educationLevel).getOrElse(halt(409))
        Try { tarjonta.getApplicationSystem(applicationObjectForThisHakukohde.hakuOid) } match {
          case Success(as) => doRedirect(userData, applicationObjectForThisHakukohde, as, educationLevel) match {
            case Left(statusCode) if statusCode == 409 =>
              logger.error("Conflicting payment information on redirect (user: {}, hakukohdeOid: {})", userData.personOid, hakukohdeOid)
              halt(statusCode)
            case Left(statusCode) =>
              halt(statusCode)
            case Right(body) =>
              write(body)
          }
          case Failure(f) =>
            logger.error("FormRedirectServlet throws", f)
            halt(500)
        }
      case _ =>
        logger.error("Session had no valid user for redirection!")
        halt(500)
    }
  }

  def doRedirect(userData: User, applicationObjectForThisHakukohde: ApplicationObject, as: ApplicationSystem, educationLevel : String) : Either[Int, Map[String, Any]] = {
    val formUrl = as.formUrl
    val payments = db.findPayments(userData)
    val shouldPay = as.maksumuuriKaytossa && countries.shouldPay(applicationObjectForThisHakukohde.educationCountry, educationLevel)
    val hasPaid = shouldPay && payments.exists(p => (p.status.equals(PaymentStatus.ok) && Some(p.kausi).equals(as.hakumaksukausi)))

    if (shouldPay && !hasPaid) {
      Left(409)
    } else {
      Right(Map("url" -> formUrl, "params" -> RedirectCreator.generateParamMap(signer, userData, applicationObjectForThisHakukohde, shouldPay, hasPaid)))
    }
  }
}
