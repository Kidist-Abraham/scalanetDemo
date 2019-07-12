package io.iohk.scalanet

import io.iohk.decco.auto._
import io.iohk.scalanet.NetUtils._
import io.iohk.scalanet.TaskValues._
import monix.execution.Scheduler.Implicits.global
//import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures._
//import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import io.iohk.scalanet.peergroup.{InetMultiAddress}
//import scala.concurrent.Future
import scala.concurrent.duration._
import java.net._
import scala.concurrent.Future


import io.iohk.scalanet.codec.StreamCodec
import io.iohk.decco.Codec
import io.iohk.scalanet.codec.FramingCodec
import io.iohk.decco.BufferInstantiator.global.HeapByteBuffer
//import scala.util.Random
object Bob extends App  {

Thread.sleep(2000) 

  implicit val patienceConfig: ScalaFutures.PatienceConfig = PatienceConfig(25 seconds)
  implicit val codec = new FramingCodec(Codec[String])
val aliceAddr: InetAddress = InetAddress.getByName("172.31.19.22");
     val bob = bobTCPPeerGroup[String]
  //   val alice = aliceTCPPeerGroup[String]


     
    
  println(s"Bob is ${bob.processAddress}")
     
      val bobMessage = "from Bob"//Random.alphanumeric.take(1024).mkString
 
       

      //b.server().foreachL(channel => channel.sendMessage(bMessage).evaluated).runAsync
      val bobClient = bob.client(new InetMultiAddress(new InetSocketAddress(aliceAddr,8080))).evaluated //   def client(to: A): Task[Channel[A, M]]
 // println("It reach here")
      bobClient.sendMessage(bobMessage).evaluated
      val bobReceived : Future[String]= bobClient.in.headL.runAsync

      

      
      
      println("Bob : " + bobReceived.futureValue)
    
      
      
   
      bob.shutdown().runAsync.futureValue
      



 
}
