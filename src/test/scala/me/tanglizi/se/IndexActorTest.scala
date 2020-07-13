package me.tanglizi.se

import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Document
import me.tanglizi.se.entity.Protocol.{IndexRequest, IndexSearchRequest}
import me.tanglizi.se.entity.Result.Token
import org.junit.Test

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

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

    Engine.indexActor ! IndexRequest(1, "我爱北京天安门", tokens)

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
  }

  @Test
  def testIndexSearchRequest(): Unit = {
    val words = Array[String]("我", "爱", "北京", "天安门")

    Engine.indexActor ! IndexSearchRequest(words, xs => {
      println(xs)
    })

    Thread.sleep(1000)
  }

}
