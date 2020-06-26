package me.tanglizi.se

import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.InvertedItem
import me.tanglizi.se.entity.Protocol.IndexRequest
import me.tanglizi.se.entity.Result.Token
import org.junit.Test

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

    Engine.invertedIndexTable.foreach{ case (keyword: String, item: InvertedItem) => {
      println(keyword, item)
    }}
  }

}