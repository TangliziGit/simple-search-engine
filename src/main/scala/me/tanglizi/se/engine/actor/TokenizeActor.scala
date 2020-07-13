package me.tanglizi.se.engine.actor

import java.util

import akka.actor.{Actor, ActorLogging}
import io.github.yizhiru.thulac4j.Segmenter
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.DocumentInfo
import me.tanglizi.se.entity.Protocol._
import me.tanglizi.se.entity.Result.Token

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

class TokenizeActor extends Actor with ActorLogging {

  def getSegments(content: String): Array[String] = {
    val words: Array[String] = {
      Segmenter.enableFilterStopWords()
      val words: util.List[String] = Segmenter.segment(content)
      words.toArray[String](Array.ofDim[String](words.size))
    }
    words
  }

  override def receive: Receive = {
    case TokenizeDocumentRequest(id, response) =>
      val (html, url) = (response.getResponseBody, response.getUri)

      // find title by regex
      val title: String = TokenizeActor.titleRegex.findFirstIn(html).getOrElse("No Title")

      // get page content by regex replace
      val content: String = {
        var content: String = html
        content = TokenizeActor.removeCodeRegex.replaceAllIn(content, "\n")
        content = TokenizeActor.htmlEscapeRegex.replaceAllIn(content, "\u0001")
        content = TokenizeActor.tempCharRegex.replaceAllIn(content, "\n")
        content = TokenizeActor.gatherRegex.replaceAllIn(content, "\n")
        content = TokenizeActor.gatherRegex.replaceAllIn(content, "\n")
        content
      }

      val words: Array[String] = getSegments(content)

      // convert tokenize result into word position list
      // word -> [pos1, pos2]
      var position: Int = 0
      val positionMap = mutable.Map[String, mutable.ArrayBuffer[Int]]()
      for (word <- words) {
        positionMap.getOrElseUpdate(word, ArrayBuffer[Int]()) += position
        position += word.length
      }

      // wrap word position list into Token
      val result: Array[Token] = positionMap
        .map { case (word, positions) => Token(word, positions.toArray) }
        .toArray

      log.info(s"tokenizer result: ${result.mkString(", ")}")
      log.info(s"content title: $title, url: $url")

      val documentInfo: DocumentInfo = DocumentInfo(title, url.toUrl, content)

      Engine.indexActor ! IndexRequest(id, documentInfo, result)

    case TokenizeSearchWordRequest(sentence) =>
      // simply tokenize the search sentence
      val words: Array[String] = getSegments(sentence)
      sender ! words
  }
}

object TokenizeActor {
  val titleRegex: Regex = new Regex("(?<=<title>).*(?=</title>)")
  val removeCodeRegex: Regex = new Regex("<script[\\s\\S]*?</script>")
  val htmlEscapeRegex: Regex = new Regex("<[^>]*>")
  val tempCharRegex: Regex = new Regex("\u0001+");
  val gatherRegex: Regex = new Regex("[\n\r \t]+")
}
