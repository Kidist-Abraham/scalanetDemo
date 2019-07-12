package io.iohk.scalanet

import io.iohk.decco.auto._
import io.iohk.scalanet.NetUtils._
import io.iohk.scalanet.TaskValues._
import monix.execution.Scheduler.Implicits.global
//import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures._
//import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.Future
import scala.concurrent.duration._
import io.iohk.scalanet.codec.StreamCodec
import io.iohk.decco.Codec
import io.iohk.scalanet.codec.FramingCodec
import io.iohk.decco.BufferInstantiator.global.HeapByteBuffer

//import java.net._
//import io.iohk.scalanet.peergroup.{InetMultiAddress}
//import scala.util.Random
object Alice extends App  {

  implicit val patienceConfig: ScalaFutures.PatienceConfig = PatienceConfig(25 seconds)
  implicit val codec = new FramingCodec(Codec[String])

     val alice = aliceTCPPeerGroup[String]
     
  println(s"Alice is ${alice.processAddress}")
      val aliceMessage = "from Alice"//Random.alphanumeric.take(1024).mkString
     
     
       // val aliceClient = alice.client(new InetMultiAddress(new InetSocketAddress("localhost",8081))).evaluated
       // val aliceReceived = aliceClient.in.headL.runAsync
      alice.server().collectChannelCreated.foreach(channel => channel.sendMessage(aliceMessage).runAsync)
     // alice.server().foreachL(channel => channel.sendMessage(aliceMessage).evaluated).runAsync
     // val aliceReceived: Future[String] = alice.server().mergeMap(channel => channel.in).headL.runAsync
        val aliceReceived: Future[String] =alice.server().collectChannelCreated.mergeMap(channel => channel.in).headL.runAsync



     
      
      println("Alice : " + aliceReceived.futureValue)
     
      
      alice.shutdown().runAsync.futureValue
    
     



 
}
