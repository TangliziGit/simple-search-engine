package me.tanglizi.se.entity

import me.tanglizi.se.engine.Engine
import me.tanglizi.se.engine.config.Config

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Document(val documentId: Long,
               val keywordsMap: Map[String, List[Int]],
               val keywordCount: Long) {
  var BM25: Double = 0
  var documentInfo: DocumentInfo = DocumentInfo("", "", "")

  def IDF(documentCountOfKeyword: Int): Double =
    math.log(Engine.totalDocumentCount.get() / documentCountOfKeyword + 1) / math.log(2)

  def TF(keyword: String): Double =
    keywordsMap(keyword).size / keywordCount.toDouble

  def calculateBM25(keywords: Array[String], documentCountsOfKeyword: List[Int]): Double = {
    val Array(k, b) = Array(Config.DOCUMENT_BM25_K, Config.DOCUMENT_BM25_B)
    var sum: Double = 0

    for ((keyword, documentCountOfKeyword) <- keywords.zip(documentCountsOfKeyword)
         if keywordsMap contains keyword) {
      val tf: Double = TF(keyword)
      val totalWordCount: Long = Engine.totalWordCount.get()
      val totalDocumentCount: Long = Engine.totalDocumentCount.get()

      val up: Double = IDF(documentCountOfKeyword) * tf * (1 + k)
      val low: Double = tf + k * (1 - b + b * keywordCount / ( totalWordCount / totalDocumentCount))
      sum += up / low
    }
    BM25 = sum
    BM25
  }

  def setInformation(title: String, url: String, content: String): Unit =
    this.documentInfo = DocumentInfo(title, url, content)

  def setInformation(documentInfo: DocumentInfo): Unit =
    this.documentInfo = documentInfo

  override def toString: String =
    s"Document[$documentId, $documentInfo, ($BM25)]"
}

object Document {
  def fromDs(keywordPositionsMaps: List[mutable.Map[Long, ArrayBuffer[Int]]], keywords: Array[String]): List[Document] = {
    val documentIdToWordsMap = mutable.Map[Long, mutable.Map[String, List[Int]]]()
    for ((documentIdToWordPositions, keyword) <- keywordPositionsMaps.zip(keywords)) {
      for ((documentId, positions) <- documentIdToWordPositions) {
        val wordsMap = documentIdToWordsMap.getOrElseUpdate(documentId, mutable.Map[String, List[Int]]())
        wordsMap.put(keyword, positions.toList)
      }
    }

    val documents = for ((documentId, wordsMap) <- documentIdToWordsMap) yield {
      val document = new Document(documentId, wordsMap.toMap, Engine.wordCountInDocument(documentId))
      document.calculateBM25(keywords, keywordPositionsMaps.map(_.size))
      document
    }

    documents.toList
  }
}
