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

### Section 8: Spark Streaming

### Section 9: Intro to GraphX 





































