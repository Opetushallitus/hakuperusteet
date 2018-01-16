package fi.vm.sade.hakuperusteet.integration.henkilo

import java.time.LocalDate
import java.util.Date

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.Urls
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.util.{CasClientUtils, HttpUtil}
import org.http4s._
import org.http4s.client.Client
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import org.json4s.Formats

import scalaz.concurrent.Task

@deprecated
object HenkiloClient {
  /*def init(c: Config) = {
    new HenkiloClient(HttpUtil.casClient(c, "/authentication-service"))
  }*/
}

case class IdpUpsertRequest(personOid: String, email: String, idpEntityId: String = OppijaToken.toString)

case class IDP(idpEntityId: IDPEntityId, identifier: String)

case class FindOrCreateUser(id: Option[Int], personOid: Option[String], email: String,
                            firstName: String, lastName: String, birthDate: Date, personId: Option[String],
                            idpEntitys: List[IDP], gender: String, nativeLanguage: String, nationality: String)

/*
case class KielisyysDto (kieliKoodi: String = "", kieliTyyppi: String = "")

case class HenkiloPerustietoDto (oidHenkilo: String = "", externalIds: List[String] = List.empty,
                                 identifications: List[IDP] = List.empty, etunimet: String = "",
                                 kutsumanimi: String = "", sukunimi: String = "",
                                  //optionals below
                                 hetu: Option[String] = None, syntymaaika: Option[LocalDate] = None,
                                 aidinkieli: Option[KielisyysDto] = None, asiointiKieli: Option[KielisyysDto] = None,
                                 kansalaisuus: Option[Set[String]] = None, henkiloTyyppi: Option[HenkiloTyyppi] = None,
                                 sukupuoli: Option[String] = None, modified: Option[Date] = None)
*/


class HenkiloTyyppi extends Enumeration {
  val OPPIJA, VIRKAILIJA, PALVELU = Value
}


object FindOrCreateUser {
  def apply(user: User): FindOrCreateUser = {

    def getOrError[T](fieldName: String, value: Option[T]) = value match {
      case Some(v) => v
      case None => throw new IllegalArgumentException(s"$fieldName is missing from user $user")
    }
    FindOrCreateUser(user.id, user.personOid, user.email, getOrError("firstName", user.firstName),
      getOrError("lastName", user.lastName), getOrError("birthDate", user.birthDate), user.personId,
      List(IDP(user.idpentityid, user.email)), getOrError("gender", user.gender),
      getOrError("nativeLanguage", user.nativeLanguage), getOrError("nationality", user.nationality))
  }
}

object IfGoogleAddEmailIDP {
  def apply(user: User): FindOrCreateUser = {
    val u = FindOrCreateUser(user)
    u.copy(idpEntitys = user.idpentityid match {
      case Google => IDP(OppijaToken, user.email) +: u.idpEntitys
      case OppijaToken => u.idpEntitys
    })
  }
}
@deprecated
class HenkiloClient(client: Client) extends LazyLogging with CasClientUtils {
  private implicit val formats: Formats = fi.vm.sade.hakuperusteet.formatsHenkilo

  //def upsertHenkilo(user: FindOrCreateUser): Henkilo = client.fetchAs[Henkilo](req(user))(json4sOf[Henkilo]).unsafePerformSync
/*
  def upsertIdpEntity(user: AbstractUser): Task[Iterable[IDP]] = {
    user.personOid match {
      case Some(oid) => client.fetchAs[Iterable[IDP]](req(IdpUpsertRequest(oid, user.email)))(json4sOf[Iterable[IDP]]) //hakee tai lisää henkilon idp tokenin ja palauttaa sen, email == identifier
      case _ => Task.fail(new IllegalArgumentException)
    }
  }

  def findOrCreateUser2HenkiloPerustietoDto(foc: FindOrCreateUser): HenkiloPerustietoDto = {
    val asiointikieli = KielisyysDto(kieliKoodi = foc.nativeLanguage, kieliTyyppi = foc.nativeLanguage)
    import java.time.ZoneId
    val input: Date = new Date()
    val date: LocalDate = input.toInstant.atZone(ZoneId.systemDefault).toLocalDate
    HenkiloPerustietoDto(
      etunimet = foc.firstName,
      sukunimi = foc.lastName,
      oidHenkilo = foc.personId.orNull,
      identifications = foc.idpEntitys,
      sukupuoli = Option(foc.gender),
      asiointiKieli = Option(asiointikieli),
      syntymaaika = Option(date)

    )
  }
*/
  private def req(user: FindOrCreateUser): Task[Request] = Request(
    method = Method.POST,
    uri = urlToUri(Urls.urls.url("authentication-service.findOrCreateUser")) // POST ONR /s2s/findOrCreateHenkiloPerustieto
  ).withBody(user)(json4sEncoderOf[FindOrCreateUser])
}
