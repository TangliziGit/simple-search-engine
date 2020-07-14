package me.tanglizi.se.crawler.actor

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.crawler.Crawler
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Protocol.{AddRequest, CrawlRequest, EnqueueCrawlRequest}
import org.asynchttpclient.{AsyncHttpClient, Dsl, Response}

import scala.util.matching.Regex

class CrawlActor extends Actor with ActorLogging {

  def getUrlsFromResponse(response: Response): Array[String] = {
    val host: String = response.getUri.getHost
    val scheme: String = response.getUri.getScheme
    val result: String = CrawlActor.imgRegex.replaceAllIn(response.getResponseBody(), "")
    CrawlActor.urlRegex.findAllIn(result)
      .map(url => {
        if (url.contains("http")) url
        else s"$scheme://$host$url"
      })
      .toArray
  }

  override def receive: Receive = {
    case CrawlRequest(url) =>
      log.info(s"crawling $url")
      val httpClient: AsyncHttpClient = Dsl.asyncHttpClient()
      val response: Response = httpClient.prepareGet(url).execute().get()
      val urls: Array[String] = getUrlsFromResponse(response)

      Engine.engineActor ! AddRequest(response)
      Crawler.dispatchActor ! EnqueueCrawlRequest(urls)
  }

}

object CrawlActor {

  val imgRegex: Regex = new Regex("(?<=<img).*?(?=>)")
  val urlRegex: Regex = new Regex("(?<=href=\").*?(?=\")")

}
