package lo5twind;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.*;
import lo5twind.Socks5RequestDecoder.Stat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class Socks5RequestDecoder extends ReplayingDecoder<Stat> {
    private static final Logger logger = LogManager.getLogger(
            Socks5RequestDecoder.class);

    enum Stat {
        INIT,
        INIT_CONNECT,
        CONNECTED,
        SUCCESS,
        FAILURE
    }

    public Socks5RequestDecoder() {
        super(Stat.INIT);
    }

    public Socks5RequestDecoder(Stat state) {
        super(state);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        logger.debug("Hello in start of decode");
        try {
            byte versionVal;
            SocksVersion version;
            switch (state()){
                case INIT:
                    ChannelPipeline p = ctx.pipeline();
                    // get socks version
                    versionVal = in.readByte();
                    version = SocksVersion.valueOf(versionVal);

                    switch (version) {
                        case SOCKS5:
                            logger.debug("Get Socsks5 Initial Request...");
                            // get method count and methods
                            final int authMethodCnt = in.readUnsignedByte();
                            if (actualReadableBytes() < authMethodCnt) {
                                logger.error("Protocol Error: actualReadableBytes() < authMethodCnt)");
                                break;
                            }
                            logger.debug("Get Socsks5 Initial Request Method count = {}...", authMethodCnt);

                            final Socks5AuthMethod[] authMethods = new Socks5AuthMethod[authMethodCnt];
                            for (int i = 0; i < authMethodCnt; i++) {
                                authMethods[i] = Socks5AuthMethod.valueOf(in.readByte());
                                logger.debug("Get Socsks5 Initial Request Method method[{}]={}...", i, authMethods[i]);
                            }
                            // add SocksServerHandler
                            p.addAfter(ctx.name(), null, new Socks5ServerHandler());
                            p.addAfter(ctx.name(), null, Socks5ServerEncoder.DEFAULT);
                            out.add(new DefaultSocks5InitialRequest(authMethods));
                            checkpoint(Stat.INIT_CONNECT);
                            break;
                        default:
                            logger.error("Unsupport Socks Version");
                            in.skipBytes(in.readableBytes());
                            checkpoint(Stat.FAILURE);
                            ctx.close();
                            return;
                    }
                    break;
                case INIT_CONNECT:
                    logger.debug("Get Socks5 Request Command...Connecting");
                    // get socks version
                    versionVal = in.readByte();
                    version = SocksVersion.valueOf(versionVal);
                    if (version != SocksVersion.SOCKS5) {
                        logger.error("Get Invalid Socks Version");
                        ctx.close();
                        return;
                    }
                    final Socks5CommandType cmdType = Socks5CommandType.valueOf(in.readByte());
                    in.skipBytes(1); // RSV byte
                    final Socks5AddressType dstAddrType = Socks5AddressType.valueOf(in.readByte());
                    final String dstAddr = Socks5AddressDecoder.DEFAULT.decodeAddress(dstAddrType, in);
                    final int dstPort = in.readUnsignedShort();

                    logger.debug("Get[{}] dst={}, dstPort={}", dstAddrType, dstAddr, dstPort);
                    // get request command
                    out.add(new DefaultSocks5CommandRequest(cmdType, dstAddrType, dstAddr, dstPort));
                    checkpoint(Stat.CONNECTED);
                    // remove this handler because no commands are need
                    ctx.pipeline().remove(this);

                    break;

                 default:
                     logger.debug("Get Unsupport data");
            }

        } catch (Exception e) {
            logger.error(e);

        }

    }

}
