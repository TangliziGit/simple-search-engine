package me.tanglizi.se.util

import org.asynchttpclient.Response

object Protocol {

  // EngineActor
  case class AddRequest(response: Response)
  case class SearchRequest(word: String, cb: Any => Any)                    // TODO: consider cb type

  // TokenizeActor
  case class TokenizeDocumentRequest(id: Long, response: Response)
  case class TokenizeSearchWordRequest(word: String)

  // IndexActor
  case class IndexRequest(id: Long, content: String, words: Array[String])  // TODO: content type should be a case class
  case class IndexSearchRequest(words: Array[String], cb: Any => Any)       // TODO: consider cb type

  // StorageActor
  case class StoreContentRequest(hash: Long, content: String)
  case class FlushIndexRequest()
  case class FlushInvertedIndexRequest()
  case class FindInvertedIndexItemRequest(word: String)

}
