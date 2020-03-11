object LearningScala3 {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(225); 
  // Functions
  
  // Format is def <function name>(parameter name: type...) : return type = { expression }
  // Don't forget the = before the expression!
  def squareIt(x: Int) : Int = {
  	x * x
  };System.out.println("""squareIt: (x: Int)Int""");$skip(43); 
  
  def cubeIt(x: Int): Int = {x * x * x};System.out.println("""cubeIt: (x: Int)Int""");$skip(26); 
  
  println(squareIt(2));$skip(24); 
  
  println(cubeIt(2));$skip(124); 
  
  // Functions can take other functions as parameters
  
  def transformInt(x: Int, f: Int => Int) : Int = {
  	f(x)
  };System.out.println("""transformInt: (x: Int, f: Int => Int)Int""");$skip(44); 
  
  val result = transformInt(2, squareIt);System.out.println("""result  : Int = """ + $show(result ));$skip(19); 
  println (result);$skip(209); val res$0 = 
  
  // "Lambda functions", "anonymous functions", "function literals"
  // You can declare functions inline without even giving them a name
  // This happens a lot in Spark.
  transformInt(3, x => x * x * x);System.out.println("""res0: Int = """ + $show(res$0));$skip(34); val res$1 = 
  
  transformInt(10, x => x / 2);System.out.println("""res1: Int = """ + $show(res$1));$skip(50); val res$2 = 
  
  transformInt(2, x => {val y = x * 2; y * y});System.out.println("""res2: Int = """ + $show(res$2));$skip(424); 
  
  // This is really important!
  
  // EXERCISE
  // Strings have a built-in .toUpperCase method. For example, "foo".toUpperCase gives you back FOO.
  // Write a function that converts a string to upper-case, and use that function of a few test strings.
  // Then, do the same thing using a function literal instead of a separate, named function.
  
  def convertUpperString(s : String) : String = {
  	s.toUpperCase
  };System.out.println("""convertUpperString: (s: String)String""");$skip(21); 
  
  val s = "hello";System.out.println("""s  : String = """ + $show(s ));$skip(33); 
  println(convertUpperString(s));$skip(81); 
  
  def transformString(s: String, f:String => String) : String = {
  	f(s)
  };System.out.println("""transformString: (s: String, f: String => String)String""");$skip(54); val res$3 = 
  
  
  transformString("hihihi", convertUpperString);System.out.println("""res3: String = """ + $show(res$3));$skip(48); val res$4 = 
  transformString("hahaha", convertUpperString);System.out.println("""res4: String = """ + $show(res$4));$skip(48); val res$5 = 
  transformString("hohoho", convertUpperString);System.out.println("""res5: String = """ + $show(res$5));$skip(53); val res$6 = 
  transformString("hello world", convertUpperString);System.out.println("""res6: String = """ + $show(res$6))}
  
}
