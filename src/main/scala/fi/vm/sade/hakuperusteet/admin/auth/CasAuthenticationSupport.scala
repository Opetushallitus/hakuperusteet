package fi.vm.sade.hakuperusteet.admin.auth

import java.net.URLEncoder

import fi.vm.sade.hakuperusteet.admin.AdminServlet
import fi.vm.sade.hakuperusteet.domain.CasSession
import org.scalatra.auth.strategy.BasicAuthSupport
import org.scalatra.auth.{ScentryConfig, ScentrySupport}

object CasSessionDB {
  private val db = new java.util.concurrent.ConcurrentHashMap[String,CasSession]()

  def insert(session: CasSession) = {
    db.put(session.ticket, session)
  }

  def get(ticket: String) = {
    db.get(ticket)
  }

  def invalidate(ticket:String) = {
    db.remove(ticket)
  }
}

trait CasAuthenticationSupport extends ScentrySupport[CasSession] with BasicAuthSupport[CasSession] { self: AdminServlet =>
  override abstract def initialize(config: ConfigT) = super.initialize(config)
  protected val scentryConfig = new ScentryConfig {}.asInstanceOf[ScentryConfiguration]

  protected def fromSession = { case ticket: String => CasSessionDB.get(ticket) }
  protected def toSession = { case usr: CasSession => usr.ticket }

  override protected def registerAuthStrategies = {
    scentry.register("CAS", app => new CasBasicAuthStrategy(app, cfg))
  }

  val redirectUrl = cfg.getString("hakuperusteet.cas.url") + "/cas/login?service=" + URLEncoder.encode(cfg.getString("hakuperusteetadmin.url.base"), "UTF-8")

  def failUnlessAuthenticated() = if (!isAuthenticated) redirect(redirectUrl)
}
