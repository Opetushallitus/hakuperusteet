package fi.vm.sade.hakuperusteet

import com.typesafe.config.Config
import fi.vm.sade.hakuperusteet.domain.Hakumaksukausi.Hakumaksukausi
import fi.vm.sade.hakuperusteet.hakuapp.HakuAppClient
import fi.vm.sade.hakuperusteet.tarjonta.Tarjonta

trait HakumaksukausiService {

  val hakuAppClient: HakuAppClient
  val tarjonta: Tarjonta

  private def applicationSystemIdForHakemus(hakemusOid: String) = hakuAppClient.getApplicationSystemId(hakemusOid)
  private def applicationSystemIdForHakukohde(hakukohdeOid: String) = tarjonta.getApplicationObject(hakukohdeOid).hakuOid
  private def getPaymentKausi(applicationSystemId: String): Hakumaksukausi = tarjonta.getApplicationSystem(applicationSystemId).hakumaksukausi

  def getHakumaksukausiForHakemus(hakemusOid: String) = getPaymentKausi(applicationSystemIdForHakemus(hakemusOid))
  def getHakumaksukausiForHakukohde(hakukohdeOid: String) = getPaymentKausi(applicationSystemIdForHakukohde(hakukohdeOid))

}

object HakumaksukausiService {
  def apply(config: Config, tarjontaService: Tarjonta) = new HakumaksukausiService {
    override val hakuAppClient = HakuAppClient.init(config)
    override val tarjonta = tarjontaService
  }
}