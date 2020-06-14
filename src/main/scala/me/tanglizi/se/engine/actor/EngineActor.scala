package me.tanglizi.se.engine.actor

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.util.Protocol._

class EngineActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case AddRequest(response) =>

    case SearchRequest(word, cb) =>

  }

}
