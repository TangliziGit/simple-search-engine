package me.tanglizi.se

import me.tanglizi.se.engine.Engine
import org.junit.Test

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.entity.Protocol.StoreContentRequest

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.hashing.MurmurHash3


@Test
class StorageActorTest {

  @Test
  def testSCRequest(): Unit = {
    implicit val timeout: Timeout = Timeout(5.seconds)
    val content: String = "我爱北京天安门"
    val hash: Long = MurmurHash3.stringHash(content)

    val future: Future[Long] = (Engine.storageActor ? StoreContentRequest(hash, content)).mapTo[Long]

    future onComplete {
      case Success(value) =>
        println(value)
      case Failure(exception) =>
        exception.printStackTrace()
    }

    Thread.sleep(2000)
  }
}
