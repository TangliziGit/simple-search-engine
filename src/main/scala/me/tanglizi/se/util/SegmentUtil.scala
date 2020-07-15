package me.tanglizi.se.util

import java.util

import io.github.yizhiru.thulac4j.Segmenter

object SegmentUtil {

  def getTokens(content: String): Array[String] = {
    Segmenter.enableFilterStopWords()
    val words: util.List[String] = Segmenter.segment(content)
    words.toArray[String](Array.ofDim[String](words.size))
  }

}
