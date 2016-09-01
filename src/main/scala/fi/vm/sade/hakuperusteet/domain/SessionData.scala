package fi.vm.sade.hakuperusteet.domain

case class SessionData(session: Session, user: Option[AbstractUser], applicationObject: List[ApplicationObjectWithHakumaksukausi], payment: List[Payment])
