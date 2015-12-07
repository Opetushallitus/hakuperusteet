package fi.vm.sade.hakuperusteet

import javax.servlet.ServletContext

import fi.vm.sade.hakuperusteet.admin.{Synchronization, AdminServlet, PaymentSynchronization}
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.email.EmailSender
import fi.vm.sade.hakuperusteet.koodisto.Koodisto
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.rsa.RSASigner
import fi.vm.sade.hakuperusteet.swagger.{AdminSwagger, SwaggerServlet}
import fi.vm.sade.hakuperusteet.tarjonta.Tarjonta
import fi.vm.sade.hakuperusteet.validation.{UserValidator, ApplicationObjectValidator}
import org.scalatra.LifeCycle
import org.scalatra.swagger.{Swagger}

class ScalatraAdminBootstrap extends LifeCycle {
  implicit val swagger: Swagger = new AdminSwagger
  val config = Configuration.props
  val database = HakuperusteetDatabase.init(config)
  val countries = Koodisto.initCountries(config)
  val languages = Koodisto.initLanguages(config)
  val educations = Koodisto.initBaseEducation(config)
  val oppijanTunnistus = OppijanTunnistus.init(config)
  val tarjonta = Tarjonta.init(config)
  val signer = RSASigner.init(config)
  val applicationObjectValidator = ApplicationObjectValidator(countries, educations)
  val userValidator = UserValidator(countries, languages)
  Synchronization(config, database, tarjonta, countries, signer).start
  val paymentSynchronization = new PaymentSynchronization(config, database)
  paymentSynchronization.start

  override def init(context: ServletContext) {
    context mount(new TarjontaServlet(tarjonta), "/api/v1/tarjonta")
    context mount(new PropertiesServlet(config, countries, languages, educations), "/api/v1/properties")
    context mount(new AdminServlet("/webapp-admin/index.html",config, oppijanTunnistus, userValidator, applicationObjectValidator, database, countries), "/")
    context mount(new SwaggerServlet, "/api-docs/*")
  }

}
