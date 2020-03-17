package com.sundog.spark

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.log4j._

/** Find the movies with the most ratings. */
object PopularMoviesT {
 
  /** Our main function where the action happens */
  def main(args: Array[String]) {
      
     Logger.getLogger("org").setLevel(Level.ERROR)
     
     // use all local resources
     val sc = new SparkContext("local[*]","PopularMoviesT")
     
     // read all data
     val lines = sc.textFile("../ml-100k/u.data")
     
     // movieID, count by 1
     val rdd = lines.map(x => (x.split("\t")(1).toInt, 1))
     
     // aggregated sum by movieID (key)
     val rdd1 = rdd.reduceByKey((x,y) => (x+y))
     
     // flipped (count, movieID)
     val rdd1f = rdd1.map(x => (x._2, x._1))
     
     // sorted by key (count)
     val rdd1fs = rdd1f.sortByKey(false)
     
     // collect action: retrieve all elements computed and assigned to output.
     val results = rdd1fs.collect()
     
//     results.foreach(println)
     for (res <- results) {
       val cnt = res._1
       val mID = res._2
       println(s"movieID $mID has $cnt times watched.")
       
     }
    
  }
  
}

