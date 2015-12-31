package fi.vm.sade.hakuperusteet.tarjonta

import java.util.Date

import com.typesafe.config.Config
import fi.vm.sade.hakuperusteet.{OrganizationGroupOid, OrganizationOid, Oid, Constants}
import fi.vm.sade.hakuperusteet.util.{TTLCache, ServerException}
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import fi.vm.sade.hakuperusteet.util.HttpUtil._

case class ApplicationObject(hakukohdeOid: String, hakuOid: String, name: Nimi2, providerName: Nimi2, baseEducations: List[String], description: Nimi2, hakuaikaId: String, status: String)
case class ApplicationSystem(hakuOid: String, formUrl: Option[String], maksumuuriKaytossa: Boolean, hakuaikas: List[HakuAika])
case class HakuAika(hakuaikaId: String, alkuPvm: Long, loppuPvm: Long)
case class EnrichedApplicationObject(hakukohdeOid: String, hakuOid: String, name: Nimi2, providerName: Nimi2, baseEducations: List[String], description: Nimi2, julkaistu: Boolean, maksumuuriKaytossa: Boolean, startDate: Date, endDate: Date)

object EnrichedApplicationObject {
  def apply(ao: ApplicationObject, as: ApplicationSystem): EnrichedApplicationObject = {
    val currentHakuAika = as.hakuaikas.find( (p) => p.hakuaikaId == ao.hakuaikaId).getOrElse(throw ServerException(s"missing hakuaika ${ao.hakuaikaId} in haku ${ao.hakuOid}"))
    EnrichedApplicationObject(ao.hakukohdeOid, ao.hakuOid, ao.name, ao.providerName, ao.baseEducations, ao.description, ao.status == "JULKAISTU",
      as.maksumuuriKaytossa, new Date(currentHakuAika.alkuPvm), new Date(currentHakuAika.loppuPvm))
  }
}

case class Tarjonta(tarjontaBaseUrl: String) {
  implicit val formats = Serialization.formats(NoTypeHints)

  val cache = TTLCache[Oid, List[String]](300, 100)

  def getApplicationObject(hakukohdeOid: String) = hakukohdeToApplicationObject(read[Result](urlToString(tarjontaBaseUrl + "hakukohde/" + hakukohdeOid)).result)

  def getApplicationSystem(hakuOid: String) = hakuToApplicationSystem(read[Result2](urlToString(tarjontaBaseUrl + "haku/" + hakuOid)).result)

  def getApplicationOptionsForOrganization(organizationOids: List[Oid]): List[String] = {
    if (organizationOids.contains(Constants.OphOrganizationOid)) {
      // As OPH is parent of everything it must be handled as a special case in higher level
      throw new AssertionError("Method must not be called for root organization")
    } else {
      organizationOids.flatMap(organizationOid => {
        organizationOid match {
          case oid: OrganizationOid => cache.getOrElseUpdate(oid, () => search(tarjontaBaseUrl + "hakukohde/search?organisationOid=" + oid))
          case oid: OrganizationGroupOid => cache.getOrElseUpdate(oid, () => search(tarjontaBaseUrl + "hakukohde/search?organisaatioRyhmaOid=" + oid))
        }
      })
    }
  }

  def enrichHakukohdeWithHaku(ao: ApplicationObject) = EnrichedApplicationObject(ao, hakuToApplicationSystem(read[Result2](urlToString(tarjontaBaseUrl + "haku/" + ao.hakuOid)).result))

  private def tarjontaUrisToKoodis(tarjontaUri: List[String]) = tarjontaUri.map(_.split("_")(1))

  private def hakukohdeToApplicationObject(r: Hakukohde) = ApplicationObject(r.oid, r.hakuOid, Nimi2(r.hakukohteenNimet), r.tarjoajaNimet, tarjontaUrisToKoodis(r.hakukelpoisuusvaatimusUris), Nimi2(r.lisatiedot), r.hakuaikaId, r.tila)
  private def hakuToApplicationSystem(r: Haku) = ApplicationSystem(r.oid, r.hakulomakeUri, r.maksumuuriKaytossa, r.hakuaikas)
  private def search(url: String): List[String] = organizationSearchToHakukohdeOids(read[Search](urlToString(url)).result)
  private def organizationSearchToHakukohdeOids(result: OrganizationResults): List[String] = result.tulokset.flatMap(r => r.tulokset.map(t => t.oid))
}

object Tarjonta {
  def init(p: Config) = Tarjonta(p.getString("tarjonta.api.url"))
}

case class HakukohdeResult(oid: String)
case class OrganizationResult(oid: String, tulokset: List[HakukohdeResult])
case class OrganizationResults(tulokset: List[OrganizationResult])
case class Search(result: OrganizationResults)

private case class Result(result: Hakukohde)
private case class Nimi(kieli_en: Option[String], kieli_fi: Option[String], kieli_sv: Option[String])
case class Nimi2(en: Option[String], fi: Option[String], sv: Option[String]) {
  def get(lang: String): String = lang match {
    case "fi" => fi.orElse(sv).orElse(en).get
    case "sv" => sv.orElse(fi).orElse(en).get
    case "en" => en.orElse(fi).orElse(sv).get
  }
}
private case class Hakukohde(oid: String, tarjoajaNimet: Nimi2, hakukohteenNimet: Nimi, hakukelpoisuusvaatimusUris: List[String], lisatiedot: Nimi, hakuOid: String, hakuaikaId: String, tila: String)

private object Nimi2 {
  def apply(nimi: Nimi): Nimi2 = {
    Nimi2(nimi.kieli_en, nimi.kieli_fi, nimi.kieli_sv)
  }
}

private case class Result2(result: Haku)
private case class Haku(oid: String, hakulomakeUri: Option[String], maksumuuriKaytossa: Boolean, hakuaikas: List[HakuAika])
