package me.tanglizi.se.engine.actor

import java.io.{BufferedReader, File, FileReader, FileWriter, PrintWriter}

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.engine.config.Config
import me.tanglizi.se.entity.Protocol._
import me.tanglizi.se.util.HashUtil

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class StorageActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case StoreContentRequest(hash, content) =>
      val fileName: String = s"${hash % 5}.content"
      val file: File = new File(Config.STORAGE_PATH, fileName)

      sender ! file.getTotalSpace
      log.info(s"$fileName (hash: $hash) will be chosen to be wrote the content")
      val writer = new PrintWriter(new FileWriter(file, true))
      writer.println(content + "\n")
      writer.close()

    case FlushIndexRequest =>
      val file: File = new File(Config.STORAGE_PATH, Config.INDEX_TABLE_FILE_NAME)
      val writer = new PrintWriter(new FileWriter(file, true))
      for ((key, value) <- Engine.indexTable)
        writer.println(s"$key $value")
      writer.close()

    case LoadIndexRequest =>
      val file: File = new File(Config.STORAGE_PATH, Config.INDEX_TABLE_FILE_NAME)
      val reader = new BufferedReader(new FileReader(file))
      reader.lines()
        .forEach {
          case s"$key $value" =>
            Engine.indexTable(key.toLong) = value.toLong
        }

    case FlushInvertedIndexRequest =>
      log.info("inverted index table will be flushed")
      for ((word, item) <- Engine.invertedIndexTable) {
        val hash: Long = HashUtil.hash(word)
        val fileName: String = s"${hash % 5}.invert"
        val file: File = new File(Config.STORAGE_PATH, fileName)
        val writer = new PrintWriter(new FileWriter(file, true))

        for ((docId, ps) <- item)
          for (p <- ps)
            writer.println(s"$word $docId $p")

        log.info(s"$word has been flushed in ${hash % 5}.invert")
        writer.close()
      }

    case FindInvertedIndexItemRequest(word) =>
      val hash: Long = HashUtil.hash(word)
      val fileName: String = s"${hash % 5}.invert"
      val file: File = new File(Config.STORAGE_PATH, fileName)
      val reader = new BufferedReader(new FileReader(file))

      val item = mutable.Map[Long, ArrayBuffer[Int]]()
      reader.lines()
        .forEach {
          case s"$keyword $docId $position" if keyword == word =>
            val arr: ArrayBuffer[Int] = item.getOrElseUpdate(docId.toLong, ArrayBuffer[Int]())
            arr += position.toInt
            // log.info(s"OKOKOKOK $keyword $docId $position")
          case _ =>
        }

      reader.close()
      sender ! item
  }
}

