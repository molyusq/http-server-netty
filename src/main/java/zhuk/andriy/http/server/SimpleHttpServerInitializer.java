package zhuk.andriy.http.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;


/**
 * Created by Andrew on 28.03.15.
 */
public class SimpleHttpServerInitializer extends ChannelInitializer<SocketChannel> {

    private static ChannelGroup channelGroup;

    @Override
    public void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        channelGroup = new DefaultChannelGroup(channel.eventLoop());
        channelGroup.add(channel);
        ChannelTrafficShapingHandler channelTrafficShapingHandler = new ChannelTrafficShapingHandler(200);

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(channelTrafficShapingHandler);
        pipeline.addLast(new SimpleHttpServerHandler(channelGroup.size(),
                channelTrafficShapingHandler.trafficCounter()));
    }
}
