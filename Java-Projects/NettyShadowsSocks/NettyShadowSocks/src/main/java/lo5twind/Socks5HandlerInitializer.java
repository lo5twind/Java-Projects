package lo5twind;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public final class Socks5HandlerInitializer extends ChannelInitializer<SocketChannel> {

    private boolean isLocal;

    public Socks5HandlerInitializer(boolean isLocal) {
        this.isLocal = isLocal;
    }

    public Socks5HandlerInitializer() {
        this.isLocal = true;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        if (isLocal) {
            socketChannel.pipeline().addLast(
                    new LoggingHandler(LogLevel.INFO),
                    new Socks5RequestDecoder()
            );
        } else { // at shadowsocks side
            socketChannel.pipeline().addLast(
                    new LoggingHandler(LogLevel.INFO),
                    new Socks5RequestDecoder(Socks5RequestDecoder.Stat.INIT_CONNECT),
                    new Socks5ServerConnectHandler(isLocal=false)
            );
        }
    }
}
