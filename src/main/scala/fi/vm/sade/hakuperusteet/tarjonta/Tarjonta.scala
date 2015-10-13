package fi.vm.sade.hakuperusteet.tarjonta

import com.typesafe.config.Config
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import fi.vm.sade.hakuperusteet.util.HttpUtil._

case class ApplicationObject(hakukohdeOid: String, name: String, providerName: String, baseEducations: List[String], description: String)
case class ApplicationSystem(hakuOid: String, formUrl: String)

case class Tarjonta(tarjontaBaseUrl: String) {
  implicit val formats = Serialization.formats(NoTypeHints)

  def getApplicationObject(hakukohdeOid: String) = Option(read[Result](urlToString(tarjontaBaseUrl + "/hakukohde/" + hakukohdeOid))).map(_.result)
    .map(r => ApplicationObject(r.oid, r.hakukohteenNimet.kieli_en, r.tarjoajaNimet.en, tarjontaUrisToKoodis(r.hakukelpoisuusvaatimusUris), r.lisatiedot.kieli_en)).get

  def getApplicationSystem(hakuOid: String) = Option(read[Result2](urlToString(tarjontaBaseUrl + "/haku/" + hakuOid))).map(_.result)
    .map(r => ApplicationSystem(r.oid, r.hakulomakeUri)).get

  private def tarjontaUrisToKoodis(tarjontaUri: List[String]) = tarjontaUri.map(_.split("_")(1))
}

object Tarjonta {
  def init(p: Config) = Tarjonta(p.getString("tarjonta.api.url"))
}

private case class Result(result: Hakukohde)
private case class Nimi(kieli_en: String)
private case class Nimi2(en: String)
private case class Hakukohde(oid: String, tarjoajaNimet: Nimi2, hakukohteenNimet: Nimi, hakukelpoisuusvaatimusUris: List[String], lisatiedot: Nimi, hakuOid: String)

private case class Result2(result: Haku)
private case class Haku(oid: String, hakulomakeUri: String)