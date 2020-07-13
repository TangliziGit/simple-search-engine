package me.tanglizi.se.engine.actor

import scala.concurrent.duration._
import akka.pattern._
import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Protocol._

import scala.concurrent.Await

class EngineActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case AddRequest(response) =>
      val documentId = Engine.getDocumentId
      Engine.tokenizeActor ! TokenizeDocumentRequest(documentId, response)

    case SearchRequest(sentence, cb) =>
      implicit val timeout: Timeout = Timeout(120.seconds)
      val words: Array[String] = {
        val wordsFuture = (Engine.tokenizeActor ? TokenizeSearchWordRequest(sentence)).mapTo[Array[String]]
        Await.result(wordsFuture, 120.seconds)  // TODO
      }

      Engine.indexActor ! IndexSearchRequest(words, cb)
  }

}
