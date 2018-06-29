package lo5twind;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Socks5ServerConnectHandler extends SimpleChannelInboundHandler<SocksMessage>{
    private static final Logger logger = LogManager.getLogger(
            Socks5ServerConnectHandler.class);

    private final Bootstrap bootstrap = new Bootstrap();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SocksMessage msg) throws Exception {
        if (!(msg instanceof Socks5CommandRequest)) {
            logger.error("Invaild message type");
            // Just pass message to next handler
            ctx.fireChannelRead(msg);
            return;
        }
        final Socks5CommandRequest connectRequest = (Socks5CommandRequest) msg;
        // Get a new promise, Use this promise in a channelActive Handler, when channel is actived,
        // set promise success, and operations in promise listener will be executed.
        Promise<Channel> promise = ctx.executor().newPromise();
        // Launch a connection to remote server define by Socks5CommandRequest
        final Channel inboundChannel = ctx.channel();
        promise.addListener(new FutureListener<Channel>() {
            @Override
            public void operationComplete(Future<Channel> future) throws Exception {
                // A new channel connects socks5 server and remote server
                final Channel outboundChannel = future.getNow();
                if (future.isSuccess()) {
                    // Send Response to Client
                    logger.debug("Connected to remote server...{}:{}", connectRequest.dstAddr(), connectRequest.dstPort());
                    logger.debug("Send CommandResponse to client");
                    ChannelFuture responseFuture =
                            inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(
                                Socks5CommandStatus.SUCCESS,
                                connectRequest.dstAddrType(),
                                connectRequest.dstAddr(),
                                connectRequest.dstPort()));
                    responseFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                logger.debug("Send CommandResponse to client...Done");
                                // set relay handler for inboundChannel and outboundChannel
                                ctx.pipeline().remove(Socks5ServerConnectHandler.this);
                                inboundChannel.pipeline().addLast(new RelayHandler(outboundChannel));
                                outboundChannel.pipeline().addLast(new RelayHandler(inboundChannel));
                            } else {
                                // response fail
                                logger.error("Send CommandResponse to client...error");
                                ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, connectRequest.dstAddrType()));
                                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                            }
                        }
                    });

                } else {
                    // Connect to remote server fail
                    logger.debug("Connected to remote server Error...{}:{}", connectRequest.dstAddr(), connectRequest.dstPort());
                    ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, connectRequest.dstAddrType()));
                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }

            }
        });
        // Do connect
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new RemoteConnectHandler(promise));
        // Connect and add listener for connect result
        bootstrap.connect(connectRequest.dstAddr(), connectRequest.dstPort()).addListener(
                new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()){
                            logger.debug("Connect to remote server success...!!");
                        } else {
                            logger.error("Connect to remote server fail...!!");
                            ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                    Socks5CommandStatus.FAILURE, connectRequest.dstAddrType()
                            )).addListener(ChannelFutureListener.CLOSE);
                        }
                    }
                }
        );
    }

    /*
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
    */
}
