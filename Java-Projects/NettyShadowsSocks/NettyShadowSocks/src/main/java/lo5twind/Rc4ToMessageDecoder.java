package lo5twind;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class Rc4ToMessageDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final Logger logger = LogManager.getLogger(
            Rc4ToMessageDecoder.class);
    private final String key;
    private final Rc4EncryptUtils rc4Codec;


    public Rc4ToMessageDecoder(String key) {
        this.key = key;
        this.rc4Codec = new Rc4EncryptUtils(this.key);

    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf s, List<Object> out) throws Exception {
        logger.debug("In Rc4ToMessageDecoder");
        if (s.readableBytes() > 4) {
            int frameLengthInt = s.readInt();
            if (frameLengthInt != s.readableBytes()) {
                logger.debug("In Rc4ToMessageDecoder get size={}, Remain frame size={}", frameLengthInt, s.readableBytes());
                logger.debug("In Rc4ToMessageDecoder get Invalid message");
                return;
            }
            logger.debug("In Rc4ToMessageDecoder get size={}, Remain frame size={}", frameLengthInt, s.readableBytes());
            ByteBuf decodeString = this.rc4Codec.encrypt(s);
            out.add(decodeString);
        }
    }
}
