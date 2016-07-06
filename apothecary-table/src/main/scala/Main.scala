import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol

case class TempValue(value: String)

trait ServiceJsonProtoocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val customerProtocol = jsonFormat1(TempValue)
}

object Main extends App with ServiceJsonProtoocol {
  implicit val system = ActorSystem("ApothecaryTableActorSystem")
  implicit val materializer = ActorMaterializer()

  val route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      } ~
      post {
        entity(as[TempValue]){
          case TempValue(str) => complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"You send value [$str]"))
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
}
