package me.tanglizi.se

import org.junit._
import Assert._
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Document
import me.tanglizi.se.entity.Protocol.SearchRequest

@Test
class EngineActorTest {

  @Test
  def testSearchRequest(): Unit = {
    val sentence: String = "我 天安门"
    val callback: List[Document] => Unit =
      docs => println(docs)

    Engine.engineActor ! SearchRequest(sentence, callback)

    Thread.sleep(1000)
  }
}
