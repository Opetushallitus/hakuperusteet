package fi.vm.sade.hakuperusteet

import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.email.EmailTemplate
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import fi.vm.sade.hakuperusteet.koodisto.Countries
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.tarjonta.Tarjonta
import fi.vm.sade.hakuperusteet.util.Translate
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait ApplicationObjectService extends LazyLogging {
  implicit val executionContext: ExecutionContext
  val db: HakuperusteetDatabase
  val tarjonta: Tarjonta
  val countries: Countries
  val oppijanTunnistus: OppijanTunnistus
  val paymentService: PaymentService

  def upsertApplicationObject(casSession: CasSession, user: AbstractUser, ao: ApplicationObject): Try[Unit] =
    db.run((for {
      _ <- ensureFullUser(user)
      oldAo <- db.findApplicationObjectByHakukohdeOid(user, ao.hakukohdeOid)
      _ <- db.upsertApplicationObject(ao)
      _ <- insertSyncRequests(user)
      _ <- HakuperusteetDatabase.toDBIO(sendPaymentInfoEmailIfPaymentNowRequired(user, oldAo, ao))
    } yield ()).transactionally.asTry)

  private def ensureFullUser(user: AbstractUser): DBIO[Unit] = user match {
    case u: User => DBIO.successful(())
    case u: PartialUser => DBIO.failed(new IllegalArgumentException(s"${user.personOid} is a partial user"))
  }
  
  private def insertSyncRequests(user: AbstractUser): DBIO[Seq[Unit]] = user match {
    case user: User => db.findApplicationObjects(user).flatMap(aos => DBIO.sequence(aos.map(db.insertSyncRequest(user, _))))
    case user: PartialUser => DBIO.failed(new RuntimeException("unexpected partial user"))
  }

  private def sendPaymentInfoEmailIfPaymentNowRequired(user: AbstractUser,
                                                       oldAO: Option[ApplicationObject],
                                                       newAO: ApplicationObject): Try[Unit] = oldAO match {
    case Some(ao) if paymentNowRequired(paymentService.findPayments(user), ao, newAO) =>
      sendPaymentInfoEmail(user, newAO.hakukohdeOid)
    case _ => Success(())
  }

  private def sendPaymentInfoEmail(user: AbstractUser, hakukohdeOid: String): Try[Unit] = {
    logger.info(s"sending payment info email to ${user.email}")
    getApplicationObjectName(hakukohdeOid, user.lang) flatMap (applicationObjectName => {
      val dueDate = nDaysInFuture(10)
      oppijanTunnistus.sendToken(hakukohdeOid, user.email,
        Translate("email", "paymentInfo", user.lang, "title"),
        EmailTemplate.renderPaymentInfo(applicationObjectName, dueDate, user.lang),
        user.lang, dueDate.getTime) match {
        case Success(_) => Success(())
        case Failure(e) => Failure(new RuntimeException(s"sending payment info email to ${user.email} failed", e))
      }
    })
  }

  private def getApplicationObjectName(hakukohdeOid: String, lang: String): Try[String] =
    Try(tarjonta.getApplicationObject(hakukohdeOid)) match {
      case Success(ao) => Success(ao.name.get(lang))
      case Failure(e) => Failure(new RuntimeException(s"fetching application object $hakukohdeOid from tarjonta failed", e))
    }

  private def nDaysInFuture(n: Int) = new Date(new Date().getTime + n * 24 * 60 * 60 * 1000)

  private def allPaymentsFailed(payments: Seq[Payment]): Boolean =
    payments.forall(p => Set(PaymentStatus.cancel, PaymentStatus.error, PaymentStatus.unknown).contains(p.status))

  private def paymentWasNotPreviouslyRequired(oldAO: ApplicationObject) =
    !countries.shouldPay(oldAO.educationCountry, oldAO.educationLevel)

  private def paymentIsCurrentlyRequired(newAO: ApplicationObject) =
    countries.shouldPay(newAO.educationCountry, newAO.educationLevel)

  private def paymentNowRequired(payments: Seq[Payment], oldAO: ApplicationObject, newAO: ApplicationObject) =
    allPaymentsFailed(payments) && paymentWasNotPreviouslyRequired(oldAO) && paymentIsCurrentlyRequired(newAO)
}

object ApplicationObjectService {
  def apply(ec: ExecutionContext, cs: Countries, database: HakuperusteetDatabase , ot: OppijanTunnistus, ps: PaymentService, t: Tarjonta): ApplicationObjectService = new ApplicationObjectService {
    override val countries: Countries = cs
    override implicit val executionContext: ExecutionContext = ec
    override val db: HakuperusteetDatabase = database
    override val oppijanTunnistus: OppijanTunnistus = ot
    override val paymentService: PaymentService = ps
    override val tarjonta: Tarjonta = t
  }
}
