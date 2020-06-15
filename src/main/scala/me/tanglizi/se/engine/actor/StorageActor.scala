package me.tanglizi.se.engine.actor

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.entity.Protocol._

class StorageActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case StoreContentRequest(hash, content) =>
      // TODO
      sender ! 0L

    case FlushIndexRequest =>

    case FlushInvertedIndexRequest =>

    case FindInvertedIndexItemRequest(word) =>

  }
}
