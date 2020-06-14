package me.tanglizi.se.engine.actor

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.util.Protocol._

class IndexActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case IndexRequest(id, content, words) =>

    case IndexSearchRequest(words, cb) =>

  }
}
