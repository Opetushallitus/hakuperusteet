package fi.vm.sade.hakuperusteet.domain

case class UserData(session: CasSession, user: User, applicationObject: Seq[ApplicationObject], payments: Seq[Payment], hasPaid: Boolean)

case class PartialUserData(session: CasSession, user: PartialUser, payments: Seq[Payment], hasPaid: Boolean, isPartialUserData: Boolean = true)

