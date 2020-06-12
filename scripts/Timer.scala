

//Timer
class Timer (var t0:Double=System.nanoTime().toDouble,   var dt:Double=0)  extends Serializable  {

  def time:Double=System.nanoTime().toDouble

  def step:Double={
    val t1 = System.nanoTime().toDouble
    dt=(t1-t0)/1e9
    t0=t1
    dt
  }
  def print(msg:String):Unit={
    val sep="----------------------------------------"
    println("\n"+msg+": "+dt+" s\n"+sep)
  }
}
