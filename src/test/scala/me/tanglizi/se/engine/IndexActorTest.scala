package me.tanglizi.se.engine

import akka.pattern._
import me.tanglizi.se.config.Config
import me.tanglizi.se.entity.DocumentInfo
import me.tanglizi.se.entity.Protocol._
import me.tanglizi.se.entity.Result.Token
import org.junit.Test

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await

@Test
class IndexActorTest {

  @Test
  def testIndexRequest(): Unit = {
    val tokens: Array[Token] = Array[Token](
      Token("我", Array(0)),
      Token("爱", Array(1)),
      Token("北京", Array(2)),
      Token("天安门", Array(4)),
    )

    Engine.indexActor ! IndexRequest(1, DocumentInfo("title", "url", "我爱北京天安门"), tokens)

    Thread.sleep(1000)

    Engine.indexTable.foreach{case (index, offset) => {
      println(index, offset)
    }}

    Engine.invertedIndexTable.foreach{
      case (keyword: String, item: mutable.Map[Long, ArrayBuffer[Int]]) =>
        println(keyword, item)
    }

    println(Engine.totalDocumentCount.get)
    println(Engine.totalWordCount.get)
    println(Engine.wordCountInDocument)

    Engine.storageActor ! FlushInvertedIndexRequest
    Engine.storageActor ! FlushIndexRequest
    Engine.storageActor ! FlushMetaRequest

    Thread.sleep(1000)
  }

  @Test
  def testIndexSearchRequest(): Unit = {
    val words = Array[String]("我", "爱", "北京", "天安门")

    implicit val timeout = Config.DEFAULT_AKKA_TIMEOUT
    val result1 = Engine.storageActor ? LoadMetaRequest
    val result2 = Engine.storageActor ? LoadIndexRequest

    Await.ready(result1, Config.DEFAULT_AWAIT_TIMEOUT)
    Await.ready(result2, Config.DEFAULT_AWAIT_TIMEOUT)

    Engine.indexActor ! IndexSearchRequest(words, xs => {
      println(xs)
    })

    Thread.sleep(1000)
  }

}
