package me.tanglizi.se.engine.actor

import java.io.{File, FileInputStream, FileOutputStream, PrintWriter}

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.entity.Protocol._

class StorageActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case StoreContentRequest(hash, content) =>
      val fileName: String = s"${hash % 30}.content"
      val file: File = new File("/home/tanglizi/tmp/", fileName)  // TODO: set a path constant
      
      sender ! file.getTotalSpace
      val writer = new PrintWriter(new FileOutputStream(file))
      writer.println(content)
      writer.close()

    case FlushIndexRequest =>

    case FlushInvertedIndexRequest =>

    case FindInvertedIndexItemRequest(word) =>

  }
}
