package lo5twind;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class Socks5ServerHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private static final Logger logger = LogManager.getLogger(
            Socks5ServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage socksMessage) throws Exception {
        if (socksMessage.version() != SocksVersion.SOCKS5) {
            logger.error("Unsupport Socks Version {}", socksMessage.version());
            ctx.close();
        }

        // process Initial Request
        if (socksMessage instanceof Socks5InitialRequest) {
            logger.debug("Send NO_AUTH response...");
            ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
        } else if (socksMessage instanceof Socks5CommandRequest){
            Socks5CommandRequest socks5CommandRequest = (Socks5CommandRequest) socksMessage;
            if (socks5CommandRequest.type() == Socks5CommandType.CONNECT) {
                // Get a connect command request
                logger.debug("Add Connect Socket Handler");
                ctx.pipeline().addLast(new Socks5ServerConnectHandler());
                logger.debug("Remove SocksServerHandler");
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(socks5CommandRequest);
            }
        } else {
            logger.error("Unknown Type");
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) { ctx.flush(); }
}
