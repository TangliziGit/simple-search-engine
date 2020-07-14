package me.tanglizi.se.util

import org.apache.commons.codec.digest.DigestUtils

import scala.util.hashing.MurmurHash3

object HashUtil {
  def hashMurmur3(content: String): Int = Math.abs(MurmurHash3.stringHash(content))
  def hashSHA256(content: String): String = DigestUtils.sha256Hex(content)
}
