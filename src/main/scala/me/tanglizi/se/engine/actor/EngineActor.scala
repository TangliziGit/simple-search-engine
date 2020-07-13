package me.tanglizi.se.engine.actor

import scala.concurrent.duration._
import akka.pattern._
import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.engine.config.Config
import me.tanglizi.se.entity.Protocol._

import scala.concurrent.{Await, Future}

class EngineActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case AddRequest(response) =>
      val documentId: Long = Engine.getDocumentId
      Engine.tokenizeActor ! TokenizeDocumentRequest(documentId, response)

    case SearchRequest(sentence, cb) =>
      implicit val timeout: Timeout = Config.DEFAULT_AKKA_TIMEOUT
      val words: Array[String] = {
        val wordsFuture: Future[Array[String]] = (Engine.tokenizeActor ? TokenizeSearchWordRequest(sentence)).mapTo[Array[String]]
        Await.result(wordsFuture, Config.DEFAULT_AWAIT_TIMEOUT)
      }

      Engine.indexActor ! IndexSearchRequest(words, cb)
  }

}
