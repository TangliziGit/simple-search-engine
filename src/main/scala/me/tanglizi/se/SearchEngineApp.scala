package me.tanglizi.se

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

object SearchEngineApp {

  def main(args: Array[String]): Unit =
    SpringApplication.run(classOf[SearchEngineApp], args: _*)

}

@SpringBootApplication
class SearchEngineApp {}