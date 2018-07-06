package lo5twind;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyShadowSocksLocal {
    // final static Integer PORT = Integer.parseInt(System.getProperty("port", "12345"));

    final public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.printf("Invalid arguments: %d ", args.length);
            System.exit(1);
        }
        final int PORT = Integer.parseInt(args[0]);
        System.out.printf("listening port: %d ", PORT);
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(group)
                     .channel(NioServerSocketChannel.class)
                     .childHandler(new Socks5HandlerInitializer(true));
            bootstrap.bind("0.0.0.0", PORT).sync().channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }

    }


}
