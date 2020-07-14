package me.tanglizi.se.crawler.actor

import akka.pattern._
import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.crawler.Crawler
import me.tanglizi.se.crawler.Crawler.{urlVisitedFilter, urlSet}
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Protocol.{CrawlRequest, EnqueueCrawlRequest}

import scala.util.control.Breaks

class DispatchActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case EnqueueCrawlRequest(urls) =>
      log.info(s"total pages number: ${urlVisitedFilter.size}")
      val loop = new Breaks

      loop.breakable {
        for (url <- urls if !urlVisitedFilter.contains(url) && Crawler.urlFilter(url)) {
          if (urlVisitedFilter.size >= 50 || (Crawler.urlSetIsFull() && urlVisitedFilter.size >= urlSet.size))
            loop.break()

          if (!Crawler.urlSetIsFull()) {
            urlSet.add(url)
            if (!urlVisitedFilter.contains(url))
              Crawler.crawlActor ! CrawlRequest(url)
          } else if (urlSet.contains(url)) {
            // urlSet is full and contains url, update it
            // TODO: EngineActor ! DeleteIfChangedRequest
            if (!urlVisitedFilter.contains(url))
              Crawler.crawlActor ! CrawlRequest(url)
          }

          urlVisitedFilter.synchronized {
            urlVisitedFilter.add(url)
          }
        }
      }
  }

}
