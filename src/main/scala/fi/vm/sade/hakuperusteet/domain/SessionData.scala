package fi.vm.sade.hakuperusteet.domain

case class SessionData(session: Session, user: Option[AbstractUser], applicationObject: List[ApplicationObjectWithHakukausi], payment: List[Payment])
