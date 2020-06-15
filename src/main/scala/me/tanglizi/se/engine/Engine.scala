package me.tanglizi.se.engine

import akka.actor.{ActorRef, ActorSystem, Props}
import io.github.yizhiru.thulac4j.Segmenter
import me.tanglizi.se.engine.actor.{EngineActor, IndexActor, StorageActor, TokenizeActor}
import me.tanglizi.se.entity.InvertedItem

import scala.collection.mutable

class Engine {

}

object Engine {
  def getDocumentId: Long = 123

  val actorSystem: ActorSystem = ActorSystem.create("searchEngineActorSystem")

  val engineActor:    ActorRef = actorSystem.actorOf(Props[EngineActor], name = "EngineActor")
  val tokenizeActor:  ActorRef = actorSystem.actorOf(Props[TokenizeActor], name = "TokenizeActor")
  val indexActor:     ActorRef = actorSystem.actorOf(Props[IndexActor], name = "IndexActor")
  val storageActor:   ActorRef = actorSystem.actorOf(Props[StorageActor], name = "StorageActor")

  val indexTable = mutable.Map[Long, Long]()
  val invertedIndexTable = mutable.Map[String, InvertedItem]()

}
