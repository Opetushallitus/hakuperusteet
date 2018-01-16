package fi.vm.sade.hakuperusteet.integration.oppijanumerorekisteri


import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.Urls
import fi.vm.sade.hakuperusteet.domain.{AbstractUser, Henkilo}
import fi.vm.sade.hakuperusteet.util.{CasClientUtils, HttpUtil}
import fi.vm.sade.oppijanumerorekisteri.dto.{HenkiloDto, IdentificationDto, KielisyysDto}
import org.http4s.Status.ResponseClass.Successful
import org.http4s.client.Client
import org.http4s.{Method, Request}
import org.json4s.Formats

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
    * 1. Attempt to get henkilo by IDP
    * 2. Attempt finding by this user OID
    * 3. If fails, add henkilo and then fetch that
    */
  def updateHenkilo(user: AbstractUser): Henkilo = {
    //todo add google idp entitys if google idp
    val task: Task[HenkiloDto] = client.get[HenkiloDto](Urls.urls.url("oppijanumerorekisteri.henkilo.byidp", user.idpentityid.toString, user.email)) {
      case Successful(resp) => resp.as[HenkiloDto](json4sOf[HenkiloDto])
      case _ =>
        user.personOid match {
          case Some(oid) =>
            client.get[HenkiloDto](Urls.urls.url("oppijanumerorekisteri.henkilo.byoid", oid)) {
              case Successful(r) => r.as[HenkiloDto](json4sOf[HenkiloDto])
              case _ =>
                val dto = user2HenkiloDto(user)
                val postreq = Request (
                  method = Method.POST,
                  uri = urlToUri(Urls.urls.url("oppijanumerorekisteri.henkilo"))
                ).withBody(dto)(json4sEncoderOf[HenkiloDto])
                client.fetch(postreq) {
                  case Successful(_) =>
                    client.get[HenkiloDto](Urls.urls.url("oppijanumerorekisteri.henkilo.byoid", oid)) {
                      case Successful(r) => r.as[HenkiloDto](decoder = json4sOf[HenkiloDto])
                      case _ => Task.fail(new IllegalArgumentException)
                    }
                  case _ => Task.fail(new IllegalArgumentException)
                }
            }
          case _ => Task.fail(new IllegalArgumentException)
        }
    }
    Henkilo(task.unsafePerformSync.getOidHenkilo)
  }

  private def req(personOid: String, idp: IdentificationDto): Task[Request] = Request(
    method = Method.POST,
    uri = urlToUri(Urls.urls.url("oppijanumerorekisteri.henkilo.idp", personOid))
  ).withBody(idp)(json4sEncoderOf[IdentificationDto])

  private def user2HenkiloDto(user: AbstractUser): HenkiloDto = {
    import collection.JavaConverters._

    var dto = new HenkiloDto()
    dto.setOidHenkilo(user.personOid.getOrElse(""))

    val langs: mutable.Set[KielisyysDto] = mutable.Set(new KielisyysDto(user.lang, user.lang))
    dto.setKielisyys(langs.asJava)
    dto.setSukunimi(user.fullName)
    dto
  }
}
