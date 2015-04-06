package zhuk.andriy.http.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;


/**
 * Created by Andrew on 28.03.15.
 */
public class SimpleHttpServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        ChannelTrafficShapingHandler channelTrafficShapingHandler = new ChannelTrafficShapingHandler(200);
        pipeline.addLast(channelTrafficShapingHandler);
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new SimpleHttpServerHandler(channelTrafficShapingHandler.trafficCounter()));
    }
}
