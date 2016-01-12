package fi.vm.sade.hakuperusteet

import java.util.Date

import com.typesafe.config.Config
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.email.EmailSender
import fi.vm.sade.hakuperusteet.google.GoogleVerifier
import fi.vm.sade.hakuperusteet.koodisto._
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.rsa.RSASigner
import org.joda.time.DateTime

trait ServletTestDependencies extends DBSupport with DummyDataTestDependency {
  val config = Configuration.props
  val database = HakuperusteetDatabase(config)
  val verifier = new DummyVerifier
  val countries = Countries(List(SimplifiedCode("032",List(SimplifiedLangValue("fi","Argentiina"))), SimplifiedCode("246", List(SimplifiedLangValue("fi", "Suomi")))),
    List("246"))
  val languages = Languages(List(SimplifiedCode("AK",List(SimplifiedLangValue("fi","AK")))))
  val educations = Educations(List(
    SimplifiedCode("102", List(SimplifiedLangValue("fi", "Alempi korkeakoulututkinto")))))
  val oppijanTunnistus: OppijanTunnistus = new DummyOppijanTunnistus(config)
  val emailSender = EmailSender.init(config)
  val signer = new DummyRSASigner(config)

}

trait DummyDataTestDependency {
  self: ServletTestDependencies =>

  private val rnd_number_gen = scala.util.Random
  def generateRandomPersonOid = s"person_oid_${rnd_number_gen.nextInt(10000000)}"
  def generateNumSeq = "%09d".format(Math.abs(rnd_number_gen.nextInt()))

  def generateRandomUser = Users.generateRandomUser

  def now = new DateTime()

  def generatePaymentToUser(u: AbstractUser) = {
    val reference = generateNumSeq
    val orderNumber = generateNumSeq
    val paymCallId = generateNumSeq
    Payment(None, u.personOid.get, new Date(), reference, orderNumber, paymCallId, PaymentStatus.started, None)
  }
}

class DummyVerifier() extends GoogleVerifier("", "") {
  override def verify(token: String) = true
}

class DummyOppijanTunnistus(c: Config) extends OppijanTunnistus(c) {
  override def createToken(email: String, hakukohdeOid: String, uiLang: String) = "dummyLoginToken"
}

class DummyRSASigner(c: Config) extends RSASigner(c) {
  override def signData(dataString: String) = {
    "dummySignedString"
  }
}
