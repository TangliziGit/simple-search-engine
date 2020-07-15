package me.tanglizi.se.entity

import me.tanglizi.se.entity.Result.Token
import org.asynchttpclient.Response

object Protocol {

  // SearchEngineActorSystem
  // EngineActor
  case class AddRequest(response: Response)
  case class SearchRequest(word: String)
  case class AsyncSearchRequest(word: String, cb: List[Document] => Any)
  case class DeleteRequest(documentUrl: String)

  // TokenizeActor
  case class TokenizeDocumentRequest(id: Long, response: Response)
  case class TokenizeSearchWordRequest(word: String)

  // IndexActor
  case class IndexRequest(id: Long, documentInfo: DocumentInfo, words: Array[Token])
  case class IndexSearchRequest(words: Array[String], cb: List[Document] => Any, isDescribed: Boolean = true)

  // StorageActor
  case class StoreDocumentRequest(hash: Long, documentInfo: DocumentInfo)
  case class FindDocumentRequest(documentId: Long)
  case class FlushMetaRequest()
  case class LoadMetaRequest()
  case class FlushIndexRequest()
  case class LoadIndexRequest()
  case class FlushInvertedIndexRequest()
  case class FindInvertedIndexItemRequest(word: String)
  case class RearrangeTablesRequest()

  // CrawlActorSystem
  // CrawlActor
  case class CrawlRequest(url: String)

  // DispatchActor
  case class EnqueueCrawlRequest(urls: Array[String])

}
