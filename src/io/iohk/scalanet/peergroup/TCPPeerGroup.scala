package io.iohk.scalanet.peergroup

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import io.iohk.scalanet.messagestream.{MessageStream, MonixMessageStream}
import io.iohk.scalanet.peergroup.PeerGroup.{InitializationError, Lift, TerminalPeerGroup}
import io.iohk.scalanet.peergroup.TCPPeerGroup.Config
import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.handler.codec.{LengthFieldBasedFrameDecoder, LengthFieldPrepender}
import io.netty.handler.codec.bytes.ByteArrayEncoder
import monix.eval.Task

import scala.language.higherKinds

class TCPPeerGroup[F[_]](val config: Config)(implicit liftF: Lift[F])
    extends TerminalPeerGroup[InetSocketAddress, F]() {

  private val nettyDecoder = new NettyDecoder()

  private val workerGroup = new NioEventLoopGroup()

  private val subscribers = new Subscribers()

  private val clientBootstrap = new Bootstrap()
    .group(workerGroup)
    .channel(classOf[NioSocketChannel])
    .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)

  private val serverBootstrap = new ServerBootstrap()
    .group(workerGroup)
    .channel(classOf[NioServerSocketChannel])
    .childHandler(new ChannelInitializer[SocketChannel]() {
      override def initChannel(ch: SocketChannel): Unit = {

        ch.pipeline()
          .addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Int.MaxValue, 0, 4, 0, 4))
          .addLast(nettyDecoder)
      }
    })
    .option[Integer](ChannelOption.SO_BACKLOG, 128)
    .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
    .bind(config.bindAddress)
    .syncUninterruptibly()

  override def sendMessage(address: InetSocketAddress, message: ByteBuffer): F[Unit] = {
    val send: Task[Unit] = Task {

      val activationAdapter = new ChannelInboundHandlerAdapter() {
        override def channelActive(ctx: ChannelHandlerContext): Unit = {
          ctx
            .writeAndFlush(Unpooled.wrappedBuffer(message))
            .addListener((_: ChannelFuture) => ctx.channel().close())
        }
      }

      clientBootstrap
        .handler(new ChannelInitializer[SocketChannel]() {
          def initChannel(ch: SocketChannel): Unit = {
            ch.pipeline()
              .addLast("frameEncoder", new LengthFieldPrepender(4))
              .addLast(new ByteArrayEncoder())
              .addLast(activationAdapter)
          }
        })
        .connect(address)
      ()
    }
    liftF(send)
  }

  override def shutdown(): F[Unit] = {
    liftF(Task {
      serverBootstrap.channel().close()
      workerGroup.shutdownGracefully()
      ()
    })
  }

  override val messageStream: MessageStream[ByteBuffer] = new MonixMessageStream(subscribers.monixMessageStream)

  private class NettyDecoder extends ChannelInboundHandlerAdapter {
    override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
      // TODO message stream will need to expose the address.
//      val remoteAddress = ctx.channel().remoteAddress().asInstanceOf[InetSocketAddress]
      val byteBuffer = msg.asInstanceOf[ByteBuf]
      subscribers.notify(byteBuffer.nioBuffer())
    }
  }
}

object TCPPeerGroup {

  case class Config(bindAddress: InetSocketAddress)

  def create[F[_]](config: Config)(implicit liftF: Lift[F]): Either[InitializationError, TCPPeerGroup[F]] =
    try {
      Right(new TCPPeerGroup[F](config))
    } catch {
      case t: Throwable =>
        Left(InitializationError(initializationErrorMsg(config), t))
    }

  def createOrThrow[F[_]](config: Config)(implicit liftF: Lift[F]): TCPPeerGroup[F] =
    try {
      new TCPPeerGroup[F](config)
    } catch {
      case t: Throwable =>
        throw new IllegalStateException(initializationErrorMsg(config), t)
    }

  private def initializationErrorMsg[F[_]](config: Config) =
    s"Failed initialization of ${classOf[TCPPeerGroup[F]].getName} with config $config. Cause follows."
}