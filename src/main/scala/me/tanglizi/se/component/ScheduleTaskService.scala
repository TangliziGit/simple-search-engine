package me.tanglizi.se.component

import me.tanglizi.se.crawler.Crawler
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Protocol.EnqueueCrawlRequest
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduleTaskService {
  val logger = LoggerFactory.getLogger(classOf[ScheduleTaskService])

  // @Scheduled(cron = "0 0 0 1/2 * ? *")
  def crawl(): Unit = {
    logger.info("start crawl")
    Crawler.initMaintain()
    Crawler.dispatchActor ! EnqueueCrawlRequest(Crawler.urlSet.toArray)
  }

  // @Scheduled(cron = "0 0 0 1/2 * ? *")
  def flushData(): Unit = {
    Engine.flushData()
    Crawler.storeData()
    Engine.rearrangeData()
  }

}
