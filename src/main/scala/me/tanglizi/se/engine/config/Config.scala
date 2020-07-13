package me.tanglizi.se.engine.config

import akka.util.Timeout

import scala.concurrent.duration._

object Config {
  val STORAGE_PATH: String = "/home/tanglizi/tmp/se"
  val INDEX_TABLE_FILE_NAME: String = "indexTable.data"
  val META_TABLE_FILE_NAME: String = "metaTable.data"

  val DEFAULT_AKKA_TIMEOUT: Timeout = Timeout(120.seconds)
  val DEFAULT_AWAIT_TIMEOUT: FiniteDuration = 120.seconds

  val INVERTED_INDEX_TABLE_FLUSH_SIZE: Long = 50
  val INDEX_TABLE_FLUSH_FREQ: Long = 50
  val META_TABLE_FLUSH_FREQ: Long = 50

  val DOCUMENT_BM25_K: Double = 2
  val DOCUMENT_BM25_B: Double = 0.75
}
