package me.tanglizi.se.util

import scala.util.hashing.MurmurHash3

object HashUtil {
  def hash(content: String): Int = Math.abs(MurmurHash3.stringHash(content))
}
