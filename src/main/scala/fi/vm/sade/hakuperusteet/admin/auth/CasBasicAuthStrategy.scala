package fi.vm.sade.hakuperusteet.admin.auth

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.domain.CasSession
import fi.vm.sade.utils.cas.CasClient
import org.scalatra.ScalatraBase
import org.scalatra.auth.ScentryStrategy
import com.typesafe.config.Config
import fi.vm.sade.utils.kayttooikeus.KayttooikeusUserDetailsService
import fi.vm.sade.hakuperusteet.Urls

import scala.util.{Failure, Success, Try}

class CasBasicAuthStrategy(protected override val app: ScalatraBase, cfg: Config) extends ScentryStrategy[CasSession] with LazyLogging {

  private def request = app.enrichRequest(app.request)

  val adminhost = cfg.getString("hakuperusteetadmin.url.base")
  val casClient = new CasClient(cfg.getString("hakuperusteet.cas.url"), org.http4s.client.blaze.defaultClient)
  val userDetailsService = new KayttooikeusUserDetailsService(Urls.urls)

  def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[CasSession] = {
    Option(request.getParameter("ticket")) match {
      case Some(ticket) =>
        logger.debug(s"User is trying to authenticate with service ticket $ticket")
        Try(casClient.validateServiceTicket(adminhost)(ticket).run) match {
          case Success(uid) =>
            logger.info(s"User $uid found")
            userDetailsService.getUserByUsername(uid, "hakuperusteet") match {
              case Right(user) =>
                logger.info(s"User $uid is authenticated")
                val userSession = CasSession(None, user.oid, uid, user.roles, ticket, user.roles.contains("APP_HAKUPERUSTEETADMIN_REKISTERINPITAJA"))
                CasSessionDB.insert(userSession)
                Some(userSession)
              case Left(error) =>
                logger.error("Unauthorized user", uid, error)
                None
            }
          case Failure(t) =>
            logger.warn("Cas ticket rejected", t)
            None
        }
      case _ =>
        logger.warn("No Cas ticket found -> unauthenticated request")
        None
    }

  }

}
