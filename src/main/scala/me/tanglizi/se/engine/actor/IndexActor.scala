package me.tanglizi.se.engine.actor

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorLogging}
import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.InvertedItem
import me.tanglizi.se.entity.Protocol._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.hashing.MurmurHash3

class IndexActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case IndexRequest(id, content, tokens) =>
      implicit val timeout: Timeout = Timeout(120.seconds)
      val documentHash: Int = MurmurHash3.stringHash(content)
      val offsetFuture: Future[Any] = Engine.storageActor ? StoreContentRequest(documentHash, content)

      offsetFuture onComplete {
        case Success(offset: Long)    => Engine.indexTable(id) = offset
        case Failure(exception) => exception.printStackTrace()
        case _ => log.error("offset future miss match")
      }

      for (token <- tokens) {
        val item: InvertedItem = Engine.invertedIndexTable(token.keyword)
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
