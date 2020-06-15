package me.tanglizi.se.engine.actor

import java.io.{File, FileInputStream, FileOutputStream, FileWriter, PrintWriter}

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.entity.Protocol._

class StorageActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case StoreContentRequest(hash, content) =>
      val fileName: String = s"${hash % 30}.content"
      val file: File = new File("/home/tanglizi/tmp/", fileName)  // TODO: set a path constant

      sender ! file.getTotalSpace
      log.info(s"$fileName (hash: $hash) will be chosen to be wrote the content")
      val writer = new PrintWriter(new FileWriter(file, true))
      writer.println(content + "\n")
      writer.close()

    case FlushIndexRequest =>

    case FlushInvertedIndexRequest =>

    case FindInvertedIndexItemRequest(word) =>

  }
}
