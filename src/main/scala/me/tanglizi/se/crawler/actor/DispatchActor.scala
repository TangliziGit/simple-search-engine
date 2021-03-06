package me.tanglizi.se.crawler.actor

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.config.Config
import me.tanglizi.se.crawler.Crawler
import me.tanglizi.se.crawler.Crawler.{urlSet, urlVisitedFilter}
import me.tanglizi.se.entity.Protocol.{CrawlRequest, EnqueueCrawlRequest}

import scala.util.control.Breaks

class DispatchActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case EnqueueCrawlRequest(urls) =>
      val loop = new Breaks

      loop.breakable {
        for (url <- urls if !urlVisitedFilter.contains(url) && Crawler.urlFilter(url)) {
          if (urlVisitedFilter.size >= Config.MAX_URL_SET_SIZE || (Crawler.urlSetIsFull() && urlVisitedFilter.size >= urlSet.size))
            loop.break()

          if (!Crawler.urlSetIsFull()) {
            urlSet.add(url)
            if (!urlVisitedFilter.contains(url))
              Crawler.crawlActor ! CrawlRequest(url)
          } else if (urlSet.contains(url)) {
            // urlSet is full and contains url, update it
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
