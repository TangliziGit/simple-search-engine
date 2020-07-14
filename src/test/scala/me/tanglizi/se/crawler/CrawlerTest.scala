package me.tanglizi.se.crawler

import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.Protocol.EnqueueCrawlRequest
import org.junit._

@Test
class CrawlerTest {

  @Test
  def testCrawler(): Unit = {
    val urls: Array[String] = Array(
      "https://www.cnblogs.com/",
      "https://www.cnblogs.com/tanglizi/p/11515409.html",
      "https://www.cnblogs.com/tanglizi/",
      "https://news.cnblogs.com/n/667106/",
      "https://q.cnblogs.com/"
    )

    Engine.eraseData()
    Crawler.dispatchActor ! EnqueueCrawlRequest(urls)

    Thread.sleep(1000*60*2)
    Engine.flushData()
  }

}
