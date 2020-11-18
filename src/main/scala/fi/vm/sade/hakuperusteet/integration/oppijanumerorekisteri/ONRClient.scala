package fi.vm.sade.hakuperusteet.integration.oppijanumerorekisteri


import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.Urls
import fi.vm.sade.hakuperusteet.domain.AbstractUser.User
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.integration.IDP
import fi.vm.sade.hakuperusteet.util.{CasClientUtils, HttpUtil}
import org.http4s.Status.ResponseClass.Successful
import org.http4s._
import org.http4s.client.Client
import org.json4s.{Formats, JValue, Reader}
import org.json4s.DefaultReaders.StringReader
import org.http4s.json4s.native.jsonOf

import scala.collection.immutable.HashSet
import scalaz.concurrent.Task
import scalaz.{-\/, \/, \/-}

object ONRClient {
  def init(c: Config) = {
    val casClient = HttpUtil.casClient(c, "/oppijanumerorekisteri-service")
    new ONRClient(casClient)
  }
}

class ONRClient(client: Client) extends LazyLogging with CasClientUtils{
  private implicit val formats: Formats = fi.vm.sade.hakuperusteet.formatsHenkilo

  def addIdpForHenkilo(user: AbstractUser): Task[List[IDP]] = {
    user.personOid match {
      case Some(oid) =>
        val idp = IDP(idpEntityId = user.idpentityid.toString, identifier = user.email)
        client.fetchAs[List[IDP]](req(oid, idp))(json4sOf[List[IDP]])
      case _ => Task.fail(new IllegalArgumentException)
    }
  }

  def updateHenkilo(user: User): Henkilo = {
    def findOrCreateUser(user: User): Task[HenkiloDto] = {
      val dto: HenkiloPerustietoDto = userToPerustietoDto(user)
      val postreq = Request (
        method = Method.POST,
        uri = urlToUri(Urls.urls.url("oppijanumerorekisteri.findorcreate"))
        ).withBody(dto)(json4sEncoderOf[HenkiloPerustietoDto])
      client.fetch(postreq) {
        case Successful(findOrCreateResponse: Message) => // 200 = exists, 201 = created
          implicit val jsonFormats: Formats = formats
          val oid: \/[Throwable, String] = findOrCreateResponse.as[String](jsonOf[String](new Reader[String] {
            override def read(v: JValue): String = {
              StringReader.read(v \ "oidHenkilo")
            }
          })).unsafePerformSyncAttemptFor(1000 * 10L)
          val dt = HenkiloDto(oidHenkilo = oid.getOrElse(""))
          Task.now(dt) // we can return half-empty dto since the function itself only returns Henkilo
        case r@_ =>
          Task.fail(new IllegalArgumentException(r.toString()))
      }
    }

    val tasks: Task[HenkiloDto] = findOrCreateUser(user)
    val response: \/[Throwable, HenkiloDto] = tasks.unsafePerformSyncAttemptFor(1000l * 1l) //30 second timeout
    response match {
      case -\/(exception) => {
        logger.error("Error querying or creating user", exception)
        throw exception
      }
      case \/-(henkilo) => Henkilo(henkilo.oidHenkilo)
    }
  }
  //adds and IDP for given person
  private def req(personOid: String, idp: IDP): Task[Request] = Request(
    method = Method.POST,
    headers = Headers(Header("caller-id", "hakuperusteet")),
    uri = urlToUri(Urls.urls.url("oppijanumerorekisteri.henkilo.byoid.idp", personOid))
  ).withBody(idp)(json4sEncoderOf[IDP])

  private def user2HenkiloDto(user: User): HenkiloDto = {
    val date = user.birthDate.getOrElse(throw new IllegalArgumentException("Birth date is required"))
    import java.time.ZoneId
    val foo = ZoneId.systemDefault
    val instant = Instant.ofEpochMilli(date.getTime)
    val localdate = instant.atZone(foo).toLocalDate

    val oid = user.personOid.orNull
    val nationalities = Set(user.nationality.getOrElse(throw new IllegalArgumentException("Nationality is required")))
    val nationalitiesdto: Set[KansalaisuusDto] = nationalities.map(n => {
      KansalaisuusDto(n)
    })
    val language = user.nativeLanguage.getOrElse(throw new IllegalArgumentException("Native language is required"))
    val hetu = user.personId match {
      case Some(x) =>
        logger.info(s"user2HenkiloDto: Found hetu for HenkiloDto with oid $oid")
        x
      case None =>
        logger.warn(s"user2HenkiloDto: Creating HenkiloDto with oid $oid with null hetu")
        null
    }
    HenkiloDto(
      oidHenkilo = oid,
      hetu = hetu,
      etunimet = user.firstName.getOrElse(throw new IllegalArgumentException("First name is required")),
      kutsumanimi = user.firstName.getOrElse(throw new IllegalArgumentException("First name is required")),
      sukunimi = user.lastName.getOrElse(throw new IllegalArgumentException("Last name is required")),
      syntymaaika = localdate.format(DateTimeFormatter.ISO_DATE),
      kansalaisuus = nationalitiesdto,
      henkiloTyyppi = "OPPIJA",
      aidinkieli = KielisyysDto(kieliKoodi = language.toLowerCase),
      sukupuoli = user.gender.getOrElse(throw new IllegalArgumentException("Gender is required"))

    )
  }

  def userToPerustietoDto(user: User): HenkiloPerustietoDto = {
    val date = user.birthDate.getOrElse(throw new IllegalArgumentException("Birth date is required"))
    import java.time.ZoneId
    val foo = ZoneId.systemDefault
    val instant = Instant.ofEpochMilli(date.getTime)
    val localdate = instant.atZone(foo).toLocalDate

    val nationalities = Set(user.nationality.getOrElse(throw new IllegalArgumentException("Nationality is required")))
    val nationalitiesDto: Set[KansalaisuusDto] = nationalities.map(n => {
      KansalaisuusDto(n)
    })

    val language = user.nativeLanguage.getOrElse(throw new IllegalArgumentException("Native language is required"))
    val kielisyysDto = new fi.vm.sade.oppijanumerorekisteri.dto.KielisyysDto()
    kielisyysDto.setKieliKoodi(language.toLowerCase)

    val dto = HenkiloPerustietoDto(
      hetu = user.personId.orNull,
      oidHenkilo = user.personOid.orNull,
      etunimet = user.firstName.getOrElse(throw new IllegalArgumentException("First name is required")),
      kutsumanimi = user.firstName.getOrElse(throw new IllegalArgumentException("First name is required")),
      sukunimi = user.lastName.getOrElse(throw new IllegalArgumentException("Last name is required")),
      syntymaaika = localdate.format(DateTimeFormatter.ISO_DATE),
      kansalaisuus = nationalitiesDto,
      henkiloTyyppi = "OPPIJA",
      aidinkieli = KielisyysDto(kieliKoodi = language.toLowerCase),
      sukupuoli = user.gender.getOrElse(throw new IllegalArgumentException("Gender is required")),
      identifications = HashSet[IdentificationDto](IdentificationDto(user.idpentityid.toString, user.email))
    )
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
  henkiloTyyppi: String = null,
  etunimet: String = null,
  kutsumanimi: String = null,
  sukunimi: String = null,
  aidinkieli: KielisyysDto = null,
  asiointiKieli: KielisyysDto = null,
  kielisyys: Set[KielisyysDto] = new HashSet[KielisyysDto],
  kansalaisuus: Set[KansalaisuusDto] = new HashSet[KansalaisuusDto],
  kasittelijaOid: String = null,
  syntymaaika: String = null,
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

case class HenkiloPerustietoDto(
  aidinkieli: KielisyysDto,
  etunimet: String,
  henkiloTyyppi: String,
  hetu: String,
  identifications: Set[IdentificationDto],
  kutsumanimi: String,
  oidHenkilo: String,
  sukunimi: String,
  sukupuoli: String,
  kielisyys: Set[KielisyysDto] = new HashSet[KielisyysDto],
  kansalaisuus: Set[KansalaisuusDto] = new HashSet[KansalaisuusDto],
  syntymaaika: String = null
)
case class KielisyysDto(kieliKoodi: String = null, kieliTyyppi: String = null)
case class KansalaisuusDto(kansalaisuusKoodi: String = null)
case class YhteystiedotRyhmaDto(ryhmaAlkuperaTieto: String = null, readOnly: Boolean = false, yhteystieto: Set[YhteystietoDto] = Set.empty)
case class YhteystietoDto(yhteystietoArvo: String, yhteystietoTyyppi: YhteystietoTyyppi)
case class IdentificationDto(identifier: String, idpEntityId: String)

sealed trait YhteystietoTyyppi

case object YHTEYSTIETO_SAHKOPOSTI extends YhteystietoTyyppi
case object YHTEYSTIETO_PUHELINNUMERO extends YhteystietoTyyppi
case object YHTEYSTIETO_MATKAPUHELINNUMERO extends YhteystietoTyyppi
case object YHTEYSTIETO_KATUOSOITE extends YhteystietoTyyppi
case object YHTEYSTIETO_KUNTA extends YhteystietoTyyppi
case object YHTEYSTIETO_POSTINUMERO extends YhteystietoTyyppi
case object YHTEYSTIETO_KAUPUNKI extends YhteystietoTyyppi
case object YHTEYSTIETO_MAA extends YhteystietoTyyppi
