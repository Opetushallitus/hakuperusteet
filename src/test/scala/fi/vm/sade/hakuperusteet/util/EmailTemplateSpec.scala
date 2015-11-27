package fi.vm.sade.hakuperusteet.util

import fi.vm.sade.hakuperusteet.domain.{IDPEntityId, User}
import fi.vm.sade.hakuperusteet.email.{WelcomeValues, EmailTemplate}
import org.scalatest.{FlatSpec, Matchers}

class EmailTemplateSpec extends FlatSpec with Matchers {
  val user = User(None, None, "", Some("Ville"), Some("Ääkkönen"), None, None, IDPEntityId.google, None, None, None, "en")

  it should "Get translated text" in {
    val welcome: String = EmailTemplate.renderWelcome(WelcomeValues(user.fullName), "en")
    welcome should include("Ville Ääkkönen")
    welcome should include("Dear")
    welcome should include("Your registration")
  }
}
