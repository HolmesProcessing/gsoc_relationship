package com.holmesprocessing.relationships.knowledgeBase

import com.holmesprocessing.relationships.models._

import play.api.libs.json._
import play.api.libs.json.Json
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object HelperMethods {

  //the gzip de/compression methods were adapted from https://gist.github.com/owainlewis/1e7d1e68a6818ee4d50e
  def decompress(x: Array[Byte]) : String = {
    val inputStream = new GZIPInputStream(new ByteArrayInputStream(x))
    val output = scala.io.Source.fromInputStream(inputStream).mkString
    return output
  }

  def compress(input: Array[Byte]): Array[Byte] = {
    val bos = new ByteArrayOutputStream(input.length)
    val gzip = new GZIPOutputStream(bos)
    gzip.write(input)
    gzip.close()
    val compressed = bos.toByteArray
    bos.close()
    compressed
  }
 /////////////////////////////////////////////////////////////

  def get_digitalsig(result: String) : String = {

    val x= (Json.parse(result) \ "version_info" \\ "value").map(_.toString)
    val y = (Json.parse(result) \ "version_info" \\ "key").map(_.toString)
    val sig = y.zip(x).filter(x=>(x._1).contains("Signature")).map(_._2)

    if (sig.isEmpty) {
      return "NONE"
    }
    return sig.head.replaceAll("""["]""","")
  }

  def get_cuckoo_urls(result: String) : String = {

    val domains = (Json.parse(result) \\ "Data").map(x=> (x \ "arguments" \ "url")).filter(y => y.isDefined).map(_.as[String]).mkString(",")
    return domains
  }

  def score(ruleset_1: String, ruleset_2:String) : Double = {

    val split_1 = ruleset_1.split(",").toSeq
    val split_2 = ruleset_2.split(",").toSeq
    if (split_1.length > 0 && split_2.length > 0) {
      return split_1.intersect(split_2).length.toDouble/split_1.union(split_2).length.toDouble
    } else {
      return 0
    }
  }

}
