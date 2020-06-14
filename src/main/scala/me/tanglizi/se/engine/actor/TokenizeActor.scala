package me.tanglizi.se.engine.actor

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.util.Protocol._

class TokenizeActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case TokenizeDocumentRequest(id, html) =>

    case TokenizeSearchWordRequest(word) =>

  }
}
