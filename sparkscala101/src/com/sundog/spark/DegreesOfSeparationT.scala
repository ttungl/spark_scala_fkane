package com.sundog.spark

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._
import org.apache.spark.util.LongAccumulator
import org.apache.log4j._
import scala.collection.mutable.ArrayBuffer


object DegreesOfSeparationT {
  
	// global variables
	val startCharID = 5306 	// SpiderMan
	val targetCharID = 14 	// Adam 3,031

	// make a global accumulator Option 
	// so we can reference it in a mapper 
	var hitCounter1 : Option[LongAccumulator] = None

	// create BFS data structures 
	type BFSData = (Array[Int], Int, String) // [charID, distance, color]
	type BFSNode = (Int, BFSData) // [charID, its associated structures]


  //// Helper methods
  def convertToBFSHelper(line : String) : BFSNode = {
		// input: raw line of data 
		// desc.: convert line into BFSNode structure, return it.
		// output: BFSNode structure 

		// Split line into fields
		val fields = line.split("\\s+")

		// get charID/heroID
		val charID = fields(0).toInt

		// get subsequence values into an array.
		// create a mutable variable.
		// add up all subsequence values
		var connections : ArrayBuffer[Int] = ArrayBuffer() // name : datatype = method from class.
		for (connection <- 1 to (fields.length - 1 )) { 
			connections += fields(connection).toInt
		}

		// Initial variables
		var color : String = "WHITE"
		var distance : Int = 9999

		if (charID == startCharID) { // if charID we're starting from.
			color = "GRAY"
			distance = 0
		}

		return (charID, (connections.toArray, distance, color))

  }

	// create Rdd to BFS structures from raw lines
  def createRdd(sc:SparkContext): RDD[BFSNode] = {
    val inputFile = sc.textFile("../marvel-graph.txt")
    return inputFile.map(convertToBFSHelper)
  }

	// the Mapper
  def bfsMap(node : BFSNode) : Array[BFSNode] = { // expend a node into this node and its children.
  	// extract data from BFSNode
  	val charID : Int = node._1
  	val data : BFSData = node._2

  	val connections : Array[Int] = data._1
  	val distance : Int = data._2
  	var color : String = data._3

  	// init return structure
  	var results : ArrayBuffer[BFSNode] = ArrayBuffer()

  	// If gray nodes are flagged, create new gray nodes for each connection, 
  	// and color BLACK to them 
  	if (color == "GRAY") {
  		for (connection <- connections) {
  			val newCharID = connection
  			val newDistance = distance + 1
  			val newColor = "GRAY" // node is being processed.

  			if (targetCharID == connection) {
  				if (hitCounter1.isDefined) {
  					hitCounter1.get.add(1) // increment the global accumulator hitCounter to let the driver script knows.
  				}
  		}

  		// create new gray nodes for each connection.
  		val newEntry : BFSNode = (newCharID, (Array(), newDistance, newColor))
  		results += newEntry
  		}
  		color = "BLACK" // node as black indicating it has been processed.
  	}

  	// update data structure
  	val thisEntry : BFSNode = (charID, (connections, distance, color))
  	results += thisEntry

  	// return results
  	return results.toArray

  }  

  // the Reducer
  def bfsReduce(data1 : BFSData, data2 : BFSData) : BFSData = {
  	// extract data that we're combining
  	val edges1 : Array[Int] = data1._1
  	val edges2 : Array[Int] = data2._1

  	val distance1 : Int = data1._2
  	val distance2 : Int = data2._2

  	val color1 : String = data1._3
  	val color2 : String = data2._3


  	// init nodes values
  	var distance : Int = 9999
  	var color : String = "WHITE"
  	var edges : ArrayBuffer[Int] = ArrayBuffer()


  	// preserving node if original
  	if (edges1.length > 0) {
  		edges ++= edges1
  	}
  	if (edges2.length > 0) {
  		edges ++= edges2
  	}

  	// preserving minimum distance
  	if (distance1 < distance2) {
  		distance = distance1
  	}
  	if (distance1 > distance2) {
  		distance = distance2
  	} 


  	// preserving darkest color 
  	if (color1 == "WHITE" && (color2 == "GRAY" || color2 == "BLACK")) {
  		color = color2
  	}	
  	if (color1 == "GRAY" && color2 == "BLACK") {
  		color = color2
  	}
  	if (color2 == "WHITE" && (color1 == "GRAY" || color1 == "BLACK")) {
  		color = color1 
  	}
  	if (color2 == "GRAY" && color1 == "BLACK") {
  		color = color1
  	}
  	if (color1 == "GRAY" && color2 == "GRAY") {
  		color = color1
  	}
  	if (color1 == "BLACK" && color2 == "BLACK") {
  		color = color1
  	}

  	return (edges.toArray, distance, color)

  }

  // main
  def main(args : Array[String]) {
    
  	// Logger
  	Logger.getLogger("org").setLevel(Level.ERROR)

  	// create a SparkContext using every cores in the local machine
  	val sc = new SparkContext("local[*]", "DegreesOfSeparationT")

  	// use an accumulator to signal if the target is hit
  	hitCounter1 = Some(sc.longAccumulator("Hit Counter"))

  	var iterationRdd = createRdd(sc)
  		
  	var iteration : Int = 0 // name : datatype = value

  	for (iteration <- 1 to 10) {
  		println(s"Running BFS iteration # $iteration")

  		// The Mapper
  		// create new vertices as needed to darken or reduce distances in the reduce stage.
  		// if we hit the target node as a gray node, increment our accumulator to signal we're done.
  		val mapped = iterationRdd.flatMap(bfsMap)

  		// use mapped.count() action to force the RDD to be evaluated.
  		// this is the reason our accumulator is actually updated.
  		println("Processing " + mapped.count() + " values.")

  		// check hit Counter if we're done.
  		if (hitCounter1.isDefined) {
  			val hitCount = hitCounter1.get.value 

  			if (hitCount > 0) { // hit 
  				println("The target is hit from " + hitCount + " different directions.")
  				return	
  			} 
  		} 
  		// The Reducer
  		// combines data for each character ID
  		// preserving/maintaining the darkest color and shortest path.
  		iterationRdd = mapped.reduceByKey(bfsReduce)

  	}
  }
}