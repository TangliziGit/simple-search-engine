package me.tanglizi.se

import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Protocol.TokenizeDocumentRequest
import org.asynchttpclient.{Dsl, Response}
import org.junit.Test

@Test
class TokenizeActor {

  val response: Response =
    Dsl.asyncHttpClient().prepareGet("https://www.cnblogs.com").execute().get()

  @Test
  def testTokenizeDocumentRequest(): Unit = {
    Engine.tokenizeActor ! TokenizeDocumentRequest(1, response)

    Thread.sleep(1000)
  }
}
