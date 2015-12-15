package fi.vm.sade.hakuperusteet.domain

case class CasSession(id: Option[Int], oid: String, username: String, roles: List[String], ticket: String, oph: Boolean = false)
