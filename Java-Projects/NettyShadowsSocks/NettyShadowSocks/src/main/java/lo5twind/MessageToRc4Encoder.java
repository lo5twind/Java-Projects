package lo5twind;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MessageToRc4Encoder extends MessageToByteEncoder<ByteBuf> {
    private static final Logger logger = LogManager.getLogger(
            MessageToRc4Encoder.class);
    private final String key;
    private final Rc4EncryptUtils rc4Codec;

    public MessageToRc4Encoder(String key) {
        this.key = key;
        this.rc4Codec = new Rc4EncryptUtils(this.key);

    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf s, ByteBuf out) throws Exception {
        logger.debug("In MessageToRc4Encoder");
        if (s.readableBytes() > 0) {
            ByteBuf encryptString = this.rc4Codec.encrypt(s);
            logger.debug("In MessageToRc4Encoder: encode size={}", encryptString.readableBytes());
            out.writeInt(encryptString.readableBytes());
            out.writeBytes(encryptString);
        }
    }
}
