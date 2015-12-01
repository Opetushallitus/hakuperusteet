package fi.vm.sade.hakuperusteet

import com.typesafe.config.Config
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.email.EmailSender
import fi.vm.sade.hakuperusteet.google.GoogleVerifier
import fi.vm.sade.hakuperusteet.koodisto._
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.rsa.RSASigner

trait ServletTestDependencies extends DBSupport {
  val config = Configuration.props
  val database = HakuperusteetDatabase.init(config)
  val verifier = new DummyVerifier
  val countries = Countries(List(SimplifiedCode("032",List(SimplifiedLangValue("fi","032")))), List.empty)
  val languages = Languages(List(SimplifiedCode("AK",List(SimplifiedLangValue("fi","AK")))))
  val educations = Educations(List.empty[SimplifiedCode])
  val oppijanTunnistus = new DummyOppijanTunnistus(config)
  val emailSender = EmailSender.init(config)
  val signer = new DummyRSASigner(config)
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