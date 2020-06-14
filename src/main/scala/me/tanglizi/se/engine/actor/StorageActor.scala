package me.tanglizi.se.engine.actor

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.util.Protocol._

class StorageActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case StoreContentRequest(hash, content) =>

    case FlushIndexRequest =>

    case FlushInvertedIndexRequest =>

    case FindInvertedIndexItemRequest(word) =>

  }
}
