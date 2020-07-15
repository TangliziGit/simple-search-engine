package me.tanglizi.se.config

import akka.util.Timeout

import scala.concurrent.duration._

object Config {
  val CRLF: String = System.getProperty("line.separator")
  val STORAGE_PATH: String = "/home/tanglizi/tmp/se"
  val INDEX_TABLE_FILE_NAME: String = "indexTable.data"
  val META_TABLE_FILE_NAME: String = "metaTable.data"
  val URL_SET: String = "urls.data"

  val DEFAULT_AKKA_TIMEOUT: Timeout = Timeout(120.seconds)
  val DEFAULT_AWAIT_TIMEOUT: FiniteDuration = 120.seconds

  val INVERTED_INDEX_TABLE_FLUSH_SIZE: Long = 50
  val INDEX_TABLE_FLUSH_FREQ: Long = 50
  val META_TABLE_FLUSH_FREQ: Long = 50
  val MAX_DELETED_DOCUMENTS_SIZE: Long = 50
  val MAX_URL_SET_SIZE: Long = 50

  val DOCUMENT_BM25_K: Double = 2
  val DOCUMENT_BM25_B: Double = 0.75

  val CONTENT_SPLITTER = "{{SPLIT}}"
  val CONTENT_HASH_SIZE = 5
  val WORD_HASH_SIZE = 5

  val KEYWORD_INTERVAL_IN_DESCRIPTION = 40
  val MAX_DESCRIPTION_LENGTH = 100
}
