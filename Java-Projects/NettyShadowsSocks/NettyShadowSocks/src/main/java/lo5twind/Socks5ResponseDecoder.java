package lo5twind;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class Socks5ResponseDecoder extends ReplayingDecoder<Void> {
    private static final Logger logger = LogManager.getLogger(
            Socks5ResponseDecoder.class);
    private Promise<Channel> promise;


    public Socks5ResponseDecoder(Promise<Channel> promise) {
        this.promise = promise;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            byte versionVal;
            SocksVersion version;
            ChannelPipeline p = ctx.pipeline();
            // get socks version
            versionVal = in.readByte();
            version = SocksVersion.valueOf(versionVal);

            switch (version) {
                case SOCKS5:
                    logger.debug("Get Socsks5 Response...from Shadowsocks server");
                    final Socks5CommandType cmdType = Socks5CommandType.valueOf(in.readByte());
                    in.skipBytes(1); // RSV byte
                    final Socks5AddressType dstAddrType = Socks5AddressType.valueOf(in.readByte());
                    final String dstAddr = Socks5AddressDecoder.DEFAULT.decodeAddress(dstAddrType, in);
                    final int dstPort = in.readUnsignedShort();

                    logger.debug("Get[{}] dst={}, dstPort={}", dstAddrType, dstAddr, dstPort);
                    // get response from shadowsocks server
                    // remove this handler because no commands are need
                    ctx.pipeline().remove(this);
                    // set promise for Shadowsocks connection
                    this.promise.setSuccess(ctx.channel());
                    break;
                default:
                    logger.error("Unsupport Socks Version");
                    in.skipBytes(in.readableBytes());
                    ctx.close();
                    return;
            }

        } catch (Exception e) {
            logger.error(e);

        }

    }

}
