package me.tanglizi.se

import me.tanglizi.se.engine.Engine
import org.junit.Test

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.entity.Protocol.{FlushIndexRequest, LoadIndexRequest, StoreContentRequest}
import me.tanglizi.se.util.HashUtil

import scala.concurrent.Future
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

}
