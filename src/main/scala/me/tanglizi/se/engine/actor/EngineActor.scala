package me.tanglizi.se.engine.actor

import akka.pattern._
import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.config.Config
import me.tanglizi.se.entity.Document
import me.tanglizi.se.entity.Protocol._

import scala.concurrent.{Await, Future}

class EngineActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case AddRequest(response) =>
      def addDocument(): Unit = {
        val documentId: Long = Engine.getDocumentId
        Engine.documentUrlToId(response.getUri.toString) = documentId
        Engine.documentIdToUrl(documentId) = response.getUri.toString
        Engine.tokenizeActor ! TokenizeDocumentRequest(documentId, response)
      }

      if (Engine.documentUrlToId.contains(response.getUri.toString)) {
        val documentId: Long = Engine.documentUrlToId(response.getUri.toString)
        if (Engine.deletedDocumentIds.contains(documentId))
          addDocument()
      } else
        addDocument()

    case SearchRequest(sentence) =>
      implicit val timeout: Timeout = Config.DEFAULT_AKKA_TIMEOUT

      // get words tokenized from searching sentence
      val words: Array[String] = {
        val wordsFuture: Future[Array[String]] = (Engine.tokenizeActor ? TokenizeSearchWordRequest(sentence)).mapTo[Array[String]]
        Await.result(wordsFuture, Config.DEFAULT_AWAIT_TIMEOUT)
      }

      val documentsFuture: Future[List[Document]] =
        (Engine.indexActor ? IndexSearchRequest(words, xs => ())).mapTo[List[Document]]
      val documents: List[Document] = Await.result(documentsFuture, Config.DEFAULT_AWAIT_TIMEOUT)
      documents.filter(doc => !Engine.deletedDocumentIds.contains(doc.documentId))
      sender ! documents

    case AsyncSearchRequest(sentence, cb) =>
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

      if (Engine.deletedDocumentIds.size >= Config.MAX_DELETED_DOCUMENTS_SIZE)
        Engine.storageActor ! RearrangeTablesRequest
  }

}
