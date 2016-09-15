package fi.vm.sade.hakuperusteet

import javax.servlet.ServletContext

import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.email.EmailSender
import fi.vm.sade.hakuperusteet.google.GoogleVerifier
import fi.vm.sade.hakuperusteet.koodisto.Koodisto
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.rsa.RSASigner
import fi.vm.sade.hakuperusteet.tarjonta.Tarjonta
import fi.vm.sade.hakuperusteet.validation.{UserValidator, ApplicationObjectValidator}
import org.scalatra.LifeCycle

import scala.concurrent.ExecutionContext

class ScalatraBootstrap extends LifeCycle {
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  val config = Configuration.props
  val database = HakuperusteetDatabase(config)
  val verifier = GoogleVerifier.init(config)
  val signer = RSASigner.init(config, "hakuperusteet")
  val countries = Koodisto.initCountries(config)
  val languages = Koodisto.initLanguages(config)
  val educations = Koodisto.initBaseEducation(config)
  val tarjonta = Tarjonta.init()
  val oppijanTunnistus = OppijanTunnistus.init(config)
  val emailSender = EmailSender.init(config)
  val applicationObjectValidator = ApplicationObjectValidator(countries, educations)
  val userValidator = UserValidator(countries, languages)
  val hakumaksukausiService = HakumaksukausiService(config, tarjonta)

  override def init(context: ServletContext) {
    context.initParameters("org.scalatra.cors.enable") = "false"
    context mount(new IndexServlet, "/app")
    context mount(new IndexServlet, "/ao")
    context mount(new VetumaServlet(config, database, oppijanTunnistus, verifier, emailSender, tarjonta, hakumaksukausiService), "/api/v1/vetuma")
    context mount(new TarjontaServlet(tarjonta), "/api/v1/tarjonta")
    context mount(new PropertiesServlet(config, countries, languages, educations), "/api/v1/properties")
    context mount(new SessionServlet(config, database, oppijanTunnistus, verifier, userValidator, applicationObjectValidator, emailSender, hakumaksukausiService), "/api/v1/session")
    context mount(new FormRedirectServlet(config, database, oppijanTunnistus, verifier, signer, countries, tarjonta), "/api/v1/form")
    context mount(new HakumaksukausiServlet(config, database, oppijanTunnistus, verifier, hakumaksukausiService), "/api/v1/hakumaksukausi")
  }
}
