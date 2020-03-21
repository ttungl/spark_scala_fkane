# Spark Scala Typing Notes

### Section 2. Scala scratch:
	+ MapReduce:
		+ map() can be used to apply any function to every item in a collection.
		+ reduce() can be used to combine together all the items in a collection using some function.
		+ filter() can remove stuff you don't need.

### Section 3. Spark Basics
	+ Built based on Scala.
	+ Resilient Distributed Dataset (RDD): encapsulates very large dataset, then apply transformations and actions to perform this data. Using sc.parallelize(data) method to create an RDD.
		+ RDD Transformations: 
			+ map: apply the function to each element of the whole RDD. 
			+ flatmap: apply the function to each elements of the RDD as in map transformation. The difference is that flatmap will return multiple values for each element in the input RDD. 
			+ filter: return a new RDD with only the elements that pass the filter condition.
			+ distinct: return distinct rows if you have duplicate rows.
			+ sample: sampling for testing
			+ union, intersection, subtract, cartesian
			
		+ RDD Actions:
			+ collect: retrieve all elements of the RDD computed and it is assigned to the output.
			+ count
			+ countByValue: unique id.
			+ take
			+ top
			+ reduce: you want to add/multiply all the values for a given key together.
			* Notes: Nothing actually happens in your driver program until an action is called. 


	+ Spark internals:
		+ stage 1: textFile() + map() function
		+ stage 2: transformation functions
		+ final stage: the tasks are scheduled across the clusters and executed.

	+ Key/value in RDD:
		+ res = rdd.map(x=>(x, 1))
		+ using functions: 
			+ reduceByKey(): combine values with the same key using some function.
				+ rdd.reduceByKey((x,y)=>x+y) --> adds them up. 
			+ groupByKey(): group values with the same key.
			+ sortByKey(): sort RDD by key values.
			+ keys(), values(): create an RDD of just the keys or values.
			+ join, rightOuterJoin, leftOuterJoin, cogroup, subtractByKey.
			+ With key/value data, use `mapValues()` and `flatMapValues()` if your transformation doesn't affect the keys.

	+ Filtering RDD: to remove out the data you don't need.
	+ Map vs FlatMap:
		+ Map(): transforms each element of an RDD into one new element.
		+ Flatmap(): can create many new elements from each one.

	+ countByValue() the "hard way": 
		+ rdd.map(x => (x, 1)).reduceByKey((x, y) => x+y)

	+ Problem 1: Total amount spent by customer
		+ Strategy:
			+ split each comma-delimited line into fields.
			+ map each line to key/value pair of customer ID and dollar amount.
			+ use reduceByKey to add up amount spent by customer ID
			+ collect() the results and print them out.

### Section 4: Advanced Spark Examples
	+ Most popular movies.
	+ Use Broadcast variables to display movie names.
		+ Broadcast var: takeing a chunk of data and explicitly sending it to all of the nodes in our clusters ready for whenever it needs it. 
			+ Broadcast objects to the executors, they're always there whenever needed. 
			+ sc.broadcast() to ship off whatever you want.
			+ sc.value() to get the object back.

	+ Find the most popular superhero in a social graph.

	+ Superhero degrees of separation.
		+ Iterative BFS implementation in Spark 
			+ A BFS iteration as a map and reduce job
				+ The mapper: 
					+ creates new nodes for each connection of gray nodes, with a distance incremented by one, color gray, and no connections.
					+ color the gray node we just processed black.
					+ copies the node itself into the results.
				+ The reducer:
					+ combines together all nodes for the same hero ID
					+ preserves the shortest distance and the darkest color found
					+ preserves the list of connections from the original node.
			+ How do we know when we're done? 
				+ An accumulator allows many executors to increment a shared value.
					+ For example:
						```var hitCounter : LongAccumulator("Hit Counter")```
						+ This sets up a shared accumulator named "Hit Counter" with an initial value of 0.
						+ for each iteration, if the character we're interested in is hit, we increment the hitCounter accumulator.
						+ after each iteration, we check if hitCounter is greater than one - if so, we're done.

		+ Iteratively process the RDD
			+ Go through looking for gray nodes to expand.
			+ Color nodes with black.
			+ Update the distances as we go.

		+ Introducing accumulators.
			+ Like a global variable, its update will be signaled to the driver program.

	+ Item-based collaborative filtering in spark, cache(), persist()
		+ Finding similar movies using Spark with MovieLens dataset.
		+ Introducing caching RDDs' (when using RDD's results more than one)
		+ Solution:
			+ Find every pair of movies that were watched by the same person.
			+ Measure the similarity of their ratings across all users who watched both.
			+ Sort by movie, then by similarity strength.
		+ Spark Approach:
			+ Map input ratings to (userID, (movieID, rating))
			+ Find every movie pair rated by the same user.
				+ This can be done with a "self-join" operation.
				+ At this point, we have (userID, ((movieID1, rating1), (movieID2, rating2)))
			+ Filter out duplicate pairs
			+ Make movie pairs the key
				+ map to ((movieID1,movieID2) -> (rating1, rating2))
			+ groupByKey() to get every rating pair found for each movie pair.
			+ compute similarity between ratings for each movie in the pair
			+ sort, save, and display results.

			+ Caching Rdd's:
				+ we'll query the final RDD of movie similarities a couple of times.
				+ any time you will perform more than one action on an RDD, must cache it.
					- otherwise, Spark might re-evaluate the entire RDD all over again.
				+ use .cache() or persist() to do this.
					+ Difference?
						+ Persist() optionally lets you cache it to disk instead of memory, just in case a node fails or something so.

	+ Running similar movies script using Spark's cluster
		+ create a jar file.
		+ Notes: Number 50 is movieID for StarWar. We will find the MovieSimilarities to StarWar movie. 
		+  ~/$ spark-submit --class com.sundog.spark.MovieSimilarities MovieSimilarities.jar 50
			20/03/15 18:08:19 WARN NativeCodeLoader: Unable to load native-hadoop library for your platform... using builtin-java classes where applicable

			Loading movie names...
			Top 10 similar movies for Star Wars (1977)
				Empire Strikes Back, The (1980)	score: 0.9895522078385338	strength: 345
				Return of the Jedi (1983)	score: 0.9857230861253026	strength: 480
				Raiders of the Lost Ark (1981)	score: 0.981760098872619	strength: 380
				20,000 Leagues Under the Sea (1954)	score: 0.9789385605497993	strength: 68
				12 Angry Men (1957)	score: 0.9776576120448436	strength: 109
				Close Shave, A (1995)	score: 0.9775948291054827	strength: 92
				African Queen, The (1951)	score: 0.9764692222674887	strength: 138
				Sting, The (1973)	score: 0.9751512937740359	strength: 204
				Wrong Trousers, The (1993)	score: 0.9748681355460885	strength: 103
				Wallace & Gromit: The Best of Aardman Animation (1996)	score: 0.9741816128302572	strength: 58


	+ Improve the quality of similar movies.
		+ Try different similarity metrics
			+ Pearson correlation coefficient [implemented]
				+ use scoreThreshold=0.8; coOccurrenceThreshold=5.0
				+ spark-submit --class com.sundog.spark.MovieSimilaritiesPearson MovieSimilaritiesPearson.jar 50
					20/03/16 23:23:46 WARN NativeCodeLoader: Unable to load native-hadoop library for your platform... using builtin-java classes where applicable

					Loading movies names...
					Top 10 similar movies for Star Wars (1977)
					Man of the Year (1995)	score: 0.9999999999999959	strength: 7
					Love in the Afternoon (1957)	score: 0.9233805168766379	strength: 9
					Dream With the Fishes (1997)	score: 0.8040844011283466	strength: 6

			+ Jaccard coefficient 
			+ Conditional Probability

		+ Adjust the thresholds for minimum co-rates or minimum score.
		+ Invent a new similarity metric that takes the number of co-raters into account.
		+ Use genre information in u.items to boost scores from movies in the same genre.

### Section 5: Running Spark on a cluster
	+ Package all the objects into one JAR file for deployment on the cluster.
		+ Make sure there are no paths to your local filesystem used in your script. That's what HDFS, S3, etc. are for
		+ Package up your scala project into a JAR file (using Export in the IDE)
		+ Use spark-submit to execute your driver script outside of the IDE
			+ spark-submit --class <class object contains your main function>
							--jars <paths to any dependencies>
							--files <files you want placed alongside your application>
							<your JAR file>

	+ Packaging with SBT
		+ Concept:
			+ like Maven for Scala
			+ manages your lib dependency tree for you
			+ can package up all of your dependencies into self-contained JAR.
			+ if you have many dependencies, it makes life a lot easier than passing a ton of jar options.
			+ get it from scala-sbt.org
		+ Using SBT:
			+ src -> main -> scala.
			+ your scala source files go in the source folder.
			+ in your project folder, create an assembly.sbt file that contains one line:
				```addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")```
			+ check the latest sbt-assembly documentation as this will change overtime.
			+ At the root (along side the src and project directories) create a build.sbt file.
				
			+ Example:
				```
				name := "PopularMovies"
				version := "1.0"
				organization := "com.sundog"
				scalaVersion := "2.13.1"
				libraryDependencies ++= Seq(
					"org.apache.spark" %% "spark-core" % "3.0.0" % "provided"
				)
				```
		+ Adding dependencies:
			+ Say, from the example, you want to depend on Kafka, which isn't built into Spark. You could add:
				```
				"org.apache.spark" %% "spark-streaming-kafka" % "1.6.1"
				``` 
				to your lib dependencies, and sbt will automatically fetch it and everything it needs and bundle it into your JAR file.
			+ Make sure you use the correct Spark version number, and note that did NOT use "provided" on the line. 

		+ Then just use: 
			```
			spark-submit <JAR file> 
			```
			and it'll run, even without specifying a class.

		+ Run "sbt assembly" to generate the JAR file.

	+ Intro Amazon Elastic MapReduce (EMR)
		+ Distributed Spark:
			+ Spark Driver -> Cluster Manager -> Cluster Worker/Executors
											 |-> Cluster Worker/Executors
											 |-> Cluster Worker/Executors


		+ Other spark-submit parameters:
			```--master```
				+ yarn - for running a YARN/Hadoop cluster
				+ hostname:port - for connecting to a master on a spark standalone cluster.
				+ mesos://masternode:port
				+ A master in your sparkConf will override this.
			```--num-executors```
				+ must set explicitly with YARN, only 2 by default.
			```--executor-memory```
				+ make sure you don't try to use more memory than you have.
			```--total-executor-cores```
		+ Amazon Elastic MapReduce (EMR):
			+ a quick way to create a cluster with Spark, Hadoop, and YARN pre-installed.

		+ Setup on EMR:
			+ Create an AWS account.
			+ Create an EC2 keypair and download .pem file.
			+ chmod 400 filename.pem
			+ Make sure you set ssh port 22 for 0.0.0.0/0 in security group on master cluster.
			+ ```ssh -i filename.pem <ssh_link>```
		+ copy files from s3 to this cluster/instance.
			+ ```spark-submit filename.jar <argument>```

	+ Optimizing for running on a cluster: Partitioning.
		+ How the data are partitioned.
		+ Self-join is expensive, and Spark won't distribute it on its own.
		+ Use .partitionBy() on an RDD before running a large operation that benefits from partitioning.
			+ join(), cogroup(), groupWith(), leftOuterJoin(), rightOuterJoin(), groupByKey(), reduceByKey(), combineByKey(), lookup().
			+ these operations will preserve your partitioning in their result too.
	+ Specifying memory per executor:
		+ use an empty, default SparkConf in your driver.
			+ this way, we'll use the default EMR sets up to pass into spark-submit from your master node.
		+ If executors start failing, you may need to adjust the memory each executor has.
			+ for example:
			```
			spark-submit --executor-memory 1g MovieSimilarities1M.jar 260
			```
			(from the master node of our cluster)
	+ Specifying a cluster manager:
		+ can use ```--master yarn``` to run on a YARN cluster (EMR sets it by default).

	+ Running on a cluster:
		+ Get your scripts and data to place that EMR can access to them. (i.e. S3, GCP)
		+ Spin up an EMR cluster for Spark using the AWS console.
		+ Get the external DNS name for the master node, and log into it using the hadoop user account and you private key files.
		+ Copy your driver program (JAR file) and its associates.
		+ Run spark-submit and wait for the result.
		+ Remember to terminate your cluster when you're done.

	+ Troubleshooting:
		+ connections:
			+ Your master will run a console on port 4040
			+ Impossible to actually connect to it from outside, easier from local (own) network.  
		+ cluster jobs:
			+ logs:
				+ in standalone mode with WebUI
				+ in YARN, the logs are distributed, you need to collect them after using ```yarn logs --appID <appID>```
			+ while your driver script runs, it will log errors like executors failing to issue.
				+ This means you are asking too much of each executor.
				+ Or, you may need more of them, ie. more machines in your cluster.
				+ each executor may need more memory.
				+ Or, use partitionBy() to demand less work from individual executors by using smaller partitions.
		+ managing dependencies:
			+ remember your executors aren't necessarily on the same box as your driver script.
			+ use broadcast variables to share data outside of RDD.
			+ need some Java or Scala package that's not pre-loaded on EMR.
				+ Bundle them into your JAR file with sbt assembly.
				+ Or, use --jars with spark-submit to add individual libs that are on the master.
				+ Try to just avoid using obscure package you don't need in the first place. 

### Section 6: SparkSQL, DataFrames, DataSets
	+ Working with structured data.
		+ extends RDD to a ```DataFrame``` object.
		+ Dataframes:
			+ contain row objects.
			+ run on SQL queries.
			+ has a schema (leading to more efficient storage)
			+ read and write to json, Hive, parquet.
			+ communicates with JDBC/ODBC, Tableau.
		+ DataSets:
			+ A dataframe is just a dataset of row objects, Dataset[Row]. 
			+ DataSets can explicitly wrap a given struct or type.
				+ Dataset[Person]
				+ Dataset[(string, Double)]
			+ DataFrames schema is inferred at runtime;
			+ Dataset can be inferred at compile time.
				+ Faster detection of errors, and better optimization.
			+ RDD can be converted to DataSets with .toDS()

			+ The trend in Spark is to use RDD's less, and DataSets more.
			+ DataSets are more efficient:
				+ they can be serialized very efficiently - even better than Kryo.
				+ optimal execution plans can be determined at compile time.
			+ DataSets allow for better interoperability.
				+ MLLib and Spark Streaming are moving towards using DataSets instead of RDDs for their primary API.
			+ DataSets simplify development.
				+ you can perform most SQL operations on a dataset with one line.
	
	+ Using Spark SQL in Scala:
		+ In spark 2.0.0, you create a SparkSession object instead of a SparkContext when using SparkSQL/DataSets.
			+ You can get a SparkContext from this session, and ise it to issue SQL queries on your DataSets
			+ Stop the session when you're done.
		
		+ Other stuff you can do with DataFrames:
			`res.show()`
			`res.select("fieldnames")`
			`res.filter(res.("fieldnames") > 200)`
			`res.groupBy(res("fieldnames")).mean()`
			`res.rdd().map(mapperFunction)`

		+ Shell Access: 
			+ SparkSQL exposes a JDBC/ODBC server (if you built Spark with Hive support).
			+ Start it with sbin/start-thriftserver.sh
			+ Listens on port 10000 by default.
			+ Connect using bin/beeline -u jdbc:hive2://localhost:10000
			+ At this point, you should have a SQL shell to SparkSQL.
			+ You can create new tables, or query existing ones that were cached using hiveCtx.cacheTable("tableName").
		+ User-Defined functions (UDF'S):
			`import org.apache.spark.sql.functions.udf`
			`val square = (x => x*x)`
			`squaredDF = df.withColumn("square", square('value'))`
		+ Example:
			+ Use social network data, query it with SQL, and then use DataSets without SQL.
			+ We re-do our popular movies example with DataSets to see how much simplier it is.


### Section 7: Machine Learning with MLLib
	+ ML capabilities:
		+ Feature extraction:
			+ Term-Frequency/Inverse Document Frequency useful for search.
		+ Basic statistics
			+ Chi-squared test, Pearson or Spearman correlation, min, max, mean, variance.
		+ Linear regression, logistic regression.
		+ Support vector machines.
		+ Naive Bayes classifier.
		+ Decision Trees
		+ K-means clustering.
		+ Principle Component Analysis, Singular value decomposition.
		+ Recommendations using Alternating Least Squares.
	+ ML uses DataFrames
		+ Previous API was called MLLib and used RDD and some specialized data structures.
		+ MLLib is deprecated in Spark 3.
		+ Newer "ML" library uses DataFrames for everything
	+ More depth:
		+ Ref book: Advanced Analytics with Spark from O'Reilly.
			+ examples with Spark SDK
			+ General ML courses

	+ Movies recommendations
		`val data = spark.read.textFile("../ml-100k/u.data")`
		`val ratings = data.map(x => x.split("\t")).map(x => Ratings(x(0).toInt, x(1).toInt, x(2).toDouble)).toDF()`
		`val als = new ALS()
					.setMaxIter(5)
					.setRegParam(0.01)
					.setUserCol("userId")
					.setItemCol("movidId")
					.setRatingCol("rating")`
		`val model = als.fit(ratings)`
	+ Run MovieRecommendationsALS.jar with spark-submit.
			~/$ spark-submit --class com.sundog.spark.MovieRecommendationsALS MRALS.jar 260
			
			20/03/20 18:27:20 WARN NativeCodeLoader: Unable to load native-hadoop library for your platform... using builtin-java classes where applicable
			Loading movie names...

			Training recommendation model...

			Top 10 recommendations for user ID 260:
			(I'll Do Anything (1994),10.982834)
			(House of Yes, The (1997),9.521749)
			(White Man's Burden (1995),9.212082)
			(April Fool's Day (1986),9.024569)
			(Warriors of Virtue (1997),8.992435)
			(Bitter Moon (1992),8.663096)
			(Braindead (1992),8.61241)
			(Ghost in the Shell (Kokaku kidotai) (1995),8.443162)
			(Faster Pussycat! Kill! Kill! (1965),8.396412)
			(Half Baked (1998),8.389826)


### Section 8: Spark Streaming
	+ Concepts:
		+ Analyzes continual streams of data
			+ common example: processing log data from a website or server
		+ Data is aggregated and analyzed at some given interval
		+ Can take data fed to some port, Amazon Kinesis, HDFS, Kafla, Flume, others.
		+ "Checkpointing" stores state to disk periodically for fault tolerance.
		+ A "Dstream" object breaks up the stream into distinct RDD.
		+ Simple example:
			```
			val stream = new StreamingContext(conf, Seconds(1))
			val lines = stream.socketTextStream("localhost", 8888)
			val errors = lines.filter(_.contains("error"))
			errors.print()
			```
			+ This listens to log data sent into port 8888, one second at a time, and prints out error lines.
			+ You need to kick off the job explicitly:
				```
				stream.start()
				stream.awaitTermination()
				``` 
			+ Remember your RDD's only contain one little chunk of incoming data.
			+ "Windowed operations" can combine results from multiple batches over some sliding time window.
				+ Refer to window(), reduceByWindow(), reduceByKeyAndWindow()
			+ updateStateByKey()
				+ Lets you maintain a state across many batches as time goes on.
					+ For example, running counts of some events.

	+ Let's stream:
		+ Run a spark streaming script that monitors live tweets from Twitter, and keeps track of the most popular hashtags as Tweets are received.
			+ Need to get an API key and access token.
			+ Allow us to stream Twitter data in realtime.
			+ Do this at https://apps.twitter.com/
		+ Create a twitter.txt file in your workspace
			+ On each line, specify a name and your own consumer key & access token information.
			+ For example, substitute in your own keys & tokens.
				```
				consumerKey AX*
				consumerSecret 9E*
				accessToken 37*
				accessTokenSecret 8*
				``` 
		+ Step 1:
			+ Get a twitter stream and extract just the messages themselves. 
				```
				val tweets = TwitterUtils.createStream(ssc, None)
				val statuse = tweets.map(status => status.getText())

				Vote for #McLaren!
				Vote for #Ferrari
				#Ferrari is the best.
				...
				``` 
		+ Step 2:
			+ Create a new Dstream that has every individual word as its own entry.
			+ We use flatMap() for this.
				```
				val tweetwords = statuses.flatMap(tweetText => tweetText.split(" "))

				Vote
				for
				#McLaren
				..
				```
		+ Step 3:
			+ Eliminate anything that's not a hashtag
				+ use the filter() function for this
				```
				val hashtags = tweetwords.filter(word => word.startsWith("#"))

				#McLaren
				#Ferrari
				#Ferrari
				...
				```
		+ Step 4:
			+ Convert our RDD of hashtags to key/value pairs
			+ We can count them up with a reduce operation.
			```
			val hashtagKV = hashtags.map(hashtag => (hashtag, 1))

			(#McLaren, 1)
			(#Ferrari, 1)
			(#Ferrari, 1)
			...
			```
		+ Step 5:
			+ Count up the results over a sliding window.
			+ A reduce operation adds up all of the values for a given key
				+ a key is a unique hashtag, and each value is 1
				+ by adding up all the "1"s associated with each instance of hashtag, we get the count of that hashtag.
			+ reduceByKeyAndWindow performs this reduce operation over a given window and slide interval.
				```
				val hashtagCounts = hashtagKV.reduceByKeyAndWindow((x,y) => x+y, (x,y)=>x-y, Seconds(300), Seconds(1))

				(#McLaren, 1)
				(#Ferrari, 2)
				...
				```
		+ Step 6:
			+ Sort and output the results.
			+ The counts are in the second value of each tuple, so we sort by the ._2 element.
				```
				val sortedResults = hashtagCounts.transform(rdd => rdd.sortBy(x => x._2, false))

				sortedResults.print

				(#Lamborghini, 3)
				(#Ferrari, 2)
				...
				```

	+ Setup a Twitter account and run scala file.
		+ Get keys and add to `twitter.txt`, then import properties with *twitter*.jar files in the SparkScala3. Finally, import file `PopularHashtags.scala`.
		+ Created Twitter dev account, waiting for approval. So have to wait it for running `PopularHashtags.scala`.

	+ Structured streaming
		+ Spark 2 introduced "structured streaming"
		+ uses DataSets as its primary API 
		+ Imagine a DataSet that just keeps getting appended to forever, and you query it whenever you want.
		+ Streaming is now real-time and not based on "micro-batches".
			```
			val inputDF = spark.readStream.json("s3://logs")
			inputDF.groupBy($"action", window($"time", "1 hour")).count()
					.writeStream.format("jdbc").start("jdbc:mysql//..")
			```

### Section 9: Intro to GraphX 
	+ Intro
		+ It can measure things like: 
			+ "connectedness"
			+ degree distribution
			+ average path length
			+ triangle counts - high level measures of a graph.
		+ It can count triangles in the graph and apply the PageRank algorithm.
		+ Can join Graphs together and transform graphs quickly.
		+ Can't find built-in support for "degrees of separation", but it does support the Pregel API for traversing a graph.

		+ Introduces VertexRDD and EdgeRDD and the Edge data type.
		+ Otherwise, GraphX code looks like any other Spark code for the most part.

	+ Creating vertex RDD's
		```
		// Function to extract hero ID -> hero name tuples (or None in case of failure)

		def parseNames(line: String) : Option[(VertexId, String)] = {
			var fields = line.split("\")
			if (fields.length > 1) {
				val heroID:Long = fields(0).trim().toLong
				if (heroID < 6487) { // ID's above 6487 aren't real characters
					return Some(fields(0).trim().toLong, fields(1))
				}
			}
			return None // flatmap will just discard None results, and extract data from Some results.
		}

		```


	+ Creating edge RDD's
		```
		// Transform an input line from marval-graph.txt into a list of edges.
		def makeEdges(line : String) : List[Edge[Int]] = {
			import scala.collection.mutable.ListBuffer
			var edges = new ListBuffer[Edge[Int]]()
			var fields = line.split(" ")
			var origin = field(0)
			for (x <- 1 to (fields.length - 1)) {
				// Our attribute field is unused, but other graphs could be used to deep track of physical distances..
				edges += Edge(origin.toLong, fields(x).toLong, 0)
			}
			return edges.toList
		}

		```
	+ Creating a Graph:
		```
		// Build up our vertices
		val names = sc.textFile("../marvel-names.txt")
		val verts = names.flatMap(parseNames)
		// Build up our edges
		val lines = sc.textFile("../marvel-graph.txt")
		val edges = lines.flatMap(makeEdges)
		// Build up our graph, and cache it as we're going to do a bunch of stuff with it.
		val default = "Nobody"
		val graph = Graph(verts, edges, default).cache()

		```

	+ Doing thing
		+ Top 10 most connected heroes
			`graph.degrees.join(verts).sortBy(_._2._1, ascending=false).take(10).foreach(println)`

			+ BFS with Pregel (prek'go) ;)

				+ How Pregel works:
					+ Vertices send messages to other vertices (their neighbors)
					+ The graph is processed in iterations called supersteps
					+ Ech superstep does the following:
						+ Messages from the previous iteration are received by each vertex.
						+ Each vertex runs a program to transform itself
						+ Each vertex sends messages to other vertices. 

				+ BFS:
					+ Initialization:
					```
					val initialGraph = graph.mapVertices((id,_) => if (id==root) 0.0 else Double.PositiveInfinity)
					``` 
					
					+ Sending messages:
					```
					triplet => {
						if (triplet.srcAttr !=Double.PositiveInfinity) {
							Iterator((triplet.dstId, triplet.srcAttr + 1))
						} else {
							Iterator.empty
						}
					}
					```

					+ Preserving the minimum distance at each step.
						+ Pregel's vertex program will preserve the minimum distance between the one it receives and its current value.
							`(id, attr, msg) => math.min(attr, msg)`
						+ Its reduce operation preserves the minimum distance if multiple messages are received for the same vertex.
							`(a, b) => math.min(a,b)`

			+ Putting it all together

				```
				val initialGraph = graph.mapVertices((id,_) => if (id==root) 0.0 else Double.PositiveInfinity)

				val bfs = initialGraph.pregel(Double.PositiveInfinity, 10)(
					(id, attr, msg) => math.min(attr, msg) // preserves min distance
					// send messages
					triplet => {
						if (triplet.srcAttr !=Double.PositiveInfinity) {
							Iterator((triplet.dstId, triplet.srcAttr + 1))
						} else {
							Iterator.empty
						}
					}
					// preserves min distance if multiple messages are received from the same vertex.
					(a, b) => math.min(a,b)

				)
				```  






















































