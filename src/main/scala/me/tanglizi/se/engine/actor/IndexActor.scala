package me.tanglizi.se.engine.actor

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging}
import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.config.Config
import me.tanglizi.se.entity.{Document, DocumentInfo}
import me.tanglizi.se.entity.Protocol._
import me.tanglizi.se.util.HashUtil

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.util.control.Breaks
import scala.util.{Failure, Success}

class IndexActor extends Actor with ActorLogging {

  import context.dispatcher

  def createDescription(document: Document, keywordPositionsMaps: List[mutable.Map[Long, ArrayBuffer[Int]]]): String = {
    val content: String = document.documentInfo.content
    val documentId: Long = document.documentId

    val positions: Array[Int] = {
      val positions = mutable.ArrayBuffer[Int]()
      for (keywordPositionMap <- keywordPositionsMaps)
        for (poss <- keywordPositionMap(documentId))
          positions += poss
      positions.sorted.toArray
    }

    val interval: Int = Config.KEYWORD_INTERVAL_IN_DESCRIPTION
    var Array(prev, pos) = Array(0, 0)
    val builder: StringBuilder = {
      if (positions(0) - interval > 0)
        new mutable.StringBuilder("...")
      else
        new mutable.StringBuilder()
    }

    val loop = new Breaks

    loop.breakable {
      for (cur <- positions) {
        val Array(cPrev, cPos) = Array(cur - interval, cur + interval)

        if (cPrev <= pos) {
          pos = cPos
        } else {
          builder.append(content.slice(prev, pos))
          builder.append("...")
          prev = cPrev
          pos  = cPos
        }

        if (builder.length() >= Config.MAX_DESCRIPTION_LENGTH)
          loop.break()
      }

      builder.append(content.slice(prev, pos))
      builder.append("...")
    }

    builder.toString.slice(0, Config.MAX_DESCRIPTION_LENGTH)
  }

  override def receive: Receive = {
    case IndexRequest(id, documentInfo, tokens) =>
      log.info(s"received IndexRequest ${IndexRequest(id, documentInfo, tokens)}")
      implicit val timeout: Timeout = Config.DEFAULT_AKKA_TIMEOUT

      // get hash code from document content
      // used to determine the file name this document locates
      val documentHash: Int = HashUtil.hashMurmur3(documentInfo.content)
      val offsetFuture: Future[Long] =
        (Engine.storageActor ? StoreDocumentRequest(documentHash, documentInfo)).mapTo[Long]

      // get document position of index table, named `offset`
      offsetFuture onComplete {
        case Success(offset) =>
          Engine.indexTable(id) = (documentHash, offset)
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

      // maintain total count
      val wordCount: Int = tokens.map(_.position.length).sum
      Engine.wordCountInDocument.getOrElseUpdate(id, wordCount)
      // Engine.totalDocumentCount.incrementAndGet()
      Engine.totalWordCount.addAndGet(wordCount)

      // conditional flush
      if (Engine.invertedIndexTable.size > Config.INVERTED_INDEX_TABLE_FLUSH_SIZE)
        Engine.storageActor ! FlushInvertedIndexRequest
      if (Engine.indexTable.size % Config.INDEX_TABLE_FLUSH_FREQ == 0)
        Engine.storageActor ! FlushIndexRequest
      if (Engine.totalDocumentCount.get() % Config.META_TABLE_FLUSH_FREQ == 0)
        Engine.storageActor ! FlushMetaRequest

    case IndexSearchRequest(words, cb, isDescribed) =>
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
        .sortBy(document => -document.BM25)

      // add document information (title, url and content) by document id
      val documentInfoFutures: List[Future[DocumentInfo]] = documents.map(document =>
        (Engine.storageActor ? FindDocumentRequest(document.documentId)).mapTo[DocumentInfo]
      )
      val documentInfoFutureList: Future[List[DocumentInfo]] = Future.sequence(documentInfoFutures)
      val documentInfoList: List[DocumentInfo] = Await.result(documentInfoFutureList, Config.DEFAULT_AWAIT_TIMEOUT)

      documents.zip(documentInfoList).foreach{
        case (document, documentInfo) =>
          document.setInformation(documentInfo)
      }

      if (isDescribed) {
        documents.foreach(doc => {
          val docInfo: DocumentInfo = doc.documentInfo
          val description: String = createDescription(doc, keywordPositionsMaps)
          doc.setInformation(docInfo.title, docInfo.url, description)
        })
      }
      cb(documents)
      sender ! documents
  }
}
