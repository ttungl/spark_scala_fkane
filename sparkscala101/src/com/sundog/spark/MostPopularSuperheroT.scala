package com.sundog.spark

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.log4j._

object MostPopularSuperheroT {
  
  
  def occurrencesCounter(line : String) = {
    var field = line.split("\\s+") // space
    (field(0).toInt, field.length -1)
  }
  
  
  def parsedLines (line : String) : Option[(Int, String)] = {
    var field = line.split('\"') // use single quote.
    if (field.length>1) {
      return Some(field(0).trim().toInt, field(1))
    } else {
      return None
    }    
  }
  
  
  def main(args : Array[String]) {
    
    Logger.getLogger("org").setLevel(Level.ERROR)
    
    val sc = new SparkContext("local[*]", "MostPopularSuperheroT")
    
    val names = sc.textFile("../Marvel-names.txt")
    
    val parsedNames = names.flatMap(parsedLines) // flatmap discards None in results.
    
    val occurrences = sc.textFile("../Marvel-graph.txt")
    
    val occurrencesCount = occurrences.map(occurrencesCounter) // returns heroID and #connections count.
    
    val totalFriendsByCharacter = occurrencesCount.reduceByKey((x,y) => (x+y)) // aggregation count
    
    val flippedKey = totalFriendsByCharacter.map(x => (x._2, x._1))
    
    val sortedFlipKey = flippedKey.sortByKey()
    
    val mostPopular = sortedFlipKey.max() 
    
    val popularHero = parsedNames.lookup(mostPopular._2)(0)
    
    println(s"$popularHero is the most popular superhero with ${mostPopular._1} co-appearances.")
    
    
  }
}