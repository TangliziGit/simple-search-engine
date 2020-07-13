package me.tanglizi.se.engine.actor

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging}
import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.engine.config.Config
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
      implicit val timeout: Timeout = Config.DEFAULT_AKKA_TIMEOUT

      // get hash code from document content
      // used to determine the file name this document locates
      val documentHash: Int = HashUtil.hash(content)
      val offsetFuture: Future[Long] = ask(Engine.storageActor, StoreContentRequest(documentHash, content)).mapTo[Long]

      // get document position of index table, named `offset`
      offsetFuture onComplete {
        case Success(offset) =>
          Engine.indexTable(id) = offset
          log.info(s"indexTable $id -> $offset")
        case Failure(exception) =>
          exception.printStackTrace()
      }

      // update inverted index table ( word -> documentID, positionsInTheDoc )
      for (token <- tokens) {
        val item: mutable.Map[Long, ArrayBuffer[Int]] = Engine.invertedIndexTable
          .getOrElseUpdate(token.keyword, mutable.Map[Long, ArrayBuffer[Int]]())
        val arr: ArrayBuffer[Int] = item.getOrElseUpdate(id, ArrayBuffer[Int]())
        arr ++= token.position
      }

      if (Engine.invertedIndexTable.size > Config.INVERTED_INDEX_TABLE_FLUSH_SIZE)
        Engine.storageActor ! FlushInvertedIndexRequest
      if (Engine.indexTable.size % Config.INDEX_TABLE_FLUSH_FREQ == 0)
        Engine.storageActor ! FlushIndexRequest

    case IndexSearchRequest(words, cb) =>
      implicit val timeout: Timeout = Config.DEFAULT_AKKA_TIMEOUT

      // get inverted index item of each keyword
      val futures: Future[List[mutable.Map[Long, ArrayBuffer[Int]]]] = {
        val futureList = words.map( word =>
            (Engine.storageActor ? FindInvertedIndexItemRequest(word))
              .mapTo[mutable.Map[Long, ArrayBuffer[Int]]]
          ).toList

        Future.sequence(futureList)
      }

      // for each word, map document id to word position list
      val keywordPositionsMaps: List[mutable.Map[Long, ArrayBuffer[Int]]] =
        Await.result(futures, Config.DEFAULT_AWAIT_TIMEOUT)

      // build Document list, and sort it
      val documents: List[Document] = Document
        .fromDs(keywordPositionsMaps, words)
        .sortBy(document => document.BM25)

      // add document information (title, url and content) by document id
      documents.foreach(document => {
        // TODO: find document information
        document.setInformation("title", "url", "content")
      })

      cb(documents)
  }
}
