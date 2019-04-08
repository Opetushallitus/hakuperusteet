package fi.vm.sade.hakuperusteet.domain

import java.util.Date

sealed trait IDPEntityId {
  def toString: String
}

object IDPEntityId {
  def withName(name: String): IDPEntityId = name match {
    case "google" => Google
    case "oppijaToken" => OppijaToken
  }
}

case object Google extends IDPEntityId {
  override def toString: String = "google"
}

case object OppijaToken extends IDPEntityId {
  override def toString: String = "oppijaToken"
}

sealed trait AbstractUser {
  def id: Option[Int]
  def idpentityid: IDPEntityId
  def email: String
  def fullName: String
  def personOid: Option[String]
  def uiLang: String

  def lang: String = if (List("fi", "sv", "en") contains uiLang) uiLang else "en"
}



object AbstractUser {

  case class PartialUser private[AbstractUser](id: Option[Int],
                                               personOid: Option[String],
                                               email: String,
                                               idpentityid: IDPEntityId,
                                               uiLang:String,
                                               partialUser: Boolean = true)
    extends AbstractUser {
      def fullName: String = email
  }

  case class User private[AbstractUser](id: Option[Int],
                                        personOid: Option[String],
                                        email: String,
                                        firstName: Option[String],
                                        lastName: Option[String],
                                        birthDate: Option[Date],
                                        personId: Option[String],
                                        idpentityid: IDPEntityId,
                                        gender: Option[String],
                                        nativeLanguage: Option[String],
                                        nationality: Option[String],
                                        uiLang: String)
    extends AbstractUser {
      def fullName: String = (firstName, lastName) match {
        case (Some(firstName), Some(lastName)) => s"$firstName $lastName"
        case _ => "<no name>"
    }
  }

  def partialUser(id: Option[Int], personOid: Option[String], email: String, idpentityid: IDPEntityId, uiLang:String) = {
    PartialUser(id, personOid, email.toLowerCase(), idpentityid, uiLang)
  }

  def user(id: Option[Int], personOid: Option[String], email: String, firstName: Option[String], lastName: Option[String], birthDate: Option[Date],
           personId: Option[String], idpentityid: IDPEntityId, gender: Option[String], nativeLanguage: Option[String], nationality: Option[String],
           uiLang: String) = {
    User(id, personOid, email.toLowerCase(), firstName, lastName, birthDate, personId, idpentityid, gender, nativeLanguage, nationality, uiLang)
  }
}