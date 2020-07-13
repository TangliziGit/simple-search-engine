package me.tanglizi.se.entity

import me.tanglizi.se.entity.Result.Token
import org.asynchttpclient.Response

object Protocol {

  // EngineActor
  case class AddRequest(response: Response)
  case class SearchRequest(word: String, cb: List[Document] => Any)                    // TODO: consider cb type

  // TokenizeActor
  case class TokenizeDocumentRequest(id: Long, response: Response)
  case class TokenizeSearchWordRequest(word: String)

  // IndexActor
  case class IndexRequest(id: Long, content: String, words: Array[Token])   // TODO: content should contains content, url and title
  case class IndexSearchRequest(words: Array[String], cb: List[Document] => Any)       // TODO: consider cb type

  // StorageActor
  case class StoreContentRequest(hash: Long, content: String)
  case class FlushIndexRequest()
  case class LoadIndexRequest()
  case class FlushInvertedIndexRequest()
  case class FindInvertedIndexItemRequest(word: String)

}
