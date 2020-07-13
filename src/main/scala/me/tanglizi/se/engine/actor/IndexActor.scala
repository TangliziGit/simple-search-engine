package me.tanglizi.se.engine.actor

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging}
import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Document
import me.tanglizi.se.entity.Protocol._
import me.tanglizi.se.util.HashUtil

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class IndexActor extends Actor with ActorLogging {

  import context.dispatcher

  override def receive: Receive = {
    case IndexRequest(id, content, tokens) =>
      log.info(s"received IndexRequest ${IndexRequest(id, content, tokens)}")

      implicit val timeout: Timeout = Timeout(120.seconds)
      val documentHash: Int = HashUtil.hash(content)
      val offsetFuture: Future[Long] = ask(Engine.storageActor, StoreContentRequest(documentHash, content)).mapTo[Long]

      offsetFuture onComplete {
        case Success(offset) =>
          Engine.indexTable(id) = offset
          log.info(s"indexTable $id -> $offset")
        case Failure(exception) =>
          exception.printStackTrace()
      }

      for (token <- tokens) {
        val item = Engine.invertedIndexTable
          .getOrElseUpdate(token.keyword, mutable.Map[Long, ArrayBuffer[Int]]())
        val arr = item.getOrElseUpdate(id, ArrayBuffer[Int]())
        arr ++= token.position
      }

      // TODO: set a flush size constant
      if (Engine.invertedIndexTable.size > 50)
        Engine.storageActor ! FlushInvertedIndexRequest
      if (Engine.indexTable.size % 50 == 0)
        Engine.storageActor ! FlushIndexRequest

    case IndexSearchRequest(words, cb) =>
      implicit val timeout: Timeout = Timeout(120.seconds)
      val futuresList: List[Future[mutable.Map[Long, ArrayBuffer[Int]]]] = words.map(word =>
        ( Engine.storageActor ? FindInvertedIndexItemRequest(word) )
          .mapTo[mutable.Map[Long, ArrayBuffer[Int]]]
      ).toList
      val futures: Future[List[mutable.Map[Long, ArrayBuffer[Int]]]] = Future.sequence(futuresList)

      // for each word, map document id to word position list
      val keywordPositionsMaps: List[mutable.Map[Long, ArrayBuffer[Int]]] = Await.result(futures, 120.seconds)

      val documents: List[Document] = Document
        .fromDs(keywordPositionsMaps, words)
        .sortBy(document => document.BM25)

      documents.foreach(document => {
        // TODO
        document.setInformation("title", "url", "content")
      })

      cb(documents)
  }
}
