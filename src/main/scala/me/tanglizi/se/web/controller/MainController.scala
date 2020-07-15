package me.tanglizi.se.web.controller

import akka.pattern._
import akka.util.Timeout
import me.tanglizi.se.config.Config
import me.tanglizi.se.crawler.Crawler
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.entity.{Document, DocumentInfo}
import me.tanglizi.se.entity.Protocol.{EnqueueCrawlRequest, SearchRequest}
import me.tanglizi.se.util.SegmentUtil
import me.tanglizi.se.web.model.{RestResponse, SearchResult}
import org.springframework.web.bind.annotation.{GetMapping, RequestMapping, RequestParam, RestController}

import scala.concurrent.{Await, Future}

@RestController
@RequestMapping(Array("/", ""))
class MainController {

  @GetMapping(Array("search"))
  def search(@RequestParam("q") sentence: String): RestResponse[SearchResult] = {
    implicit val timeout: Timeout = Config.WEB_AKKA_TIMEOUT

    val documents: List[Document] = {
      val future: Future[List[Document]] =
        (Engine.engineActor ? SearchRequest(sentence)).mapTo[List[Document]]
      Await.result(future, Config.WEB_AWAIT_TIMEOUT)
    }

    val searchResult: SearchResult = SearchResult(
      documents.map(_.documentInfo).slice(0, Config.MAX_SEARCH_RESULT_LENGTH).toSet.toArray,
      SegmentUtil.getTokens(sentence)
    )

    RestResponse.ok[SearchResult](searchResult)
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
