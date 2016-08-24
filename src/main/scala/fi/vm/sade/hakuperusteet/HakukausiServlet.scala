package fi.vm.sade.hakuperusteet

import com.typesafe.config.Config
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.Hakukausi.Hakukausi
import fi.vm.sade.hakuperusteet.google.GoogleVerifier
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import org.json4s.jackson.Serialization._

class HakukausiServlet(config: Config, db: HakuperusteetDatabase, oppijanTunnistus: OppijanTunnistus, verifier: GoogleVerifier, hakukausiService: HakukausiService)
  extends HakuperusteetServlet(config, db, oppijanTunnistus, verifier) {

  get("/hakukohde/:hakukohdeoid") {
    failUnlessAuthenticated()
    write(HakukausiRespone(hakukausiService.getHakukausiForHakukohde(params.get("hakukohdeoid").get)))
  }

  get("/hakemus/:hakemusoid") {
    failUnlessAuthenticated()
    write(HakukausiRespone(hakukausiService.getHakukausiForHakemus(params.get("hakemusoid").get)))
  }

  case class HakukausiRespone(hakukausi:Hakukausi)

}
