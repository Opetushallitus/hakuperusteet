package fi.vm.sade.hakuperusteet.rsa

import java.io.{FileInputStream, InputStream}
import java.security.{Signature, KeyFactory}
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success, Try}

class RSASigner(config: Config, serviceName: String) extends LazyLogging {
  val key = readPrivateKey()

  def signData(dataString: String) = {
    val signature = Signature.getInstance("SHA256withRSA")
    signature.initSign(key)
    signature.update(dataString.getBytes("UTF-8"))
    Base64.getEncoder.encodeToString(signature.sign())
  }

  private def sourceToBytes(is: InputStream) =
    Stream
      .continually(is.read)
      .takeWhile(_ != -1)
      .map(_.toByte)
      .toArray

  private def bytesFromUrl(url: String): Array[Byte] = {
    Try(sourceToBytes(new FileInputStream(url))) match {
      case Success(v) => v
      case Failure(e) =>
        logger.error(s"Failed to read RSA key from $url")
        Try(sourceToBytes(getClass.getResourceAsStream(url))) match {
          case Success(v) => v
          case Failure(e) =>
            logger.error(s"Failed to read RSA key from classpath url $url")
            throw e
        }
    }
  }
  private def readPrivateKey(): RSAPrivateKey = {
    val keyPath = config.getString(s"rsa.sign.key.$serviceName")
    val bs = bytesFromUrl(keyPath)
    KeyFactory.getInstance("RSA")
      .generatePrivate(new PKCS8EncodedKeySpec(bs))
      .asInstanceOf[RSAPrivateKey]
  }
}

object RSASigner {
  def init(config: Config, serviceName: String) = {
    new RSASigner(config, serviceName)
  }
}