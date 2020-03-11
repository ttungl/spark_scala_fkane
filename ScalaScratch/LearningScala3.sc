object LearningScala3 {
  // Functions
  
  // Format is def <function name>(parameter name: type...) : return type = { expression }
  // Don't forget the = before the expression!
  def squareIt(x: Int) : Int = {
  	x * x
  }                                               //> squareIt: (x: Int)Int
  
  def cubeIt(x: Int): Int = {x * x * x}           //> cubeIt: (x: Int)Int
  
  println(squareIt(2))                            //> 4
  
  println(cubeIt(2))                              //> 8
  
  // Functions can take other functions as parameters
  
  def transformInt(x: Int, f: Int => Int) : Int = {
  	f(x)
  }                                               //> transformInt: (x: Int, f: Int => Int)Int
  
  val result = transformInt(2, squareIt)          //> result  : Int = 4
  println (result)                                //> 4
  
  // "Lambda functions", "anonymous functions", "function literals"
  // You can declare functions inline without even giving them a name
  // This happens a lot in Spark.
  transformInt(3, x => x * x * x)                 //> res0: Int = 27
  
  transformInt(10, x => x / 2)                    //> res1: Int = 5
  
  transformInt(2, x => {val y = x * 2; y * y})    //> res2: Int = 16
  
  // This is really important!
  
  // EXERCISE
  // Strings have a built-in .toUpperCase method. For example, "foo".toUpperCase gives you back FOO.
  // Write a function that converts a string to upper-case, and use that function of a few test strings.
  // Then, do the same thing using a function literal instead of a separate, named function.
  
  def convertUpperString(s : String) : String = {
  	s.toUpperCase
  }                                               //> convertUpperString: (s: String)String
  
  val s = "hello"                                 //> s  : String = hello
  println(convertUpperString(s))                  //> HELLO
  
  def transformString(s: String, f:String => String) : String = {
  	f(s)
  }                                               //> transformString: (s: String, f: String => String)String
  
  
  transformString("hihihi", convertUpperString)   //> res3: String = HIHIHI
  transformString("hahaha", convertUpperString)   //> res4: String = HAHAHA
  transformString("hohoho", convertUpperString)   //> res5: String = HOHOHO
  transformString("hello world", convertUpperString)
                                                  //> res6: String = HELLO WORLD
  
}