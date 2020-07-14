package me.tanglizi.se.crawler

import me.tanglizi.se.crawler.actor.CrawlActor
import org.asynchttpclient.{Dsl, Response}
import org.junit._

@Test
class CrawlActorTest {

  @Test
  def testGetUrlsFromHtml(): Unit = {
    val response: Response = Dsl.asyncHttpClient().prepareGet("https://www.cnblogs.com").execute().get()
    val urls: Array[String] = {
      val host: String = response.getUri.getHost
      val scheme: String = response.getUri.getScheme
      val result: String = CrawlActor.imgRegex.replaceAllIn(response.getResponseBody(), "")
      CrawlActor.hrefRegex.findAllIn(result)
        .map(url => {
          if (url.contains("http")) url
          else s"$scheme://$host$url"
        })
        .toArray
    }

    println(urls.mkString(", "))
  }

}
