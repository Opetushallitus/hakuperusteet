package fi.vm.sade.hakuperusteet.util

import com.typesafe.scalalogging.LazyLogging
import org.http4s.headers.`Content-Type`
import org.http4s._
import org.json4s.Formats
import org.json4s.jackson.Serialization._

import scala.util.control.NoStackTrace
import scalaz.\/._
import scalaz.concurrent.{Future, Task}

trait CasClientUtils extends LazyLogging {
  def urlToUri(url: String) = new Task(Future.now(Uri.fromString(url).leftMap((fail: ParseFailure) => new IllegalArgumentException(fail.sanitized)))).run

  def parseJson4s[A](json: String)(implicit formats: Formats, mf: Manifest[A]) = scala.util.Try(read[A](json)).map(right).recover {
    case t =>
      logger.error("json decoding failed " + json, t)
      left(ParseFailure("json decoding failed", t.getMessage))
  }.get

  def json4sEncoderOf[A <: AnyRef](implicit formats: Formats, mf: Manifest[A]): EntityEncoder[A] = EntityEncoder.stringEncoder(Charset.`UTF-8`).contramap[A](item => write[A](item))
    .withContentType(`Content-Type`(MediaType.`application/json`))

  def json4sOf[A](implicit formats: Formats, mf: Manifest[A]): EntityDecoder[A] = EntityDecoder.decodeBy[A](MediaType.`application/json`) {
    case r @ Response(status, _, _, _, _) if status.code == 200 => DecodeResult(EntityDecoder.decodeString(r)(Charset.`UTF-8`).map(parseJson4s[A]))
    case r @ Response(status, _, _, _, _) if status.code == 409 => throw new ConflictException(fetchBody(r))
    case r @ Response(status, _, _, _, _) =>
      val body = fetchBody(r)
      logger.error(s"CAS-client external request failed with status ${status.code}, body $body")
      throw new ServerException(status.code.toString)
  }

  private def fetchBody[A](r: Response) = EntityDecoder.decodeString(r).attemptRun.getOrElse("")
}

case class ServerException(msg: String) extends Exception(msg) with NoStackTrace
case class ConflictException(msg: String) extends Exception(msg) with NoStackTrace
