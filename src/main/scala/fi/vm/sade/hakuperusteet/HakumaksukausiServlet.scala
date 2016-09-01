package fi.vm.sade.hakuperusteet

import com.typesafe.config.Config
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.Hakumaksukausi.Hakumaksukausi
import fi.vm.sade.hakuperusteet.google.GoogleVerifier
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import org.json4s.jackson.Serialization._

class HakumaksukausiServlet(config: Config, db: HakuperusteetDatabase, oppijanTunnistus: OppijanTunnistus, verifier: GoogleVerifier, hakumaksukausiService: HakumaksukausiService)
  extends HakuperusteetServlet(config, db, oppijanTunnistus, verifier) {

  get("/hakukohde/:hakukohdeoid") {
    failUnlessAuthenticated()
    write(HakumaksukausiRespone(hakumaksukausiService.getHakumaksukausiForHakukohde(params.get("hakukohdeoid").get)))
  }

  get("/hakemus/:hakemusoid") {
    failUnlessAuthenticated()
    write(HakumaksukausiRespone(hakumaksukausiService.getHakumaksukausiForHakemus(params.get("hakemusoid").get)))
  }

  case class HakumaksukausiRespone(hakumaksukausi:Hakumaksukausi)

}
