package me.tanglizi.se.engine.actor

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
      Engine.documentUrlToId(response.getUri.toString) = documentId
      Engine.documentIdToUrl(documentId) = response.getUri.toString
      Engine.tokenizeActor ! TokenizeDocumentRequest(documentId, response)

    case SearchRequest(sentence, cb) =>
      implicit val timeout: Timeout = Config.DEFAULT_AKKA_TIMEOUT

      // get words tokenized from searching sentence
      val words: Array[String] = {
        val wordsFuture: Future[Array[String]] = (Engine.tokenizeActor ? TokenizeSearchWordRequest(sentence)).mapTo[Array[String]]
        Await.result(wordsFuture, Config.DEFAULT_AWAIT_TIMEOUT)
      }

      // start searching, and use callback function to process documents
      Engine.indexActor ! IndexSearchRequest(words, cb)

    case DeleteRequest(url) =>
      val documentId: Long = Engine.documentUrlToId(url)
      Engine.deletedDocumentIds.add(documentId)

      if (Engine.deletedDocumentIds.size >= Config.DELETED_DOCUMENTS_SIZE)
        Engine.storageActor ! RearrangeTablesRequest
  }

}
