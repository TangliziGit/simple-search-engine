package me.tanglizi.se.engine

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ActorRef, ActorSystem, Props}
import me.tanglizi.se.engine.actor.{EngineActor, IndexActor, StorageActor, TokenizeActor}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Engine {

}

object Engine {
  // TODO: implement below functions
  def getDocumentId: Long = 123

  val totalDocumentCount: AtomicLong = new AtomicLong(0)
  val totalWordCount: AtomicLong = new AtomicLong(0)
  val wordCountInDocument = mutable.Map[Long, Long]()

  val actorSystem: ActorSystem = ActorSystem.create("searchEngineActorSystem")

  val engineActor:    ActorRef = actorSystem.actorOf(Props[EngineActor], name = "EngineActor")
  val tokenizeActor:  ActorRef = actorSystem.actorOf(Props[TokenizeActor], name = "TokenizeActor")
  val indexActor:     ActorRef = actorSystem.actorOf(Props[IndexActor], name = "IndexActor")
  val storageActor:   ActorRef = actorSystem.actorOf(Props[StorageActor], name = "StorageActor")

  val indexTable = mutable.Map[Long, Long]()
  val invertedIndexTable = mutable.Map[String, mutable.Map[Long, ArrayBuffer[Int]]]()

}
