package me.tanglizi.se

import org.junit._
import Assert._
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Document
import me.tanglizi.se.entity.Protocol.{AddRequest, SearchRequest}
import org.asynchttpclient.{AsyncHttpClient, Dsl, Response}

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

  @Test
  def testAddRequest(): Unit = {
    val httpClient: AsyncHttpClient = Dsl.asyncHttpClient()
    val urls: Array[String] = Array(
      "https://www.cnblogs.com",
      "https://www.cnblogs.com/tanglizi/p/11515409.html",
      "https://www.cnblogs.com/tanglizi/",
      "https://news.cnblogs.com/n/667106/",
      "https://q.cnblogs.com/"
    )

    val responses: Array[Response] =
      urls.map(url => httpClient.prepareGet(url).execute().get())

    for (response <- responses)
      Engine.engineActor ! AddRequest(response)

    Thread.sleep(20000)
  }
}
