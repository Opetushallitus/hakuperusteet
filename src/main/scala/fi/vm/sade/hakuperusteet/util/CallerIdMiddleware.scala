package fi.vm.sade.hakuperusteet.util

import org.http4s.{Header, Request, Service}
import org.http4s.client.{Client, DisposableResponse}
import scalaz.concurrent.Task

object CallerIdMiddleware {
  val id = "1.2.246.562.10.00000000001.hakuperusteet"

  def apply(client: Client): Client = {

    def setCallersIds(req: Request): Task[DisposableResponse] = {
      client.open(
        req.copy(headers = req.headers ++ List(
          Header("clientSubSystemCode", id),
          Header("Caller-Id", id),
          Header("CSRF", id),
          Header("Cookie", "CSRF="+id)
        )))
    }

    client.copy(open = Service.lift(setCallersIds))
  }
}
