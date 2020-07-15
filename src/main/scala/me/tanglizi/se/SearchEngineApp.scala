package me.tanglizi.se

import me.tanglizi.se.crawler.Crawler
import me.tanglizi.se.engine.Engine
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

object SearchEngineApp {

  def main(args: Array[String]): Unit = {
    Engine.loadData()
    Crawler.loadData()
    SpringApplication.run(classOf[SearchEngineApp], args: _*)
  }

}

@SpringBootApplication
class SearchEngineApp {}