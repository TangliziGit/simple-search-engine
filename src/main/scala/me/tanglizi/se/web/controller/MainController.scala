package me.tanglizi.se.web.controller

import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.config.Config
import me.tanglizi.se.crawler.Crawler
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.{Document, DocumentInfo}
import me.tanglizi.se.entity.Protocol.{AsyncSearchRequest, EnqueueCrawlRequest, SearchRequest}
import me.tanglizi.se.web.controller.model.RestResponse
import org.springframework.web.bind.annotation.{GetMapping, RequestMapping, RequestParam, RestController}

import scala.concurrent.{Await, Future}

@RestController
@RequestMapping(Array("/", ""))
class MainController {

  @GetMapping("search")
  def search(@RequestParam("q") sentence: String): RestResponse[Array[DocumentInfo]] = {
    implicit val timeout: Timeout = Config.DEFAULT_AKKA_TIMEOUT

    val documents: Array[Document] = {
      val future: Future[Array[Document]] =
        (Engine.engineActor ? SearchRequest(sentence)).mapTo[Array[Document]]
      Await.result(future, Config.DEFAULT_AWAIT_TIMEOUT)
    }

    RestResponse.ok[Array[DocumentInfo]](
      documents.map(_.documentInfo)
    )
  }

  @GetMapping("crawl")
  def crawl(): RestResponse[Null] = {
    Crawler.initMaintain()
    Crawler.dispatchActor ! EnqueueCrawlRequest(Crawler.urlSet.toArray)

    RestResponse.ok(null)
  }

  @GetMapping("flush")
  def flush(): RestResponse[Null] = {
    Engine.asyncFlushData()

    RestResponse.ok(null)
  }

}
