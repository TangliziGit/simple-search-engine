package me.tanglizi.se.engine.actor

import java.util

import akka.actor.{Actor, ActorLogging}
import io.github.yizhiru.thulac4j.Segmenter
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Protocol._
import me.tanglizi.se.entity.Result.Token

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

class TokenizeActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case TokenizeDocumentRequest(id, response) =>
      val (html, url) = (response.getResponseBody, response.getUri)
      val title: String = TokenizeActor.titleRegex.findFirstIn(html).getOrElse("No Title")
      val content: String = {
        var content: String = html
        content = TokenizeActor.removeCodeRegex.replaceAllIn(content, "\n")
        content = TokenizeActor.htmlEscapeRegex.replaceAllIn(content, "\u0001")
        content = TokenizeActor.tempCharRegex.replaceAllIn(content, "\n")
        content = TokenizeActor.gatherRegex.replaceAllIn(content, "\n")
        content = TokenizeActor.gatherRegex.replaceAllIn(content, "\n")
        content
      }

      val words: Array[String] = {
        Segmenter.enableFilterStopWords()
        val words: util.List[String] = Segmenter.segment(content)
        words.toArray[String](Array.ofDim[String](words.size))
      }

      var position: Int = 0
      val positionMap = mutable.Map[String, mutable.ArrayBuffer[Int]]()
      for (word <- words) {
        positionMap.getOrElseUpdate(word, ArrayBuffer[Int]()) += position
        position += word.length
      }

      val result: Array[Token] = positionMap
        .map { case (word, positions) => Token(word, positions.toArray) }
        .toArray

      log.info(s"tokenizer result: ${result.mkString(", ")}")
      log.info(s"content title: $title, url: $url")

      Engine.indexActor ! IndexRequest(id, content, result)

    case TokenizeSearchWordRequest(word) =>

  }
}

object TokenizeActor {
  val titleRegex: Regex = new Regex("(?<=<title>).*(?=</title>)")
  val removeCodeRegex: Regex = new Regex("<script[\\s\\S]*?</script>")
  val htmlEscapeRegex: Regex = new Regex("<[^>]*>")
  val tempCharRegex: Regex = new Regex("\u0001+");
  val gatherRegex: Regex = new Regex("[\n\r \t]+")
}
