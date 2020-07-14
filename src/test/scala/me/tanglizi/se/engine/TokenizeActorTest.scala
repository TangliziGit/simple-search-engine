package me.tanglizi.se

import scala.concurrent.duration._
import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Protocol.{TokenizeDocumentRequest, TokenizeSearchWordRequest}
import org.asynchttpclient.{Dsl, Response}
import org.junit.Test

import scala.concurrent.{Await, Future}

@Test
class TokenizeActorTest {

  val response: Response =
    Dsl.asyncHttpClient().prepareGet("https://www.cnblogs.com").execute().get()

  @Test
  def testTokenizeDocumentRequest(): Unit = {
    Engine.tokenizeActor ! TokenizeDocumentRequest(1, response)

    Thread.sleep(1000)
  }

  @Test
  def testTokenizeSearchWordRequest(): Unit = {
    implicit val timeout: Timeout = Timeout(120.seconds)
    val wordsFuture: Future[Array[String]] = (Engine.tokenizeActor ? TokenizeSearchWordRequest("我爱北京天安门")).mapTo[Array[String]]
    val words = Await.result(wordsFuture, 120.seconds)

    println(words.mkString(", "))
  }
}
