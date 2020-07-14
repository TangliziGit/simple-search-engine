package me.tanglizi.se.crawler.actor

import akka.pattern._
import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.crawler.CrawlerDispatcher
import me.tanglizi.se.crawler.CrawlerDispatcher.{filter, urlSet}
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Protocol.{CrawlRequest, EnqueueCrawlRequest}

import scala.util.control.Breaks

class DispatchActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case EnqueueCrawlRequest(urls) =>
      val loop = new Breaks;

      loop.breakable {
        for (url <- urls if !filter.contains(url)) {
          if (!(filter.size < 50 && filter.size < urlSet.size))
            loop.break()

          filter.add(url)
          if (urlSet.size < 50) {
            // urlSet is not full
            urlSet.add(url)
            CrawlerDispatcher.crawlActor ! CrawlRequest(url)
          } else if (urlSet.contains(url)) {
            // urlSet is full and contains url, update it
            // TODO: EngineActor ! DeleteIfChangedRequest
            CrawlerDispatcher.crawlActor ! CrawlRequest(url)
          }

        }
      }
  }

}
