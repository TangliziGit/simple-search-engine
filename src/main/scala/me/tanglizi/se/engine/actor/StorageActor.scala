package me.tanglizi.se.engine.actor

import java.io.{BufferedReader, File, FileReader, FileWriter, PrintWriter, RandomAccessFile}

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.engine.config.Config
import me.tanglizi.se.entity.DocumentInfo
import me.tanglizi.se.entity.Protocol._
import me.tanglizi.se.util.HashUtil

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class StorageActor extends Actor with ActorLogging {

  def randomAccessFileReadLineForChinese(reader: RandomAccessFile): String = {
    val content: String = reader.readLine();
    val bytes: Array[Byte] = content.toCharArray.map(_.toByte)
    new String(bytes)
  }

  override def receive: Receive = {
    case StoreDocumentRequest(hash, documentInfo) =>
      val fileName: String = s"${hash % Config.CONTENT_HASH_SIZE}.content"
      val file: File = new File(Config.STORAGE_PATH, fileName)

      // return the file space, which means the document `offset` in this content file
      sender ! file.getTotalSpace
      log.info(s"$fileName (hash: $hash) will be chosen to be wrote the content")

      // write content
      val writer = new PrintWriter(new FileWriter(file, true))
      writer.println(documentInfo.title)
      writer.println(documentInfo.url)
      writer.println(documentInfo.content + "\n" + Config.CONTENT_SPLITTER)
      writer.close()

    case FindDocumentRequest(documentId) =>
      val indexItem: (Int, Long) = Engine.indexTable(documentId)
      val Array(documentHash, offset) = Array(indexItem._1, indexItem._2)

      // find filename by document hash code, and read it in random mode
      val fileName: String = s"${documentHash % Config.CONTENT_HASH_SIZE}.content"
      val file: File = new File(Config.STORAGE_PATH, fileName)
      val reader = new RandomAccessFile(file, "r")
      reader.seek(offset)

      val title: String = reader.readLine()
      val url: String = reader.readLine()
      val content: String = {
        var content: String = ""
        var line: String = ""

        while ({
          line = randomAccessFileReadLineForChinese(reader)
          line != Config.CONTENT_SPLITTER
        })
          content += line
        content
      }
      sender ! DocumentInfo(title, url, content)

    case FlushMetaRequest =>
      val file: File = new File(Config.STORAGE_PATH, Config.META_TABLE_FILE_NAME)

      val writer = new PrintWriter(new FileWriter(file))
      writer.println("DC" + Engine.totalDocumentCount)
      writer.println("WC" + Engine.totalWordCount)
      for ((docId, wordCount) <- Engine.wordCountInDocument)
        writer.println(s"$docId $wordCount")
      writer.close()

    case LoadMetaRequest =>
      val file: File = new File(Config.STORAGE_PATH, Config.META_TABLE_FILE_NAME)

      val reader = new BufferedReader(new FileReader(file))
      reader.lines()
        .forEach {
          case s"DC$documentCount" =>
            Engine.totalDocumentCount.set(documentCount.toLong)
          case s"WC$wordCount" =>
            Engine.totalWordCount.set(wordCount.toLong)
          case s"$docId $wordCount" =>
            Engine.wordCountInDocument.put(docId.toLong, wordCount.toLong)
        }
      reader.close()
      sender ! true

    case FlushIndexRequest =>
      val file: File = new File(Config.STORAGE_PATH, Config.INDEX_TABLE_FILE_NAME)

      // write index table
      val writer = new PrintWriter(new FileWriter(file, true))
      for ((key, (hash, offset)) <- Engine.indexTable)
        writer.println(s"$key $hash $offset")
      writer.close()

    case LoadIndexRequest =>
      val file: File = new File(Config.STORAGE_PATH, Config.INDEX_TABLE_FILE_NAME)

      // read to load index table
      val reader = new BufferedReader(new FileReader(file))
      reader.lines()
        .forEach {
          case s"$key $hash $offset" =>
            Engine.indexTable(key.toLong) = (hash.toInt, offset.toLong)
        }
      reader.close()
      sender ! true

    case FlushInvertedIndexRequest =>
      log.info("inverted index table will be flushed")

      // traverse inverted index table to restore
      for ((word, item) <- Engine.invertedIndexTable) {
        val hash: Long = HashUtil.hash(word)
        val fileName: String = s"${hash % Config.WORD_HASH_SIZE}.invert"
        val file: File = new File(Config.STORAGE_PATH, fileName)

        val writer = new PrintWriter(new FileWriter(file, true))

        for ((docId, ps) <- item)
          for (p <- ps)
            writer.println(s"$word $docId $p")

        log.info(s"$word has been flushed in ${hash % Config.WORD_HASH_SIZE}.invert")
        writer.close()
      }

    case FindInvertedIndexItemRequest(word) =>
      log.info(s"find inverted index item of $word")
      val hash: Long = HashUtil.hash(word)
      val fileName: String = s"${hash % Config.WORD_HASH_SIZE}.invert"
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

