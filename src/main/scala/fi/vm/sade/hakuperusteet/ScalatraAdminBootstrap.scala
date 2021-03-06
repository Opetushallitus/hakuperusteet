package fi.vm.sade.hakuperusteet

import javax.servlet.ServletContext

import fi.vm.sade.hakuperusteet.admin.{AdminServlet, PaymentSynchronization, Synchronization}
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.email.EmailSender
import fi.vm.sade.hakuperusteet.integration.oppijanumerorekisteri.ONRClient
import fi.vm.sade.hakuperusteet.koodisto.{Countries, Koodisto}
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.rsa.RSASigner
import fi.vm.sade.hakuperusteet.swagger.{AdminSwagger, SwaggerServlet}
import fi.vm.sade.hakuperusteet.tarjonta.Tarjonta
import fi.vm.sade.hakuperusteet.validation.{ApplicationObjectValidator, UserValidator}
import org.scalatra.LifeCycle
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext

class ScalatraAdminBootstrap extends LifeCycle {
  implicit val swagger: Swagger = new AdminSwagger
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  val config = Configuration.props
  val onrClient = ONRClient.init(config)
  val database = HakuperusteetDatabase(config)
  val countries = Koodisto.initCountries(config)
  val oppijanTunnistus = OppijanTunnistus.init(config)
  val tarjonta = Tarjonta.init()
  val paymentService = PaymentService(database)
  val languages = Koodisto.initLanguages(config)
  val educations = Koodisto.initBaseEducation(config)
  val signer = RSASigner.init(config, "hakuperusteetadmin")
  val applicationObjectValidator = ApplicationObjectValidator(countries, educations)
  val userValidator = UserValidator(countries, languages)
  val emailSender = EmailSender.init(config)
  val hakumaksukausiService = HakumaksukausiService(config, tarjonta)
  Synchronization(config, database, tarjonta, countries, signer, hakumaksukausiService).start
  val paymentSynchronization = new PaymentSynchronization(config, database, emailSender)
  paymentSynchronization.start

  override def init(context: ServletContext) {
    context.initParameters("org.scalatra.cors.enable") = "false"
    context mount(new TarjontaServlet(tarjonta), "/api/v1/tarjonta")
    context mount(new PropertiesServlet(config, countries, languages, educations), "/api/v1/properties")
    context mount(new AdminServlet("/webapp-admin/index.html",config, userValidator, applicationObjectValidator,
      UserService(executionContext, onrClient, database),
      paymentService,
      ApplicationObjectService(executionContext, countries, database, oppijanTunnistus, paymentService, tarjonta)), "/")
    context mount(new SwaggerServlet, "/api-docs/*")
  }
}
