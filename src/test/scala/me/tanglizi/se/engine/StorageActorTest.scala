package me.tanglizi.se.engine

import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.config.Config
import me.tanglizi.se.entity.DocumentInfo
import me.tanglizi.se.entity.Protocol._
import me.tanglizi.se.entity.Result.Token
import me.tanglizi.se.util.HashUtil
import org.junit.Test

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}


@Test
class StorageActorTest {

  @Test
  def testStoreContentRequest(): Unit = {
    implicit val timeout: Timeout = Timeout(5.seconds)
    val content: String = "我爱北京天安门"
    val documentInfo = DocumentInfo("title", "url", content)
    val hash: Long = HashUtil.hashMurmur3(content)

    val future: Future[Long] = (Engine.storageActor ? StoreDocumentRequest(hash, documentInfo)).mapTo[Long]

    future onComplete {
      case Success(value) =>
        println(value)
      case Failure(exception) =>
        exception.printStackTrace()
    }

    Thread.sleep(2000)
  }

  @Test
  def testFindDocumentRequest(): Unit = {
    Engine.indexTable(0) = (HashUtil.hashMurmur3("我爱北京天安门"), 0)

    implicit val timeout: Timeout = Timeout(5.seconds)
    val documentInfoFuture = (Engine.storageActor ? FindDocumentRequest(0)).mapTo[DocumentInfo]
    val documentInfo = Await.result(documentInfoFuture, Config.DEFAULT_AWAIT_TIMEOUT)

    println(documentInfo)
  }

  @Test
  def testFlushIndexRequest(): Unit = {
    implicit val timeout: Timeout = Timeout(5.seconds)
    val content: String = "我爱北京天安门"

    for (i <- Range(1, 10)) {
      val newContent: String = content + i.toString
      val newDocumentInfo = DocumentInfo("title", "url", newContent)
      val hash: Int = HashUtil.hashMurmur3(newContent)

      val future: Future[Long] =
        (Engine.storageActor ? StoreDocumentRequest(hash, newDocumentInfo)).mapTo[Long]

      future onComplete {
        case Success(offset) =>
          Engine.indexTable(i) = (hash, offset)
        case Failure(exception) =>
          exception.printStackTrace()
      }
    }

    Thread.sleep(1000)

    Engine.storageActor ! FlushIndexRequest

    Thread.sleep(1000)
  }

  @Test
  def testLoadIndexRequest(): Unit = {
    Engine.storageActor ! LoadIndexRequest

    Thread.sleep(1000)
    println(Engine.indexTable)
  }

  @Test
  def testFlushInvertedIndexRequest(): Unit = {
    val content: String = "我爱北京天安门"
    val documentInfo = DocumentInfo("title", "url", content)
    val tokens: Array[Token] = Array[Token](
      Token("我", Array(0)),
      Token("爱", Array(1)),
      Token("北京", Array(2)),
      Token("天安门", Array(4)),
    )

    for (i <- Range(0, 20))
      Engine.indexActor ! IndexRequest(i, documentInfo, tokens)
    Thread.sleep(1000)

    println(Engine.invertedIndexTable)
    Engine.storageActor ! FlushInvertedIndexRequest
    Thread.sleep(5000)
  }

  @Test
  def testFindInvertedIndexItemRequest(): Unit = {
    implicit val timeout: Timeout = Timeout(120.seconds)
    val futures = ArrayBuffer[Future[mutable.Map[Long, ArrayBuffer[Int]]]]()
    for (word <- Array("我", "爱", "北京", "天安门")) {
      val future = (Engine.storageActor ? FindInvertedIndexItemRequest(word)).mapTo[mutable.Map[Long, ArrayBuffer[Int]]]
      future onComplete {
        case Success(item) =>
          println(item)
        case Failure(e) =>
          e.printStackTrace()
      }
    }

    Thread.sleep(1000)
  }

  @Test
  def testFlushMetaRequest(): Unit =
    Engine.storageActor ! FlushMetaRequest

  @Test
  def testLoadMetaRequest(): Unit = {
    implicit val timeout = Config.DEFAULT_AKKA_TIMEOUT

    val result = Engine.storageActor ? LoadMetaRequest
    Await.ready(result, Config.DEFAULT_AWAIT_TIMEOUT)

    println(Engine.totalDocumentCount.get)
    println(Engine.totalWordCount.get)
    println(Engine.wordCountInDocument)
  }
}
