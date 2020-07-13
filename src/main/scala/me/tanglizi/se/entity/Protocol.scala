package me.tanglizi.se.entity

import me.tanglizi.se.entity.Result.Token
import org.asynchttpclient.Response

object Protocol {

  // EngineActor
  case class AddRequest(response: Response)
  case class SearchRequest(word: String, cb: List[Document] => Any)

  // TokenizeActor
  case class TokenizeDocumentRequest(id: Long, response: Response)
  case class TokenizeSearchWordRequest(word: String)

  // IndexActor
  case class IndexRequest(id: Long, documentInfo: DocumentInfo, words: Array[Token])
  case class IndexSearchRequest(words: Array[String], cb: List[Document] => Any)

  // StorageActor
  case class StoreDocumentRequest(hash: Long, documentInfo: DocumentInfo)
  case class FindDocumentRequest(documentId: Long)
  case class FlushMetaRequest()
  case class LoadMetaRequest()
  case class FlushIndexRequest()
  case class LoadIndexRequest()
  case class FlushInvertedIndexRequest()
  case class FindInvertedIndexItemRequest(word: String)

}
