package me.tanglizi.se.web.controller

import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.config.Config
import me.tanglizi.se.crawler.Crawler
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.{Document, DocumentInfo}
import me.tanglizi.se.entity.Protocol.{EnqueueCrawlRequest, SearchRequest}
import me.tanglizi.se.web.model.RestResponse
import org.springframework.web.bind.annotation.{GetMapping, RequestMapping, RequestParam, RestController}

import scala.concurrent.{Await, Future}

@RestController
@RequestMapping(Array("/", ""))
class MainController {

  @GetMapping(Array("search"))
  def search(@RequestParam("q") sentence: String): RestResponse[Array[DocumentInfo]] = {
    implicit val timeout: Timeout = Config.DEFAULT_AKKA_TIMEOUT

    val documents: List[Document] = {
      val future: Future[List[Document]] =
        (Engine.engineActor ? SearchRequest(sentence)).mapTo[List[Document]]
      Await.result(future, Config.DEFAULT_AWAIT_TIMEOUT)
    }

    println(documents)
    RestResponse.ok[Array[DocumentInfo]](
      documents.map(_.documentInfo).toArray
    )
  }

  @GetMapping(Array("crawl"))
  def crawl(): RestResponse[Null] = {
    Crawler.initMaintain()
    Crawler.dispatchActor ! EnqueueCrawlRequest(Crawler.urlSet.toArray)

    RestResponse.ok(null)
  }

  @GetMapping(Array("flush"))
  def flush(): RestResponse[Null] = {
    Engine.asyncFlushData()
    Crawler.storeData()

    RestResponse.ok(null)
  }

}
