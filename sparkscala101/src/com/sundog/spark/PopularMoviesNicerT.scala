package com.sundog.spark

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.log4j._
import scala.io.Source
import java.nio.charset.CodingErrorAction
import scala.io.Codec

object PopularMoviesNicerT {
  
  def readMovieNames() : Map[Int, String] = {
    
    // handles codec issues
    implicit val codec = Codec("UTF-8")
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)
    
    // create a map ID String and populate it from u.item
    var movienames : Map[Int, String] = Map()
    val lines = Source.fromFile("../ml-100k/u.item").getLines()

    for (line <- lines) {
      var splitted = line.split('|') // Notes: using single quote works. Double quotes doesn't work.
      if (splitted.length > 1) {
        movienames += (splitted(0).toInt -> splitted(1)) // mapping (ID -> movie name)
        
      }
    }
    
    return movienames
    
  }
  
  
  def main(args : Array[String]) {
    
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)
    
    val sc = new SparkContext("local[*]", "PopularMoviesNicerT") // init SparkContext
    
    var nameDict = sc.broadcast(readMovieNames) // create a broadcast variable to our id -> movies title mapping
    
    val lines = sc.textFile("../ml-100k/u.data") // read data
    
    val mapLines = lines.map(x => (x.split("\t")(1).toInt, 1)) // map movie ID to 1 
    
    val aggMapLines = mapLines.reduceByKey((x,y) => (x+y)) // aggregation count on movie ID
    
    val flippedKey = aggMapLines.map(x => (x._2, x._1)) // (key,val) ~ (count, movie ID)
    
    val sortedFlipKey = flippedKey.sortByKey() // sort key(count) in decreasing order 
    
    val sortedMovieNamesCount = sortedFlipKey.map(x => (nameDict.value(x._2), x._1)) // lookup movie names from broadcast var
    
    val results = sortedMovieNamesCount.collect() // collect the results
    
    results.foreach(println)
 
        
  }
  
}