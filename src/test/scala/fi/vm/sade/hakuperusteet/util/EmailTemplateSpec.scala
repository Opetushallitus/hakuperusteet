package fi.vm.sade.hakuperusteet.util

import fi.vm.sade.hakuperusteet.domain.{AbstractUser, Google}
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import fi.vm.sade.hakuperusteet.email.{EmailTemplate, WelcomeValues}
import org.scalatest.{FlatSpec, Matchers}

class EmailTemplateSpec extends FlatSpec with Matchers {
  val user = AbstractUser.user(None, None, "", Some("Ville"), Some("Ääkkönen"), None, None, Google, None, None, None, "en")

  it should "Get translated text" in {
    val welcome: String = EmailTemplate.renderWelcome(WelcomeValues(user.fullName), "en")
    welcome should include("Ville Ääkkönen")
    welcome should include("Dear")
    welcome should include("Your registration")
  }
}
