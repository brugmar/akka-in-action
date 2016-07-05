import PrintActor.Print
import akka.actor.{Actor, ActorSystem, Props}

object PrintActor {
  case class Print(message: String)
}

class PrintActor extends Actor {
  override def receive: Receive = {
    case Print(msg) => println(msg)
  }
}

object Main extends App {
  val system = ActorSystem("PrintIt")
  val printActor = system.actorOf(Props[PrintActor])
  printActor ! Print("Hello World Akka!")
}
