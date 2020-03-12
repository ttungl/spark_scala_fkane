package com.sundog.spark

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.log4j._
import scala.math.max

object MaxTemperatures {
  
  def parsedLine(line:String) = {
    val fields = line.split(",")
    val stationID = fields(0)
    val entryType = fields(2) 
    val temperature = fields(3).toFloat * 0.1f * (9.0f / 5.0f) + 32.0f
    (stationID, entryType, temperature)    
  }
  
  
  def main(args:Array[String]){
    Logger.getLogger("org").setLevel(Level.ERROR)
    
    // create SparkContext using every cores in the local machine
    val sc = new SparkContext("local[*]", "MaxTemperatures")
    
    // read data
    val lines = sc.textFile("../1800.csv")
    
    // convert data to tuples
    val parsedLines = lines.map(parsedLine)
    
    // filter out entryType=TMAX
    val maxTemps = parsedLines.filter(x => x._2=="TMAX")
    
    // convert to (StaionID, temps)
    val stationTemps = maxTemps.map(x => (x._1, x._3.toFloat))
    
    // aggregated data by stationID
    val aggStationTemps = stationTemps.reduceByKey((x,y) => max(x,y))
    
    // collect action
    val result = aggStationTemps.collect()
    
    // display
    for (res <- result.sorted) {
      val station_ID = res._1
      val temp = res._2
      val temp_val = f" $temp%.2f F"
      println(s"$station_ID has maximum temperature: $temp_val")
      
      
    }
    
    
    
    
    
  }
  
  
  
  
}