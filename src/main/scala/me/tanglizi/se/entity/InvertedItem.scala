package me.tanglizi.se.entity

import scala.collection.mutable.ArrayBuffer

case class InvertedItem(indices: ArrayBuffer[(Long, Int)], positions: ArrayBuffer[Array[Int]]) {
  override def toString: String =
    s"InvertedItem(${indices.mkString(", ")}, ${positions.map(xs => xs.mkString(", "))})"
}
