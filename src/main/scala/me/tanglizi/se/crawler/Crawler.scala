package me.tanglizi.se.crawler

import java.io.{BufferedReader, File, FileReader, FileWriter, PrintWriter}
import java.util.concurrent.ConcurrentHashMap

import akka.actor.{ActorRef, ActorSystem, Props}
import me.tanglizi.se.config.Config
import me.tanglizi.se.crawler.actor.{CrawlActor, DispatchActor}

import scala.collection.mutable

object Crawler {

  val urlSet = mutable.Set[String]()
  val urlHashMap = mutable.Map[String, String]()

  val actorSystem: ActorSystem = ActorSystem.create("crawlActorSystem")

  val crawlActor: ActorRef = actorSystem.actorOf(Props[CrawlActor], name = "crawlActor")
  val dispatchActor: ActorRef = actorSystem.actorOf(Props[DispatchActor], name = "dispatchActor")

  // var urlVisitedFilter = new ConcurrentHashMap[String, Boolean]()
  var urlVisitedFilter = mutable.Set[String]()

  def urlSetIsFull(): Boolean = urlSet.size >= Config.MAX_URL_SET_SIZE
  def urlFilter(url: String): Boolean = url.contains("cnblogs")

  def initMaintain(): Unit = {
    loadData()
    urlVisitedFilter.clear()
  }

  def loadData(): Unit = {
    val file: File = new File(Config.STORAGE_PATH, Config.URL_SET)
    if (file.exists()) {
      val reader = new BufferedReader(new FileReader(file))
      reader.lines()
        .forEach {
          case s"$url $hash" =>
            urlHashMap.put(url, hash)
            urlSet.add(url)
        }
    }
  }

  def storeData(): Unit = {
    val file: File = new File(Config.STORAGE_PATH, Config.URL_SET)
    val writer = new PrintWriter(new FileWriter(file))

    for ((url, contentHash) <- urlHashMap)
      writer.println(s"$url $contentHash")
    writer.close()
  }

}
