package fi.vm.sade.hakuperusteet

import java.util.NoSuchElementException

import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.henkilo.{HenkiloClient, IfGoogleAddEmailIDP}
import fi.vm.sade.hakuperusteet.util.{AuditLog, PaymentUtil}
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait UserService extends LazyLogging {
  implicit val executionContext: ExecutionContext
  val db: HakuperusteetDatabase
  val henkiloClient: HenkiloClient
  
  def searchUsers(searchTerm: String): Seq[AbstractUser] = {
    val lowerCaseSearchTerm = searchTerm.toLowerCase
    db.allUsers.filter(u => lowerCaseSearchTerm.isEmpty ||
      u.email.toLowerCase.contains(lowerCaseSearchTerm) ||
      (u.fullName).toLowerCase.contains(lowerCaseSearchTerm))
  }

  def findByPersonOid(personOid: String): Option[AbstractUser] =
    db.findUserByOid(personOid)

  def findUserData(casSession: CasSession, personOid: String): Option[AbstractUserData] =
    db.findUserByOid(personOid).map {
      case user: User => db.run(fetchUserDataAction(casSession, user), 5 seconds)
      case user: PartialUser => fetchPartialUserData(casSession, user)
    }
  
  def findUsersWithDrasticallyChangedPaymentState(): Seq[UserPaymentData] = {
    db.findAllUsersAndPaymentsWithDrasticallyChangedPaymentStates.map(entry => {
      val (user, payments) = entry

      val oldest = PaymentUtil.hasPaidWithTheseStatuses(payments.map(db.oldestPaymentStatuses))
      val newest = PaymentUtil.hasPaidWithTheseStatuses(payments.map(db.newestPaymentStatuses))
      UserPaymentData(user, payments, oldest, newest)
    })
  }

  def updateUser(casSession: CasSession, user: AbstractUser): AbstractUserData = user match {
    case user: User =>
      db.findUserByOid(user.personOid.getOrElse(throw new IllegalArgumentException("user must contain person oid"))) match {
        case Some(oldUser: User) =>
          val updatedUser = oldUser.copy(
            firstName = user.firstName,
            lastName = user.lastName,
            birthDate = user.birthDate,
            personId = user.personId,
            gender = user.gender,
            nativeLanguage = user.nativeLanguage,
            nationality = user.nationality)
          saveUpdatedUserData(casSession, updatedUser)
        case _ => throw new NoSuchElementException(s"user ${user.email} not found")
      }
    case user: PartialUser => throw new RuntimeException(s"unexpected partial user ${user.email}")
  }

  def syncAndWriteResponse(casSession: CasSession, user: AbstractUser): AbstractUserData = user match {
    case user: User =>
      db.run((for {
        userData <- fetchUserDataAction(casSession, user)
        _ <- DBIO.sequence(userData.applicationObject.map(db.insertSyncRequest(userData.user, _)))
      } yield userData).transactionally, 5 seconds)
    case user: PartialUser => throw new RuntimeException(s"unexpected partial user ${user.email}")
  }
  
  def fetchPartialUserData(casSession: CasSession, user: AbstractUser): AbstractUserData = user match {
    case user: PartialUser => {
      val payments = db.findPayments(user)
      PartialUserData(casSession, user, PaymentUtil.sortPaymentsByStatus(payments).map(decorateWithPaymentHistory), PaymentUtil.hasPaid(payments))
    }
    case user: User => throw new RuntimeException(s"unexpected full user ${user.email}")
  }
  
  def fetchUserData(casSession: CasSession, user: AbstractUser): AbstractUserData = user match {
    case user: User => db.run(fetchUserDataAction(casSession, user), 5 seconds)
    case user: PartialUser => throw new RuntimeException(s"unexpected partial user ${user.email}")
  }

  private def fetchUserDataAction(casSession: CasSession, user: AbstractUser): DBIO[UserData] = user match {
    case user: User => for {
      payments <- db.findPaymentsAction(user).map(PaymentUtil.sortPaymentsByStatus)
      applicationObjects <- db.findApplicationObjects(user)
    } yield UserData(casSession, user, applicationObjects, payments.map(decorateWithPaymentHistory), PaymentUtil.hasPaid(payments))
    case user: PartialUser => throw new RuntimeException(s"unexpected partial user ${user.email}")
  }

  private def decorateWithPaymentHistory(p: Payment): Payment =
    p.copy(history = Some(db.findStateChangingEventsForPayment(p).sortBy(_.created)))

  private def saveUpdatedUserData(casSession: CasSession, updatedUserData: AbstractUser): AbstractUserData = updatedUserData match {
    case updatedUserData: User =>
    Try(henkiloClient.upsertHenkilo(IfGoogleAddEmailIDP(updatedUserData))) match {
      case Success(_) => upsertAndAudit(casSession, updatedUserData)
      case Failure(t: IllegalArgumentException) =>
        logger.error("error parsing user", t)
        throw t
      case Failure(t) =>
        val error = s"upserting user ${updatedUserData.email} to henkilopalvelu failed"
        logger.error(error, t)
        throw new RuntimeException(error, t)
    }
    case user: PartialUser => throw new RuntimeException(s"unexpected partial user ${user.email}")
  }

  private def upsertAndAudit(casSession: CasSession, user: AbstractUser): AbstractUserData = user match {
    case user: User => {
      db.insertUserDetails(user)
      AuditLog.auditAdminPostUserdata(casSession.oid, user)
      syncAndWriteResponse(casSession, user)
    }
    case user: PartialUser => throw new RuntimeException(s"unexpected partial user ${user.email}")
  }

}

object UserService {
  def apply(ec: ExecutionContext, hc: HenkiloClient, database: HakuperusteetDatabase): UserService = new UserService {
    override implicit val executionContext: ExecutionContext = ec
    override val henkiloClient: HenkiloClient = hc
    override val db: HakuperusteetDatabase = database
  }
}

sealed trait AbstractUserData

case class UserData(session: CasSession, user: User, applicationObject: Seq[ApplicationObject], payments: Seq[Payment], hasPaid: Boolean) extends AbstractUserData

case class PartialUserData(session: CasSession, user: PartialUser, payments: Seq[Payment], hasPaid: Boolean, isPartialUserData: Boolean = true) extends AbstractUserData

case class UserPaymentData(user: AbstractUser, payments: Seq[Payment], old_state: Boolean, new_state: Boolean)
