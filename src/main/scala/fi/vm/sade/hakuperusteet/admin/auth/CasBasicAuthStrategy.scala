package fi.vm.sade.hakuperusteet.admin.auth

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.domain.{CasSession}
import fi.vm.sade.security.ldap._
import fi.vm.sade.utils.cas.CasClient
import org.scalatra.{ScalatraBase}
import org.scalatra.auth.ScentryStrategy
import com.typesafe.config.Config
import scala.util.{Failure, Success, Try}

class CasBasicAuthStrategy(protected override val app: ScalatraBase, cfg: Config) extends ScentryStrategy[CasSession] with LazyLogging {

  private def request = app.enrichRequest(app.request)

  val adminhost = cfg.getString("hakuperusteetadmin.url.base")
  val casClient = new CasClient(cfg.getString("hakuperusteet.cas.url"), org.http4s.client.blaze.defaultClient)
  val ldapClient = new LdapClient(LdapConfig(cfg.getString("cas.ldap.host"),cfg.getString("cas.ldap.userDn"),cfg.getString("cas.ldap.password"),cfg.getInt("cas.ldap.port")))

  def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[CasSession] = {
    Option(request.getParameter("ticket")) match {
      case Some(ticket) =>
        logger.debug(s"User is trying to authenticate with service ticket $ticket")
        Try(casClient.validateServiceTicket(adminhost)(ticket).run) match {
          case Success(uid) =>
            logger.info(s"User $uid found")
            ldapClient.findUser(uid) match {
              case Some(user) =>
                logger.info(s"User $uid is authenticated")
                val userSession = CasSession(None, user.oid, uid, user.roles, ticket, user.roles.contains("APP_HAKUPERUSTEETADMIN_REKISTERINPITAJA"))
                CasSessionDB.insert(userSession)
                Some(userSession)
              case _ =>
                logger.error("Unauthorized user", uid)
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
