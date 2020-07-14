package me.tanglizi.se.crawler

import akka.actor.{ActorRef, ActorSystem, Props}
import me.tanglizi.se.crawler.actor.CrawlActor
import me.tanglizi.se.engine.Engine

import scala.collection.mutable

object CrawlerDispatcher {

  val urlSet = mutable.Set[String]()

  val actorSystem: ActorSystem = ActorSystem.create("crawlActorSystem")

  val crawlActor: ActorRef = actorSystem.actorOf(Props[CrawlActor], name = "crawlActor")

  def maintain(): Unit = {
    val urlQueue = mutable.Queue[String]()
    val filter = mutable.Set[String]()

    while (filter.size < 50 && filter.size < urlSet.size) {
      val url = urlQueue.front
      // TODO
    }

  }

}
