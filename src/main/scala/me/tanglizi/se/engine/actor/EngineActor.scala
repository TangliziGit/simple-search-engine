package me.tanglizi.se.engine.actor

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Protocol._

class EngineActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case AddRequest(response) =>
      val documentId = Engine.getDocumentId
      Engine.tokenizeActor ! TokenizeDocumentRequest(documentId, response)

    case SearchRequest(word, cb) =>

  }

}
