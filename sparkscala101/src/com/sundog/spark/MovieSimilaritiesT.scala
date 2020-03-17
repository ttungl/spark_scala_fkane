package com.sundog.spark

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.log4j._
import scala.io.Source
import java.nio.charset.CodingErrorAction
import scala.io.Codec
import scala.math.sqrt

object MovieSimilaritiesT {
  
  // load movie names to dictionary
  def loadMovieNames() : Map[Int, String] = {

  	// Handle character encoding issues:
    implicit val codec = Codec("UTF-8")
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

    // create a map of ints to strings, and populate it from u.item
    var movieNames : Map[Int, String] = Map()

    val lines = Source.fromFile("../ml-100k/u.item").getLines()
    for (line <- lines) {
    	var fields = line.split('|')
    	if (fields.length > 1) {
    		movieNames += (fields(0).toInt -> fields(1))
    	}
    }
    return movieNames
  }

  type MovieRating = (Int, Double) 	// MovieRate = (MovieID, RatingScore)
  type UserRatingPair = (Int, (MovieRating, MovieRating))	// (UserID, (MovieRate, MovieRate))
  def makePairs (userRatings : UserRatingPair) = {			 
  	val movieRating1 = userRatings._2._1
  	val movieRating2 = userRatings._2._2

  	val movie1 = movieRating1._1
  	val rating1 = movieRating1._2
  	val movie2 = movieRating2._1
  	val rating2 = movieRating2._2

  	((movie1, movie2), (rating1, rating2))
  }

  def filterDuplicates(userRatings : UserRatingPair) : Boolean = {
  	val movieRating1 = userRatings._2._1
  	val movieRating2 = userRatings._2._2

  	val movie1 = movieRating1._1
  	val movie2 = movieRating2._1
  	
  	return movie1 < movie2 

  }

  // Cosine Similarity method
  type RatingPair = (Double, Double) // (score1, score2)
  type RatingPairs = Iterable[RatingPair]

  def computeCosineSimilarity(ratingPairs : RatingPairs) : (Double, Int) = {
  	var numPairs : Int = 0
  	var sum_xx : Double = 0.0
  	var sum_yy: Double = 0.0
  	var sum_xy : Double = 0.0
  	
  	for (pair <- ratingPairs) {
  		val ratingX = pair._1
  		val ratingY = pair._2

  		sum_xx += ratingX * ratingX
  		sum_yy += ratingY * ratingY
  		sum_xy += ratingX * ratingY
  		
  		numPairs += 1
  	}

  	val numerator : Double = sum_xy
  	val denominator = sqrt(sum_xx) * sqrt(sum_yy)
  	var score : Double = 0.0
  	if (denominator != 0) {
  		score = numerator/denominator
  	}

  	return (score, numPairs)

  }


  /** Our main function where the action happens */
  def main(args: Array[String]) {
  	// Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // SparkContext
    val sc = new SparkContext("local[*]", "MovieSimilaritiesT")


    println("\nLoading movies names...")

    val nameDict = loadMovieNames()

    // read data
    val data = sc.textFile("../ml-100k/u.data")

    // map ratings to key-value pairs: userID => (movieID, rating)
    val ratings = data.map(x => x.split("\t")).map(x => (x(0).toInt, (x(1).toInt, x(2).toDouble)))

    // self-join to find all combinations.
    val joinRatings = ratings.join(ratings)
    // At this point, the output joinRatings (RDD) consists of 
    // (userID => ((MovieID, Rating),(MovieID, Rating)))

    // filter out duplicate pairs 
    // filter method only keeps true value in the new collection.
    val uniqueJoinedPairs = joinRatings.filter(filterDuplicates) 

    // make key by (movie1, movie2) pairs
    val moviePairs = uniqueJoinedPairs.map(makePairs)

    // At this point, we have (movie1, movie2) => (rating1, rating2)
    // collect all ratings from each movie pair as a key 
    val aggMoviePairRatings = moviePairs.groupByKey()



    // At this point, we have the aggregated level of (movie1, movie2) as a key and 
    // values (rating1, rating2), (rating1, rating2), ...

    // Then compute similarity, and caching the results for later use.
    val moviePairSimilarities = aggMoviePairRatings.mapValues(computeCosineSimilarity).cache()

    

    // At this point, the moviePairSimilarities have
    // ((movie1, movie2), (rating1, rating2))

    // save the results if needed.
    // val sorted = moviePairSimilarities.sortByKey()
    // sorted.saveAsTextFile("movie-sims-T")

    // Extract similarity for the movies we care about that are "good".

    if (args.length > 0) {
    	val scoreThreshold = 0.97
    	val coOccurrenceThreshold = 50.0

    	val movieID : Int = args(0).toInt

    	val filteredResults = moviePairSimilarities.filter(x => 
    		{
    			val pair = x._1 // movies pair
    			val sim = x._2 // ratings pair 
    			(pair._1 == movieID || pair._2 == movieID) && sim._1 > scoreThreshold && sim._2 > coOccurrenceThreshold
    		})

    	// sort by quality score 
    	val results = filteredResults.map(x => (x._2, x._1)).sortByKey(false).take(10)

    	println("\nTop 10 similar movies for " + nameDict(movieID))

    	for (res <- results) {
    		val similar = res._1
    		val pair = res._2

    		var similarMovieID = pair._1

    		if (similarMovieID == movieID) {
    			similarMovieID = pair._2
    		}
    		println(nameDict(similarMovieID) + "\tscore: " + similar._1 + "\tstrength: " + similar._2)
    	}

    }
  
  }
}