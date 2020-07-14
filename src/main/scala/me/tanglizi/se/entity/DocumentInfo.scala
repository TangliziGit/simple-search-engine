package me.tanglizi.se.entity

case class DocumentInfo(title: String, url: String, content: String) {
  override def toString: String = s"$title, $url, ${content.slice(0, 20)}"
}
