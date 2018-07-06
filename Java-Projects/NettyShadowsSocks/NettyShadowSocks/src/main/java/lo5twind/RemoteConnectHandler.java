package lo5twind;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.NetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoteConnectHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(
            RemoteConnectHandler.class);
    private final Promise<Channel> promise;
    private boolean isLocal;
    private Socks5CommandRequest connectRequest;

    public RemoteConnectHandler(final Promise<Channel> promise) {
        this.promise = promise;
    }

    public RemoteConnectHandler(final Promise<Channel> promise, final boolean isLocal, final Socks5CommandRequest connectRequest) {
        this.promise = promise;
        this.isLocal = isLocal;
        this.connectRequest = connectRequest;
    }

    public RemoteConnectHandler(final Promise<Channel> promise, final boolean isLocal) {
        this.promise = promise;
        this.isLocal = isLocal;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().remove(this);
        ctx.pipeline().addFirst(new LoggingHandler(LogLevel.INFO));

        if (this.isLocal) {
            // if at local side, send connect request to shadowsocks server
            logger.debug("Send connect Request to Shadowsocks server...");
            logger.debug(ctx.channel());
            // transfer connectRequest to ByteBuf
            final ByteBuf connReq = ctx.channel().alloc().buffer();
            connReq.writeByte(0x05); // Socks5
            connReq.writeByte(0x01); // CMD Connect
            connReq.writeByte(0x00); // RSV
            connReq.writeByte(0x01); // IPv4
            connReq.writeBytes(NetUtil.createByteArrayFromIpAddressString(connectRequest.dstAddr()));
            connReq.writeByte(connectRequest.dstPort() >> 8 & 0xFF); // port first 8 bit
            connReq.writeByte(connectRequest.dstPort() & 0xFF); // port last 8 bit

            // ReferenceCountUtil.release(connectRequest);

            ctx.channel().writeAndFlush(connReq).addListener(
                    new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture channelFuture) throws Exception {
                            if (channelFuture.isSuccess()) {
                                logger.debug("send request to shadowsocks...done");
                                // TODO: add response sync here
                                ctx.pipeline().addLast(new Socks5ResponseDecoder(promise));
                                // promise.setSuccess(ctx.channel());
                            } else {
                                logger.error("Send request to shadowsocks error");
                                logger.error(channelFuture.cause());
                            }
                        }
                    }
            );
            // add a socks5 response decoder for promise, if get response from shadowsocks server
            // promis will be set
        } else {
            logger.debug("Set promise success...");
            this.promise.setSuccess(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        this.promise.setFailure(cause);
    }
}
