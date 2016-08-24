package fi.vm.sade.hakuperusteet

import com.typesafe.config.Config
import fi.vm.sade.hakuperusteet.domain.Hakukausi.Hakukausi
import fi.vm.sade.hakuperusteet.hakuapp.HakuAppClient
import fi.vm.sade.hakuperusteet.tarjonta.Tarjonta

trait HakukausiService {

  val hakuAppClient: HakuAppClient
  val tarjonta: Tarjonta

  private def applicationSystemIdForHakemus(hakemusOid: String) = hakuAppClient.getApplicationSystemId(hakemusOid)
  private def applicationSystemIdForHakukohde(hakukohdeOid: String) = tarjonta.getApplicationObject(hakukohdeOid).hakuOid
  private def getPaymentKausi(applicationSystemId: String): Hakukausi = tarjonta.getApplicationSystem(applicationSystemId).hakukausi

  def getHakukausiForHakemus(hakemusOid: String) = getPaymentKausi(applicationSystemIdForHakemus(hakemusOid))
  def getHakukausiForHakukohde(hakukohdeOid: String) = getPaymentKausi(applicationSystemIdForHakukohde(hakukohdeOid))

}

object HakukausiService {
  def apply(config: Config, tarjontaService: Tarjonta) = new HakukausiService {
    override val hakuAppClient = HakuAppClient.init(config)
    override val tarjonta = tarjontaService
  }
}