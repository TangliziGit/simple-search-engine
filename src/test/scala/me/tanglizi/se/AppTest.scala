package me.tanglizi.se

import org.junit._
import Assert._
import me.tanglizi.se.crawler.Crawler
import me.tanglizi.se.engine.Engine

@Test
class AppTest {

  @Test
  def flushData(): Unit = {
    Engine.loadData()
    Crawler.loadData()

    Engine.flushData()
    Crawler.storeData()
    Engine.rearrangeData()
  }

}


