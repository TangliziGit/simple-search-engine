package me.tanglizi.se.web.model

import me.tanglizi.se.entity.{Document, DocumentInfo}

import scala.beans.BeanProperty

case class SearchResult(@BeanProperty result: Array[DocumentInfo],
                        @BeanProperty tokens: Array[String])
