import DrawerActor.{GetValue, KeptValue}
import RoutingActor._
import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol
import akka.pattern.ask

case class Drawer(number: Int, value: String)

trait ServiceJsonProtoocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val customerProtocol = jsonFormat2(Drawer)
}

object DrawerActor {
  case object GetValue
  case class KeptValue(value : String)
}

class DrawerActor(value : String) extends Actor {
  override def receive: Receive = {
    case GetValue => sender() ! KeptValue(value)
  }
}

object RoutingActor {
  case class GetValueFromDrawer(num: Int)
  case class PutValueInDrawer(num: Int, value: String)

  case object NoDrawer
  case object DrawerAlreadyExists
  case class ValueFromDrawer(value: String)
}

class RoutingActor extends Actor {
  override def receive: Receive = {
    case GetValueFromDrawer(num) =>
      val routeSender = sender()
      def noDrawer = sender() ! NoDrawer
      context.child(s"Actor$num").fold(noDrawer)(drawerActor => (drawerActor ? GetValue).onSuccess({
        case KeptValue(v) => sender() ! ValueFromDrawer(v)
      }))
    case PutValueInDrawer(num, value) =>
      def createDrawer : Unit = {
        context.actorOf(Props[DrawerActor], s"Actor$num")
      }
      context.child(s"Actor$num").fold(createDrawer)(drawerActor => drawerActor ! DrawerAlreadyExists)
  }
}

object Main extends App with ServiceJsonProtoocol {
  implicit val system = ActorSystem("ApothecaryTableActorSystem")
  implicit val materializer = ActorMaterializer()

  val routingActor = system.actorOf(Props[RoutingActor])

  // PUT apothecary {draw: num, value: "value"}
  // GET apothecary/NUM

  val route =
    path("apothecary" / IntNumber) { drawerNum => {
        get {
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"You send number [$drawerNum]"))
        }
      }
    } ~
    path("apothecary") {
      put {
        entity(as[Drawer]){
          case Drawer(number, value) => {
            (routingActor ? PutValueInDrawer(number, value)).onSuccess({
              case
            })
            complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"You send number [$number, $value]"))
          }
        }
      }
    }
  //      get {
  //        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
  //      } ~
  //      put {
  //        entity(as[Drawer]){
  //          case Drawer(str) => complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"You send value [$str]"))
  //        }
  //      }
  //    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
}
