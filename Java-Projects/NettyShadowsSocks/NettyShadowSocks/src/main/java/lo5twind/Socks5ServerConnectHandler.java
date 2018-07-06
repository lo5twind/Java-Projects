package lo5twind;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.NetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Socks5ServerConnectHandler extends SimpleChannelInboundHandler<SocksMessage>{
    private static final Logger logger = LogManager.getLogger(
            Socks5ServerConnectHandler.class);

    private final Bootstrap bootstrap = new Bootstrap();
    private boolean isLocal = true;

    public Socks5ServerConnectHandler(boolean isLocal) {
        this.isLocal = isLocal;
    }

    public Socks5ServerConnectHandler() {
        this.isLocal = true;
    }

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
            public void operationComplete(Future<Channel> future) throws Exception {
                // A new channel connects socks5 server and remote server
                final Channel outboundChannel = future.getNow();
                if (future.isSuccess()) {
                    // Send Response to Client
                    if (isLocal) {
                        // give response to client when request is send
                        logger.debug("Connected to remote server...{}:{}", connectRequest.dstAddr(), connectRequest.dstPort());
                        logger.debug("Send CommandResponse to client");
                        ChannelFuture responseFuture =
                                inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(
                                        Socks5CommandStatus.SUCCESS,
                                        connectRequest.dstAddrType(),
                                        connectRequest.dstAddr(),
                                        connectRequest.dstPort()));
                        responseFuture.addListener(new ChannelFutureListener() {
                            public void operationComplete(ChannelFuture future) throws Exception {
                                if (future.isSuccess()) {
                                    logger.debug("Send CommandResponse to client...Done");
                                    // set relay handler for inboundChannel and outboundChannel
                                    ctx.pipeline().remove(Socks5ServerConnectHandler.this);
                                    inboundChannel.pipeline().addLast(new RelayHandler(outboundChannel));
                                    // inbound handler
                                    outboundChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(32 * 1024, 0, 4));
                                    outboundChannel.pipeline().addLast(new Rc4ToMessageDecoder("key"));
                                    // outbound handler
                                    outboundChannel.pipeline().addLast(new MessageToRc4Encoder("key"));
                                    // inbound handler
                                    outboundChannel.pipeline().addLast(new RelayHandler(inboundChannel));

                                    // send remote address and port to shadowsocks server
                                    // TODO: what do to if send request error
                                    // outboundChannel.pipeline().writeAndFlush(connectRequest);
                                } else {
                                    // response fail
                                    logger.error("Send CommandResponse to client...error");
                                    ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, connectRequest.dstAddrType()));
                                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                                }
                            }
                        });
                    } else { // at shadowsocks side
                        if (future.isSuccess()){
                            ctx.pipeline().remove(Socks5ServerConnectHandler.this);
                            logger.debug("Processing handler......");
                            outboundChannel.pipeline().addLast(new RelayHandler(inboundChannel));
                            // inbound handler
                            inboundChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(32 * 1024, 0, 4));
                            inboundChannel.pipeline().addLast(new Rc4ToMessageDecoder("key"));
                            // outbound handler
                            inboundChannel.pipeline().addLast(new MessageToRc4Encoder("key"));
                            // inbound handler
                            inboundChannel.pipeline().addLast(new RelayHandler(outboundChannel));
                            logger.debug(inboundChannel.pipeline());
                        }
                    }


                } else {
                    // Connect to remote server fail
                    logger.debug("Connected to remote server Error...{}:{}", connectRequest.dstAddr(), connectRequest.dstPort());
                    ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, connectRequest.dstAddrType()));
                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }

            }
        });
        // Do connect
        // if isLocal connect to shadowsocks server, if NOT connect to remote server specified by address and port
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new RemoteConnectHandler(promise, isLocal, connectRequest));
        // Connect and add listener for connect result
        if (isLocal) {
            // TODO: get host and port from configuration file
            // bootstrap.connect("127.0.0.1", 8889).addListener(
            bootstrap.connect("45.63.93.31", 8889).addListener(
                    new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()){
                                logger.debug("Connect to shadowsocks server success...!!");
                            } else {
                                logger.error("Connect to shadowsocks server fail...!!");
                                inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(
                                        Socks5CommandStatus.FAILURE, connectRequest.dstAddrType()
                                )).addListener(ChannelFutureListener.CLOSE);
                            }
                        }
                    }
            );

        } else { // at shadowsocks server side
            bootstrap.connect(connectRequest.dstAddr(), connectRequest.dstPort()).addListener(
                    new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()){
                                logger.debug("Connect to remote server success...!!");
                                logger.debug("Send Response to local...!!");
                                final ByteBuf respBuf = inboundChannel.alloc().buffer();
                                respBuf.writeByte(0x05); // Socks5
                                respBuf.writeByte(0x00); // Success
                                respBuf.writeByte(0x00); // RSV
                                respBuf.writeByte(0x01); // IPv4
                                respBuf.writeBytes(NetUtil.createByteArrayFromIpAddressString(connectRequest.dstAddr()));
                                respBuf.writeByte(connectRequest.dstPort() >> 8 & 0xFF); // port first 8 bit
                                respBuf.writeByte(connectRequest.dstPort() & 0xFF); // port last 8 bit
                                ReferenceCountUtil.release(connectRequest);
                                inboundChannel.writeAndFlush(respBuf).addListener(
                                        new ChannelFutureListener() {
                                            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                                                if (channelFuture.isSuccess()){
                                                    logger.debug("Send Response to local success...");
                                                } else {
                                                    logger.error("Send Response to local fail...{}", channelFuture.cause());
                                                }
                                            }
                                        }
                                );
                            } else {
                                logger.error("Connect to remote server fail...!!");
                                inboundChannel.writeAndFlush(new DefaultSocks5CommandResponse(
                                        Socks5CommandStatus.FAILURE, connectRequest.dstAddrType()
                                )).addListener(ChannelFutureListener.CLOSE);
                            }
                        }
                    }
            );

        }
    }

   /*
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        logger.error(cause);
    }
    */

}
