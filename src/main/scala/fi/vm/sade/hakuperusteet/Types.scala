package fi.vm.sade.hakuperusteet

sealed trait Oid {
  val value: String
  override def toString = value
}

object Oid {
  private val oidRegex = """^([0-9]+(\.[0-9]+)+)$""".r

  def isOid(oid: String): Boolean = oid match {
    case oidRegex(_*) => true
    case _ => false
  }

  def apply(value: String): Oid = {
    if (!isOid(value)) {
      throw new IllegalArgumentException(s"OID ($value) format was invalid")
    }

    if (value.startsWith("1.2.246.562.10.")) {
      OrganizationOid(value)
    } else if (value.startsWith("1.2.246.562.28.")) {
      OrganizationGroupOid(value)
    } else {
      throw new IllegalArgumentException(s"Unrecognized OID $value")
    }
  }
}

sealed case class OrganizationOid(value: String) extends Oid

sealed case class OrganizationGroupOid(value: String) extends Oid
