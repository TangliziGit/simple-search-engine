package me.tanglizi.se.entity

object Result {

  case class Token(keyword: String, position: Array[Int]) {
    override def toString: String = s"($keyword, [${position.mkString(", ")}])"
  }

}
