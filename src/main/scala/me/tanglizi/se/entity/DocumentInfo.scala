package me.tanglizi.se.entity

import scala.beans.BeanProperty

case class DocumentInfo(@BeanProperty title: String, @BeanProperty url: String, @BeanProperty content: String) {
  override def toString: String = s"$title, $url, ${content.slice(0, 20)}"
}
