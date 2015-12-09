package fi.vm.sade

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.util.{Date, TimeZone}

import fi.vm.sade.hakuperusteet.domain.IDPEntityId
import fi.vm.sade.hakuperusteet.domain.PaymentStatus.PaymentStatus
import fi.vm.sade.hakuperusteet.domain._
import org.json4s.JsonAST.{JInt, JNull, JString}
import org.json4s.native.Serialization
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
      case JString(s) => Date.from(LocalDate.parse(s, UIDateFormatter).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
      case JNull => null
    },
    {
      case d: Date => JString(UIDateFormatter.format(
        LocalDateTime.ofInstant(Instant.ofEpochMilli(d.getTime()), ZoneId.systemDefault())))
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
  } + UiDateSerializer + PaymentStatusSerializer + IDPEntityIdSerializer

  case object PaymentStatusSerializer extends CustomSerializer[PaymentStatus](format => (
    { case JString(s) => PaymentStatus.withName(s) },
    { case x: PaymentStatus => JString(x.toString) })
  )

  implicit val formats = org.json4s.DefaultFormats + PaymentStatusSerializer + IDPEntityIdSerializer
  val UIDateFormatter = DateTimeFormatter.ofPattern("ddMMyyyy")
  val personIdDateFormatter = DateTimeFormatter.ofPattern("ddMMyy")

}
