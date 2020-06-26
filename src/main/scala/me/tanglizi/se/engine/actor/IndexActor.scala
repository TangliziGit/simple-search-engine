package me.tanglizi.se.engine.actor

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging}
import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.InvertedItem
import me.tanglizi.se.entity.Protocol._
import me.tanglizi.se.util.HashUtil

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.util.hashing.MurmurHash3

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
        val item: InvertedItem = Engine.invertedIndexTable
          .getOrElseUpdate(token.keyword, InvertedItem(ArrayBuffer(), ArrayBuffer()))
        item.indices += ((id, item.positions.length))
        item.positions += token.position
      }

      // TODO: set a flush size constant
      if (Engine.invertedIndexTable.size > 50)
        Engine.storageActor ! FlushInvertedIndexRequest
      if (Engine.indexTable.size % 50 == 0)
        Engine.storageActor ! FlushIndexRequest

    case IndexSearchRequest(words, cb) =>

  }
}
