package lo5twind;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4JLoggerFactory;

public class Socks5Server {
    // final static Integer PORT = Integer.parseInt(System.getProperty("port", "12345"));

    final public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.printf("Invalid arguments: %d ", args.length);
            System.exit(1);
        }
        final int PORT = Integer.parseInt(args[0]);
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(group)
                     .channel(NioServerSocketChannel.class)
                     .childHandler(new Socks5HandlerInitializer());
            bootstrap.bind("0.0.0.0", PORT).sync().channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }

    }


}
