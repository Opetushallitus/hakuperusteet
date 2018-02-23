package fi.vm.sade.hakuperusteet.util

import org.json4s._
import org.json4s.jackson.JsonMethods._

object Translate {
  val map = loadMap

  def navigate(map: Map[String, Any], keys: Seq[String]): Any = {
    if(keys.size == 1) {
      map(keys.head)
    } else {
      navigate(map(keys.head).asInstanceOf[Map[String, Any]], keys.tail)
    }
  }

  def get(keys: String*) = navigate(map, keys.flatMap((k) => k.split("\\.")))

  def getMap(keys: String*) = navigate(map, keys.flatMap((k) => k.split("\\."))).asInstanceOf[Map[String, Any]]

  def apply(keys: String*) = navigate(map, keys.flatMap((k) => k.split("\\."))).toString

  def loadMap: Map[String, Any] = {
    implicit val formats = org.json4s.DefaultFormats
    val file = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/web-translations.json")).mkString
    parse(file).extract[Map[String, Any]]
  }
}
