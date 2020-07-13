package me.tanglizi.se

import me.tanglizi.se.engine.Engine
import org.junit.Test

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.engine.config.Config
import me.tanglizi.se.entity.Protocol.{FindInvertedIndexItemRequest, FlushIndexRequest, FlushInvertedIndexRequest, FlushMetaRequest, IndexRequest, LoadIndexRequest, LoadMetaRequest, StoreContentRequest, TokenizeDocumentRequest}
import me.tanglizi.se.entity.Result.Token
import me.tanglizi.se.util.HashUtil

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}


@Test
class StorageActorTest {

  @Test
  def testStoreContentRequest(): Unit = {
    implicit val timeout: Timeout = Timeout(5.seconds)
    val content: String = "我爱北京天安门"
    val hash: Long = HashUtil.hash(content)

    val future: Future[Long] = (Engine.storageActor ? StoreContentRequest(hash, content)).mapTo[Long]

    future onComplete {
      case Success(value) =>
        println(value)
      case Failure(exception) =>
        exception.printStackTrace()
    }

    Thread.sleep(2000)
  }

  @Test
  def testFlushIndexRequest(): Unit = {
    implicit val timeout: Timeout = Timeout(5.seconds)
    val content: String = "我爱北京天安门"

    for (i <- Range(1, 10)) {
      val newContent: String = content + i.toString
      val hash: Long = HashUtil.hash(newContent)

      val future: Future[Long] =
        (Engine.storageActor ? StoreContentRequest(hash, newContent)).mapTo[Long]

      future onComplete {
        case Success(value) =>
          Engine.indexTable(i) = value
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
    val tokens: Array[Token] = Array[Token](
      Token("我", Array(0)),
      Token("爱", Array(1)),
      Token("北京", Array(2)),
      Token("天安门", Array(4)),
    )

    for (i <- Range(0, 20))
      Engine.indexActor ! IndexRequest(i, content, tokens)
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
