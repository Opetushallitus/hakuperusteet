package fi.vm.sade.hakuperusteet.email

import java.io.{StringWriter, StringReader}
import java.text.DateFormat
import java.util.{Locale, Date}
import com.github.mustachejava.{Mustache, DefaultMustacheFactory}
import fi.vm.sade.hakuperusteet.util.Translate
import collection.JavaConversions._

object EmailTemplate {
  private val welcomeTemplate: Mustache = compileMustache("/email/welcome.mustache")
  private val receiptTemplate: Mustache = compileMustache("/email/receipt.mustache")
  private val paymentInfoFiTemplate: Mustache = compileMustache("/email/paymentInfoFi.mustache")
  private val paymentInfoSvTemplate: Mustache = compileMustache("/email/paymentInfoSv.mustache")
  private val paymentInfoEnTemplate: Mustache = compileMustache("/email/paymentInfoEn.mustache")

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

  def renderPaymentInfo(applicationOptionName: String, dueDate: Date, lang: String) = {
    val sw = new StringWriter()
    val template = lang match {
      case "fi" => paymentInfoFiTemplate
      case "sv" => paymentInfoSvTemplate
      case "en" => paymentInfoEnTemplate
    }
    template.execute(sw, mapAsJavaMap(Translate.getMap("email", "paymentInfo", lang)
      ++ Map(
      "verification-link-placeholder" -> "{{verification-link}}",
      "application-option-name" -> applicationOptionName,
      "due-date" -> DateFormat.getDateInstance(DateFormat.MEDIUM, new Locale(lang)).format(dueDate))))
    sw.toString
  }

  private def compileMustache(templateUrl: String) = {
    val templateString = scala.io.Source.fromInputStream(getClass.getResourceAsStream(templateUrl)).mkString
    new DefaultMustacheFactory().compile(new StringReader(templateString), templateUrl)
  }
}

case class WelcomeValues(fullName: String)
case class ReceiptValues(fullName: String, amount: String, reference: String)
