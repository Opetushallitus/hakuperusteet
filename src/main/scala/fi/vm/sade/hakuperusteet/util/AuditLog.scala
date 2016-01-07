package fi.vm.sade.hakuperusteet.util

import fi.vm.sade.auditlog.hakuperusteet.HakuPerusteetOperation
import fi.vm.sade.auditlog.hakuperusteet.LogMessage.builder
import fi.vm.sade.auditlog.{ApplicationType, Audit}
import fi.vm.sade.hakuperusteet.domain.PaymentStatus.PaymentStatus
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
object AuditLog {
  val serviceName = "hakuperusteet"
  val audit = new Audit(serviceName, ApplicationType.OPISKELIJA)
  val auditAdmin = new Audit(serviceName, ApplicationType.VIRKAILIJA)

  def auditPostUserdata(userData: User) = audit.log(buildCommonUserData(userData).build())

  def auditAdminPostUserdata(henkiloOid: String, userData: User) = auditAdmin.log(
    buildCommonUserData(userData).virkailijaHenkiloOid(henkiloOid).build())

  def auditPostEducation(userData: User, education: ApplicationObject) = audit.log(buildCommonEducation(userData, education).build())

  def auditAdminPostEducation(henkiloOid: String, userData: User, education: ApplicationObject) = auditAdmin.log(
    buildCommonEducation(userData, education).virkailijaHenkiloOid(henkiloOid).build())

  def auditPayment(userData: AbstractUser, payment: Payment) = audit.log(buildCommonPayment(userData, payment).build())

  def auditAdminPayment(henkiloOid: String, userData: AbstractUser, payment: Payment) = auditAdmin.log(
    buildCommonPayment(userData, payment).virkailijaHenkiloOid(henkiloOid).build())

  private def buildCommonUserData(userData: User) = builder()
      .oppijaHenkiloOid(userData.personOid.getOrElse("<unknown>"))
      .setOperaatio(HakuPerusteetOperation.USERDATA)
      .email(userData.email)
      .firstName(userData.firstName.getOrElse("<none>"))
      .lastName(userData.lastName.getOrElse("<none>"))
      .birthDate(userData.birthDate.orNull)
      .personId(userData.personId.getOrElse("<none>"))
      .gender(userData.gender.getOrElse("<none>"))
      .nativeLanguage(userData.nativeLanguage.getOrElse("<none>"))
      .nationality(userData.nationality.getOrElse("<none>"))

  private def buildCommonEducation(userData: User, education: ApplicationObject) = builder()
      .oppijaHenkiloOid(userData.personOid.getOrElse("<unknown>"))
      .email(userData.email)
      .setOperaatio(HakuPerusteetOperation.EDUCATION)
      .hakuOid(education.hakuOid)
      .hakukohdeOid(education.hakukohdeOid)
      .educationLevel(education.educationLevel)
      .educationCountry(education.educationCountry)

  private def buildCommonPayment(userData: AbstractUser, payment: Payment) = builder()
      .oppijaHenkiloOid(userData.personOid.getOrElse("<unknown>"))
      .email(userData.email)
      .setOperaatio(statusToOperation(payment.status))
      .timestamp(payment.timestamp)
      .reference(payment.reference)
      .orderNumber(payment.orderNumber)
      .paymCallId(payment.paymCallId)

  private def statusToOperation(status: PaymentStatus) = status match {
    case PaymentStatus.started => HakuPerusteetOperation.PAYMENT_STARTED
    case PaymentStatus.ok => HakuPerusteetOperation.PAYMENT_OK
    case PaymentStatus.cancel => HakuPerusteetOperation.PAYMENT_CANCEL
    case PaymentStatus.error => HakuPerusteetOperation.PAYMENT_ERROR
  }
}
