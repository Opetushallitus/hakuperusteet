package fi.vm.sade.hakuperusteet.integration.oppijanumerorekisteri


import java.time.LocalDate
import java.util.Date

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.Urls
import fi.vm.sade.hakuperusteet.domain.AbstractUser.User
import fi.vm.sade.hakuperusteet.domain.{AbstractUser, Google, Henkilo, OppijaToken}
import fi.vm.sade.hakuperusteet.integration.henkilo.IDP
import fi.vm.sade.hakuperusteet.util.{CasClientUtils, HttpUtil}
import fi.vm.sade.oppijanumerorekisteri.dto._
import org.http4s.Status.ResponseClass.Successful
import org.http4s.client.Client
import org.http4s.{Message, Method, Request, Response}
import org.json4s.Formats

import scala.collection.immutable.HashSet
import scala.collection.mutable
import scalaz.concurrent.Task

object ONRClient {
  def init(c: Config) = {
    new ONRClient(HttpUtil.casClient(c, "/oppijanumerorekisteri-service"))
  }
}

class ONRClient(client: Client) extends LazyLogging with CasClientUtils{
  private implicit val formats: Formats = fi.vm.sade.hakuperusteet.formatsHenkilo

  def addIdpForHenkilo(user: AbstractUser): Task[Iterable[IdentificationDto]] = {
    user.personOid match {
      case Some(oid) =>
        val idp = IdentificationDto.of(user.idpentityid.toString, user.email)
        client.fetchAs[Iterable[IdentificationDto]](req(oid, idp))(json4sOf[Iterable[IdentificationDto]])
      case _ => Task.fail(new IllegalArgumentException)
    }
  }

    /*
    * 1. Attempt to get henkilo by IDP (if not, add)
    * 2. Attempt finding by this user OID
    * 3. If fails, add henkilo and then fetch that
    */
  def updateHenkilo(user: User): Henkilo = {

    val create: User => Task[HenkiloDto] = (user: User) => {
      val dto = user2HenkiloCreateDto(user)
      val postreq = Request (
        method = Method.POST,
        uri = urlToUri(Urls.urls.url("oppijanumerorekisteri.henkilo"))
      ).withBody(dto)(json4sEncoderOf[HenkiloCreateDto])
      client.fetch(postreq) {
        case Successful(createresponse: Message) =>
          val oid = createresponse.as[String].unsafePerformSyncFor(1000l * 10)
          val dt = HenkiloDto(oidHenkilo = oid)
          Task.now(dt) // we can return half-empty dto since the function itself only returns Henkilo
        case _ => Task.fail(new IllegalArgumentException)
      }
    }

    val task: Task[HenkiloDto] = client.get[HenkiloDto](Urls.urls.url("oppijanumerorekisteri.henkilo.byidp", user.idpentityid.toString, user.email)) {
      case Successful(resp) => resp.as[HenkiloDto](json4sOf[HenkiloDto])
      case _ =>
        user.personOid match {
          case Some(oid) =>
            val url = Urls.urls.url("oppijanumerorekisteri.henkilo.byoid", oid)
            client.get[HenkiloDto](url) {
              case Successful(r) =>
                r.as[HenkiloDto](json4sOf[HenkiloDto])
              case _ =>
                create(user)
            }
          case _ => //just try adding and fetching
            create(user)
        }
    }
    val response = task.unsafePerformSyncFor(1000l * 10)
    Henkilo(response.oidHenkilo)
  }

  //returns person oid
  private def createHenkilo(user: User): (Response => Task[String]) => Task[String] = {
    val dto = user2HenkiloCreateDto(user)
    val postreq = Request (
      method = Method.POST,
      uri = urlToUri(Urls.urls.url("oppijanumerorekisteri.henkilo"))
    ).withBody(dto)(json4sEncoderOf[HenkiloCreateDto])
    client.fetch[String](postreq)
  }

  private def getOrError[T](user: User, fieldName: String, value: Option[T]) = value match {
    case Some(v) => v
    case None => throw new IllegalArgumentException(s"$fieldName is missing from user $user")
  }

  private def req(personOid: String, idp: IdentificationDto): Task[Request] = Request(
    method = Method.POST,
    uri = urlToUri(Urls.urls.url("oppijanumerorekisteri.henkilo.idp", personOid))
  ).withBody(idp)(json4sEncoderOf[IdentificationDto])

  private def user2HenkiloCreateDto(user: User): HenkiloCreateDto = {
    import collection.JavaConverters._
    var dto = new HenkiloCreateDto()
    dto.setEtunimet(user.firstName.getOrElse(throw new IllegalArgumentException("First name is required")))
    dto.setKutsumanimi(dto.getEtunimet)
    dto.setSukunimi(user.lastName.getOrElse(throw new IllegalArgumentException("Last name is required")))
    dto.setOppijanumero(user.personOid.getOrElse(""))
    val date = user.birthDate.getOrElse(throw new IllegalArgumentException("Birth date is required"))
    import java.time.ZoneId
    val localdate = date.toInstant.atZone(ZoneId.systemDefault).toLocalDate
    dto.setSyntymaaika(localdate)

    val nationalities: Set[String] = Set(user.nationality.orNull)
    val nationalitiesdto: Set[KansalaisuusDto] = nationalities.filter(p => p != null && !p.isEmpty).map(n => {
      val k= new KansalaisuusDto()
      k.setKansalaisuusKoodi(n)
      k
    })
    dto.setKansalaisuus(nationalitiesdto.asJava)

    dto
  }

  private def user2HenkiloDto(user: User): fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto = {
    import collection.JavaConverters._

    var dto = new fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto()
    dto.setOidHenkilo(user.personOid.getOrElse(""))

    dto.setEtunimet(user.firstName.getOrElse(throw new IllegalArgumentException("First name is required")))
    dto.setSukunimi(user.lastName.getOrElse(throw new IllegalArgumentException("Last name is required")))
    val date = user.birthDate.getOrElse(throw new IllegalArgumentException("Birth date is required"))
    import java.time.ZoneId
    val localdate = date.toInstant.atZone(ZoneId.systemDefault).toLocalDate
    dto.setSyntymaaika(localdate)

    dto.setSukupuoli(user.gender.getOrElse(throw new IllegalArgumentException("Gender is required")))

    val language = user.nativeLanguage.getOrElse(throw new IllegalArgumentException("Native language is required"))
    dto.setAidinkieli(new KielisyysDto(language, language))

    val nationalities = Set(user.nationality.getOrElse(throw new IllegalArgumentException("Nationality is required")))
    val nationalitiesdto: Set[KansalaisuusDto] = nationalities.map(n => {
      val k= new KansalaisuusDto()
      k.setKansalaisuusKoodi(n)
      k
    })
    dto.setKansalaisuus(nationalitiesdto.asJava)
    dto
  }
}

/**
  * Just a copy paste of fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto converted to scala because json converters
  * didn't understand the java dto for some reason in test cases
  */
case class HenkiloDto (
  oidHenkilo: String = null,
  hetu: String = null,
  passivoitu: Boolean = false,
  henkiloTyyppi: HenkiloTyyppi = null,
  etunimet: String = null,
  kutsumanimi: String = null,
  sukunimi: String = null,
  aidinkieli: KielisyysDto = null,
  asiointiKieli: KielisyysDto = null,
  kielisyys: Set[KielisyysDto] = new HashSet[KielisyysDto],
  kansalaisuus: Set[KansalaisuusDto] = new HashSet[KansalaisuusDto],
  kasittelijaOid: String = null,
  syntymaaika: LocalDate = null,
  sukupuoli: String = null,
  oppijanumero: String = null,
  turvakielto: Boolean = false,
  eiSuomalaistaHetua: Boolean = false,
  yksiloity: Boolean = false,
  yksiloityVTJ: Boolean = false,
  yksilointiYritetty: Boolean = false,
  duplicate: Boolean = false,
  created: Date = null,
  modified: Date = null,
  vtjsynced: Date = null,
  huoltaja: HenkiloDto = null,
  yhteystiedotRyhma: Set[YhteystiedotRyhmaDto] = new HashSet[YhteystiedotRyhmaDto]
)