package com.sundog.spark

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.log4j._
import scala.math.max
import scala.math.min

object MostPrecipitationDay {
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
    
    // filter out entryType=PRCP
    val prcpTemps = parsedLines.filter(x => x._2=="PRCP")
    
    // convert to (StaionID, temps)
    val stationTemps = prcpTemps.map(x => (x._1, x._3.toFloat))
    
    // aggregated data by stationID
    val aggStationTempsMax = stationTemps.reduceByKey((x,y) => max(x,y))
    val aggStationTempsMin = stationTemps.reduceByKey((x,y) => min(x,y))
    
    // collect action
    val result_max = aggStationTempsMax.collect()
    val result_min = aggStationTempsMin.collect()
    
    // display
    for ((resmax, resmin) <- (result_max.sorted zip result_min.sorted)) {
      val station_IDmax = resmax._1
      val tempmax = resmax._2
      val tempmax_val = f" $tempmax%.2f F"
      println(s"$station_IDmax has the max precipitation temperature: $tempmax_val")
      
      val station_IDmin = resmin._1
      val tempmin = resmin._2
      val tempmin_val = f" $tempmin%.2f F"
      println(s"$station_IDmin has the min precipitation temperature: $tempmin_val")
      
      
    }
    
    
    
    
    
  }
}