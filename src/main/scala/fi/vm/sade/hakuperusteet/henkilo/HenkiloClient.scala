package fi.vm.sade.hakuperusteet.henkilo

import java.util.Date

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.domain.IDPEntityId.IDPEntityId
import fi.vm.sade.hakuperusteet.domain.{Henkilo, IDPEntityId, User}
import fi.vm.sade.hakuperusteet.util.CasClientUtils
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasParams}
import org.http4s.Uri._
import org.http4s._
import org.http4s.client.Client

import scalaz.concurrent.Task

object HenkiloClient {
  def init(c: Config) = {
    val host = c.getString("hakuperusteet.cas.url")
    val username = c.getString("hakuperusteet.user")
    val password = c.getString("hakuperusteet.password")
    val casClient = new CasClient(host, org.http4s.client.blaze.defaultClient)
    val casParams = CasParams("/authentication-service", username, password)
    new HenkiloClient(host, new CasAuthenticatingClient(casClient, casParams, org.http4s.client.blaze.defaultClient))
  }
}

case class IdpUpsertRequest(personOid: String, email: String, idpEntityId: String = IDPEntityId.oppijaToken.toString)

case class IDP(idpEntityId: IDPEntityId, identifier: String)

case class FindOrCreateUser(id: Option[Int], personOid: Option[String], email: String,
                            firstName: String, lastName: String, birthDate: Date, personId: Option[String],
                            idpEntitys: List[IDP], gender: String, nativeLanguage: String, nationality: String)

object FindOrCreateUser {
  private def validateUserObject(user: User) = {
    def validateOptionalField[T](fieldName: String, value: Option[T]) = if (value.isEmpty)
      throw new IllegalArgumentException(s"$fieldName is missing from user $user")
    validateOptionalField("firstName", user.firstName)
    validateOptionalField("lastName", user.lastName)
    validateOptionalField("birthDate", user.birthDate)
    validateOptionalField("gender", user.gender)
    validateOptionalField("nativeLanguage", user.nativeLanguage)
    validateOptionalField("nationality", user.nationality)
  }
  def apply(user: User): FindOrCreateUser = {
    validateUserObject(user)
    FindOrCreateUser(user.id, user.personOid, user.email, user.firstName.get,
      user.lastName.get, user.birthDate.get, user.personId, List(IDP(user.idpentityid, user.email)),
      user.gender.get, user.nativeLanguage.get, user.nationality.get)
  }
}

class HenkiloClient(henkiloServerUrl: String, client: Client) extends LazyLogging with CasClientUtils {
  implicit val formats = fi.vm.sade.hakuperusteet.formatsHenkilo

  def upsertHenkilo(user: FindOrCreateUser) = client.prepAs[Henkilo](req(user))(json4sOf[Henkilo]).run

  def upsertIdpEntity(user: User): Task[Henkilo] = user.personOid match {
    case Some(oid) => client.prepAs[Henkilo](req(IdpUpsertRequest(oid, user.email)))(json4sOf[Henkilo])
    case _ => Task.fail(new IllegalArgumentException)
  }

  private def req(idpUpsert: IdpUpsertRequest) = Request(
    method = Method.POST,
    uri = resolve(urlToUri(henkiloServerUrl), Uri(path = "/authentication-service/resources/s2s/hakuperusteet/idp"))
  ).withBody(idpUpsert)(json4sEncoderOf[IdpUpsertRequest])

  private def req(user: FindOrCreateUser) = Request(
    method = Method.POST,
    uri = resolve(urlToUri(henkiloServerUrl), Uri(path = "/authentication-service/resources/s2s/hakuperusteet"))
  ).withBody(user)(json4sEncoderOf[FindOrCreateUser])
}
