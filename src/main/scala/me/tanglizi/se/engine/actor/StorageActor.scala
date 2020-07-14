package me.tanglizi.se.engine.actor

import java.io.{BufferedReader, File, FileReader, FileWriter, PrintWriter, RandomAccessFile}
import java.nio.channels.{FileChannel, FileLock}

import akka.actor.{Actor, ActorLogging}
import me.tanglizi.se.engine.Engine
import me.tanglizi.se.config.Config
import me.tanglizi.se.entity.DocumentInfo
import me.tanglizi.se.entity.Protocol._
import me.tanglizi.se.util.HashUtil

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class StorageActor extends Actor with ActorLogging {

  def flushIndexTable(): Unit = {
    val file: File = new File(Config.STORAGE_PATH, Config.INDEX_TABLE_FILE_NAME)

    // write index table
    val writer = new PrintWriter(new FileWriter(file, false))
    for ((key, (hash, offset)) <- Engine.indexTable)
      writer.println(s"$key $hash $offset")
    writer.close()
  }

  def flushMetaTable(): Unit = {
    val file: File = new File(Config.STORAGE_PATH, Config.META_TABLE_FILE_NAME)

    val writer = new PrintWriter(new FileWriter(file))
    writer.println("DC" + Engine.totalDocumentCount)
    writer.println("WC" + Engine.totalWordCount)
    for ((docId, wordCount) <- Engine.wordCountInDocument) {
      val url: String = Engine.documentIdToUrl(docId)
      writer.println(s"$docId $wordCount $url")
    }
    writer.close()
  }

  def randomAccessFileReadLineForChinese(reader: RandomAccessFile): String = {
    val content: String = reader.readLine();
    val bytes: Array[Byte] = content.toCharArray.map(_.toByte)
    new String(bytes)
  }

  def rearrangeContentTables(): Unit = {
    val directory = new File(Config.STORAGE_PATH)
    val documentInfos: ArrayBuffer[DocumentInfo] = ArrayBuffer[DocumentInfo]()
    for (file <- directory.listFiles((dir, name) => name.contains(".content"))) {

      // use shared lock to read the file
      val reader = new RandomAccessFile(file, "r")
      val sLock: FileLock = reader.getChannel.lock(0, Long.MaxValue, true)

      // read all content
      while (reader.getFilePointer < reader.length()) {
        val title: String = randomAccessFileReadLineForChinese(reader)
        val url: String = reader.readLine()
        val content: String = {
          var Array(content, line) = Array("", "")

          while ({
            line = randomAccessFileReadLineForChinese(reader)
            line != Config.CONTENT_SPLITTER
          })
            content += line
          content
        }

        val documentId: Long = Engine.documentUrlToId(url)
        if (!Engine.deletedDocumentIds.contains(documentId))
          documentInfos += DocumentInfo(title, url, content)
      }
      sLock.release()
      reader.close()

      file.delete()

      // write content
      // use exclusive lock to write the file
      val writer = new RandomAccessFile(file, "rw")
      val xLock: FileLock = writer.getChannel.lock()

      for (documentInfo <- documentInfos) {
        writer.write(s"${documentInfo.title}${Config.CRLF}".getBytes())
        writer.write(s"${documentInfo.url}${Config.CRLF}".getBytes())
        writer.write(s"${documentInfo.content}${Config.CRLF + Config.CONTENT_SPLITTER + Config.CRLF}".getBytes())
      }
      xLock.release()
      writer.close()
    }
  }

  def rearrangeInvertIndexTables(): Unit = {
    val directory = new File(Config.STORAGE_PATH)
    val invertedIndexes = ArrayBuffer[(String, String, String)]()

    for (file <- directory.listFiles((dir, name) => name.contains(".invert"))) {
      // TODO: should we use shared lock to read file?
      val reader = new BufferedReader(new FileReader(file))
      reader.lines()
        .forEach {
          case s"$keyword $docId $position" if !Engine.deletedDocumentIds.contains(docId.toLong) =>
            invertedIndexes.+=((keyword, docId, position))
          case _ =>
        }

      file.delete()

      // write content
      // use exclusive lock to write the file
      val writer = new RandomAccessFile(file, "rw")
      val xLock: FileLock = writer.getChannel.lock()

      for ((keyword, docId, position) <- invertedIndexes)
          writer.write(s"$keyword $docId $position${Config.CRLF}".getBytes())

      xLock.release()
      writer.close()
    }
  }

  def rearrangeIndexTable(): Unit = {
    // need flush
    for (documentId <- Engine.deletedDocumentIds)
      Engine.indexTable.remove(documentId)
  }

  def rearrangeMetaTable(): Unit = {
    // need flush
    for (documentId <- Engine.deletedDocumentIds) {
      val wordCount = Engine.wordCountInDocument(documentId)
      Engine.totalWordCount.addAndGet(-wordCount)
      Engine.totalDocumentCount.decrementAndGet()
      Engine.wordCountInDocument.remove(documentId)
    }
  }

  override def receive: Receive = {
    case StoreDocumentRequest(hash, documentInfo) =>
      val fileName: String = s"${hash % Config.CONTENT_HASH_SIZE}.content"
      val file: File = new File(Config.STORAGE_PATH, fileName)

      // return the file space, which means the document `offset` in this content file
      log.info(s"$fileName (hash: $hash) will be chosen to be wrote the content")

      // write content
      val writer = new RandomAccessFile(file, "rw")
      val channel: FileChannel = writer.getChannel
      val offset: Long = writer.length()

      // use exclusive lock to write the file
      val xLock: FileLock = channel.lock()
      writer.seek(offset)
      writer.write(s"${documentInfo.title}${Config.CRLF}".getBytes())
      writer.write(s"${documentInfo.url}${Config.CRLF}".getBytes())
      writer.write(s"${documentInfo.content}${Config.CRLF + Config.CONTENT_SPLITTER + Config.CRLF}".getBytes())
      sender ! offset
      xLock.release()
      writer.close()

    case FindDocumentRequest(documentId) =>
      val indexItem: (Int, Long) = Engine.indexTable(documentId)
      val Array(documentHash, offset) = Array(indexItem._1, indexItem._2)

      // find filename by document hash code, and read it in random mode
      val fileName: String = s"${documentHash % Config.CONTENT_HASH_SIZE}.content"
      val file: File = new File(Config.STORAGE_PATH, fileName)
      val reader = new RandomAccessFile(file, "r")

      // use shared lock to read the file
      val channel: FileChannel = reader.getChannel
      val sLock: FileLock = channel.lock(0, Long.MaxValue, true)
      reader.seek(offset)

      val title: String = randomAccessFileReadLineForChinese(reader)
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
      sLock.release()
      reader.close()
      sender ! DocumentInfo(title, url, content)

    case FlushMetaRequest =>
      flushMetaTable()
      sender ! true

    case LoadMetaRequest =>
      val file: File = new File(Config.STORAGE_PATH, Config.META_TABLE_FILE_NAME)

      val reader = new BufferedReader(new FileReader(file))
      reader.lines()
        .forEach {
          case s"DC$documentCount" =>
            Engine.totalDocumentCount.set(documentCount.toLong)
          case s"WC$wordCount" =>
            Engine.totalWordCount.set(wordCount.toLong)
          case s"$docId $wordCount $url" =>
            Engine.wordCountInDocument.put(docId.toLong, wordCount.toLong)
            Engine.documentIdToUrl.put(docId.toLong, url)
            Engine.documentUrlToId.put(url, docId.toLong)
        }
      reader.close()
      sender ! true

    case FlushIndexRequest =>
      flushIndexTable()
      sender ! true

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
      println("1")
      for ((word, item) <- Engine.invertedIndexTable) {
        val hash: Long = HashUtil.hashMurmur3(word)
        println("1")
        val fileName: String = s"${hash % Config.WORD_HASH_SIZE}.invert"
        println("1")
        val file: File = new File(Config.STORAGE_PATH, fileName)
        println("1")

        val writer = new RandomAccessFile(file, "rw")
        println("1")
        writer.seek(writer.length())
        println("1")

        // use exclusive lock to write files
        val channel: FileChannel = writer.getChannel
        val xLock: FileLock = channel.lock()
        println("1")

        for ((docId, ps) <- item)
          for (p <- ps)
            writer.write(s"$word $docId $p${Config.CRLF}".getBytes())

        println("1")
        xLock.release()
        writer.close()
        println("1")
      }
      Engine.invertedIndexTable.clear()
      sender ! true

    case FindInvertedIndexItemRequest(word) =>
      // TODO: should we use shared lock to read file?
      val hash: Long = HashUtil.hashMurmur3(word)
      val fileName: String = s"${hash % Config.WORD_HASH_SIZE}.invert"
      val file: File = new File(Config.STORAGE_PATH, fileName)
      val reader = new BufferedReader(new FileReader(file))

      val item = mutable.Map[Long, ArrayBuffer[Int]]()
      reader.lines()
        .forEach {
          case s"$keyword $docId $position" if keyword == word =>
            val arr: ArrayBuffer[Int] = item.getOrElseUpdate(docId.toLong, ArrayBuffer[Int]())
            arr += position.toInt
          case _ =>
        }

      reader.close()
      sender ! item

    case RearrangeTablesRequest =>
      // rearrange all tables
      rearrangeContentTables()
      rearrangeInvertIndexTables()
      rearrangeIndexTable()
      rearrangeMetaTable()

      // flush index table and meta table
      flushIndexTable()
      flushMetaTable()
      sender ! true
  }


}

