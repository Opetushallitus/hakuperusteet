package fi.vm.sade

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.util.{Date, TimeZone}

import fi.vm.sade.hakuperusteet.domain.IDPEntityId
import fi.vm.sade.hakuperusteet.domain._
import org.json4s.JsonAST.{JInt, JNull, JString}
import org.json4s.jackson.Serialization
import org.json4s.{CustomSerializer, DefaultFormats}

package object hakuperusteet {
  type Oid = String

  case object DateSerializer extends CustomSerializer[Date](format => (
    {
      case JString(s) => new Date(s.toLong)
      case JInt(s) => new Date(s.toLong)
      case JNull => null
    },
    {
      case d: Date => JInt(d.getTime)
    }
    )
  )

  case object UiDateSerializer extends CustomSerializer[Date](format => (
    {
      case JString(s) => Date.from(LocalDate.parse(s, UIDateFormatter).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant)
      case JNull => null
    },
    {
      case d: Date => JString(UIDateFormatter.format(
        LocalDateTime.ofInstant(Instant.ofEpochMilli(d.getTime), ZoneId.systemDefault())))
    }
    )
  )

  case object IDPEntityIdSerializer extends CustomSerializer[IDPEntityId](format => (
    { case JString(name) => IDPEntityId.withName(name) },
    { case idpEntityId: IDPEntityId => JString(idpEntityId.toString) })
  )

  val formatsHenkilo = Serialization.formats(org.json4s.NoTypeHints) + DateSerializer + IDPEntityIdSerializer

  val formatsUI = new DefaultFormats {
    override def dateFormatter = {
      val df = new SimpleDateFormat("ddMMyyyy")
      df.setTimeZone(TimeZone.getDefault)
      df
    }
  } + UiDateSerializer + IDPEntityIdSerializer + EnumerationSerializer

  //There cannot be separate serializers for different enumerations due to this bug: https://github.com/json4s/json4s/issues/142
  case object EnumerationSerializer extends CustomSerializer[Enumeration#Value](format => (
    {case JString(s) =>
      if(s == Hakumaksukausi.s2016.toString || s == Hakumaksukausi.k2017.toString) Hakumaksukausi.withName(s)
      else PaymentStatus.withName(s)
    },
    {case x: Enumeration#Value => JString(x.toString)
    })
  )

  implicit val formats = org.json4s.DefaultFormats + EnumerationSerializer + IDPEntityIdSerializer
  val UIDateFormatter = DateTimeFormatter.ofPattern("ddMMyyyy")
  val personIdDateFormatter = DateTimeFormatter.ofPattern("ddMMyy")

}
