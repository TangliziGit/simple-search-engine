package me.tanglizi.se.engine

import java.io.File
import java.util.concurrent.atomic.AtomicLong

import akka.pattern._
import akka.actor.{ActorRef, ActorSystem, Props}
import me.tanglizi.se.engine.actor.{EngineActor, IndexActor, StorageActor, TokenizeActor}
import me.tanglizi.se.engine.config.Config
import me.tanglizi.se.entity.Protocol.{FlushIndexRequest, FlushInvertedIndexRequest, FlushMetaRequest, LoadIndexRequest, LoadMetaRequest}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}

object Engine {

  val totalDocumentCount: AtomicLong = new AtomicLong(0)
  val totalWordCount: AtomicLong = new AtomicLong(0)
  val wordCountInDocument = mutable.Map[Long, Long]()

  val actorSystem: ActorSystem = ActorSystem.create("searchEngineActorSystem")

  val engineActor:    ActorRef = actorSystem.actorOf(Props[EngineActor], name = "EngineActor")
  val tokenizeActor:  ActorRef = actorSystem.actorOf(Props[TokenizeActor], name = "TokenizeActor")
  val indexActor:     ActorRef = actorSystem.actorOf(Props[IndexActor], name = "IndexActor")
  val storageActor:   ActorRef = actorSystem.actorOf(Props[StorageActor], name = "StorageActor")

  // indexTable: documentId -> (hashCode, fileOffset)
  val indexTable = mutable.Map[Long, (Int, Long)]()
  val invertedIndexTable = mutable.Map[String, mutable.Map[Long, ArrayBuffer[Int]]]()

  def getDocumentId: Long = totalDocumentCount.getAndIncrement()

  def loadData(): Unit = {
    implicit val timeout = Config.DEFAULT_AKKA_TIMEOUT
    val result1 = Engine.storageActor ? LoadMetaRequest
    val result2 = Engine.storageActor ? LoadIndexRequest

    Await.ready(result1, Config.DEFAULT_AWAIT_TIMEOUT)
    Await.ready(result2, Config.DEFAULT_AWAIT_TIMEOUT)
  }

  def flushData(): Unit = {
    implicit val timeout = Config.DEFAULT_AKKA_TIMEOUT

    val result1 = Engine.storageActor ? FlushInvertedIndexRequest
    val result2 = Engine.storageActor ? FlushIndexRequest
    val result3 = Engine.storageActor ? FlushMetaRequest

    println("flushing inverted index table")
    Await.ready(result1, Config.DEFAULT_AWAIT_TIMEOUT)
    println("flushing index table")
    Await.ready(result2, Config.DEFAULT_AWAIT_TIMEOUT)
    println("flushing meta table")
    Await.ready(result3, Config.DEFAULT_AWAIT_TIMEOUT)
  }

  def asyncFlushData(): Unit = {
    Engine.storageActor ! FlushInvertedIndexRequest
    Engine.storageActor ! FlushIndexRequest
    Engine.storageActor ! FlushMetaRequest
  }

  def eraseData(): Unit = {
    val dir = new File(Config.STORAGE_PATH)

    for (file <- dir.listFiles())
      file.delete()
  }
}
