package fi.vm.sade.hakuperusteet.vetuma

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain._
import scala.concurrent.duration._


class VetumaGuessMac(config: Config, db: HakuperusteetDatabase) extends LazyLogging {
  val hostBaseUrl = config.getString("host.url.base")

  def paymentToMacCandidates(payment: Payment): List[String] = {
    db.findUserByOid(payment.personOid) match {
      case Some(user) =>
        if(payment.hakemusOid.isDefined) {
          handleHakuAppUserPayment(payment, user)
        } else {
          handleAppObjectUserPayment(payment, user, db.run(db.findApplicationObjects(user), 5 seconds))
        }
      case _ =>
        logger.error(s"Unable to find User for payment $payment")
        List()
    }
  }

  private def handleAppObjectUserPayment(payment: Payment, user: AbstractUser, applicationObjects: Seq[ApplicationObject]): List[String] = {
    applicationObjects match {
      case Seq() =>
        logger.error(s"$user had $payment but no applications. Not enough information to create MAC.")
        List()
      case _ =>
        val macs = applicationObjects.map(ao => Vetuma(config, payment, user.uiLang, hostBaseUrl, s"&ao=${ao.hakukohdeOid}").mac).toList
        logger.debug(s"Guessing MAC for payment $payment using hakukohde OIDs, $macs")
        macs
    }
  }

  private def handleHakuAppUserPayment(payment: Payment, user: AbstractUser): List[String] = {
    val mac = Vetuma(config, payment, user.uiLang, hostBaseUrl, s"&app=${payment.hakemusOid.get}").mac
    logger.debug(s"Guessing MAC for payment $payment using hakemus OID, $mac")
    List(mac)
  }
}
