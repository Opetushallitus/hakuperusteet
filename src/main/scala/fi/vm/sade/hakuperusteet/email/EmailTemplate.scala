package fi.vm.sade.hakuperusteet.email

import java.io.{StringWriter, StringReader}
import com.github.mustachejava.{Mustache, DefaultMustacheFactory}
import fi.vm.sade.hakuperusteet.util.Translate
import collection.JavaConversions._

object EmailTemplate {
  private val welcomeTemplate: Mustache = compileMustache("/email/welcome.mustache")
  private val receiptTemplate: Mustache = compileMustache("/email/receipt.mustache")
  private val paymentInfoTemplate: Mustache = compileMustache("/email/paymentInfo.mustache")

  def renderWelcome(values: WelcomeValues, lang: String) = {
    val sw = new StringWriter()
    welcomeTemplate.execute(sw, mapAsJavaMap(Translate.getMap("email.welcome",lang) ++ Map("values" ->  values)))
    sw.toString
  }

  def renderReceipt(values: ReceiptValues, lang: String) = {
    val sw = new StringWriter()
    receiptTemplate.execute(sw, mapAsJavaMap(Translate.getMap("email.receipt",lang) ++ Map("values" ->  values)))
    sw.toString
  }

  def renderPaymentInfo(fullName: String, lang: String) = {
    val sw = new StringWriter()
    paymentInfoTemplate.execute(sw, mapAsJavaMap(Translate.getMap("email", "paymentInfo", lang)
      ++ Map("verification-link-placeholder" -> "{{verification-link}}", "fullName" -> fullName)))
    sw.toString
  }

  private def compileMustache(templateUrl: String) = {
    val templateString = io.Source.fromInputStream(getClass.getResourceAsStream(templateUrl)).mkString
    new DefaultMustacheFactory().compile(new StringReader(templateString), templateUrl)
  }
}

case class WelcomeValues(fullName: String)
case class ReceiptValues(fullName: String, amount: String, reference: String)
