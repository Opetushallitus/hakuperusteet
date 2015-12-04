package fi.vm.sade.hakuperusteet.domain

case class UserData(user: User, applicationObject: Seq[ApplicationObject], payments: Seq[Payment], hasPaid: Boolean)

case class PartialUserData(user: PartialUser, payments: Seq[Payment], hasPaid: Boolean, isPartialUserData: Boolean = true)

