package com.sundog.spark

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.log4j._
import scala.math.min
import scala.math.max

/** Compute the total amount spent per customer in some fake e-commerce data. */
object TotalSpentByCustomerMinMax {
  
  /** Convert input data to (customerID, amountSpent) tuples */
  def extractCustomerPricePairs(line: String) = {
    val fields = line.split(",")
    (fields(0).toInt, fields(2).toFloat)
  }
 
  /** Our main function where the action happens */
  def main(args: Array[String]) {
   
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)
    
     // Create a SparkContext using every core of the local machine
    val sc = new SparkContext("local[*]", "TotalSpentByCustomer")   
    
    val input = sc.textFile("../customer-orders.csv")

    val mappedInput = input.map(extractCustomerPricePairs)
    
    val totalByCustomer = mappedInput.reduceByKey( (x,y) => x + y )
    
    // sorted decreasing order amount
    val totalByCustomer_flipped = totalByCustomer.map(x => (x._2, x._1))
    
    val minSpender = totalByCustomer_flipped.reduceByKey((x,y) => min(x,y))
    val maxSpender = totalByCustomer_flipped.reduceByKey((x,y) => max(x,y))
    
    
    val minResults = minSpender.collect()
    val maxResults = maxSpender.collect()
    
    val minval = minResults.min 
    val maxval = maxResults.max
    
    
    val cid = minval._2
    val amt = minval._1
    val camount = f"$amt%.2f F"
    println(s"customer ID $cid min spent amount $camount")
    
    val cid1 = maxval._2
    val amt1 = maxval._1
    val camount1 = f"$amt1%.2f F"
    println(s"customer ID $cid1 max spent amount $camount1")
    
  
    
  }
  
}

