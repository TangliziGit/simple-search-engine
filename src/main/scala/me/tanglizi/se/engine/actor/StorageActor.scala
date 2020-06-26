package me.tanglizi.se.engine.actor

import java.io.{BufferedReader, File, FileInputStream, FileOutputStream, FileReader, FileWriter, PrintWriter}

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.engine.config.Config
import me.tanglizi.se.entity.Protocol._

class StorageActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case StoreContentRequest(hash, content) =>
      val fileName: String = s"${hash % 5}.content"
      val file: File = new File(Config.STORAGE_PATH, fileName)  // TODO: set a path constant

      sender ! file.getTotalSpace
      log.info(s"$fileName (hash: $hash) will be chosen to be wrote the content")
      val writer = new PrintWriter(new FileWriter(file, true))
      writer.println(content + "\n")
      writer.close()

    case FlushIndexRequest =>
      // TODO
      val file: File = new File(Config.STORAGE_PATH, "indexTable.data")
      val writer = new PrintWriter(new FileWriter(file))
      for ((key, value) <- Engine.indexTable)
        writer.println(s"$key $value")
      writer.close()

    case LoadIndexRequest =>
      // TODO
      val file: File = new File(Config.STORAGE_PATH, "indexTable.data")
      val reader = new BufferedReader(new FileReader(file))
      reader.lines()
        .forEach {
          case s"$key $value" =>
            Engine.indexTable(key.toLong) = value.toLong
        }

    case FlushInvertedIndexRequest =>

    case FindInvertedIndexItemRequest(word) =>

  }
}

