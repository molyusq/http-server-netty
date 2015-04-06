package zhuk.andriy.http.server;

import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.*;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import zhuk.andriy.http.server.util.*;

import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * Created by Andrew on 28.03.15.
 */
public class SimpleHttpServerHandler extends SimpleChannelInboundHandler<Object> {

    private static RequestUtil requestUtil = new RequestUtil();
    private static RedirectUtil redirectUtil = new RedirectUtil();
    private static ConcurrentHashMap<Channel, ConnectionInfo> connections = new ConcurrentHashMap<Channel, ConnectionInfo>();
    private TrafficCounter trafficCounter;
    private static final int LAST_REQUESTS_TO_SHOW = 16;

    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public SimpleHttpServerHandler(TrafficCounter trafficCounter) {
        this.trafficCounter = trafficCounter;
        //this.trafficCounter.start();

    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object message) throws ExecutionException, InterruptedException {
        if(message instanceof DefaultHttpRequest) {
            String uri = ((DefaultHttpRequest) message).getUri();
            if(connections.get(channelHandlerContext.channel()).getUri() == null)
                connections.get(channelHandlerContext.channel()).setUri(uri);
            if(connections.get(channelHandlerContext.channel()).getUri().equals("/favicon.ico") ||
                    connections.get(channelHandlerContext.channel()).getUri() == null)
                connections.remove(channelHandlerContext.channel());
            QueryStringDecoder decoder = new QueryStringDecoder(uri);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            if(!decoder.path().equals("/favicon.ico")) {
                requestUtil.incrementTotalRequestCount();
                requestUtil.setRequestInfo(((InetSocketAddress) channelHandlerContext.channel().remoteAddress()).getHostName());
            }
            if(decoder.path().equals("/hello")) {
                ScheduledExecutorService service = Executors.newScheduledThreadPool(20);
                ScheduledFuture scheduledFuture = service.schedule(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return createHelloResponse();
                    }
                }, 2, TimeUnit.SECONDS);
                response = (FullHttpResponse)scheduledFuture.get();

            }
            if(decoder.path().startsWith("/redirect")) {
                response = createRedirectResponse(decoder);

            }
            if(decoder.path().equals("/status")) {
                createStatusResponse(channelHandlerContext.channel());
                response = createStatusResponse(channelHandlerContext.channel());
            }
            channelHandlerContext.writeAndFlush(response);
            ConnectionInfo currentInfo = connections.get(channelHandlerContext.channel());
            currentInfo.setDate();
            currentInfo.setReadBytes(trafficCounter.cumulativeReadBytes());
            currentInfo.setSentBytes(trafficCounter.cumulativeWrittenBytes());
            currentInfo.setSpeed(trafficCounter.lastWrittenBytes());

        }
    }

    @Override
    public void channelActive(ChannelHandlerContext channelHandlerContext) throws Exception {
        String address = ((InetSocketAddress)channelHandlerContext.channel().remoteAddress()).getHostName();
        if(!connections.contains(channelHandlerContext.channel()))
            connections.put(channelHandlerContext.channel(), new ConnectionInfo(address));
        channelGroup.add(channelHandlerContext.channel());
        trafficCounter.start();
        super.channelActive(channelHandlerContext);
    }

    @Override
    public void channelInactive(ChannelHandlerContext channelHandlerContext) throws Exception {
        trafficCounter.resetCumulativeTime();
        trafficCounter.stop();
        channelHandlerContext.close();
        super.channelInactive(channelHandlerContext);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable cause) {
        channelHandlerContext.close();
    }

    private FullHttpResponse createHelloResponse() {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.content().writeBytes(Unpooled.copiedBuffer("Hello World", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    private FullHttpResponse createRedirectResponse(QueryStringDecoder decoder) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(LOCATION, "http://" + decoder.parameters().get("url").get(0));
        redirectUtil.setRedirectionItem(decoder.parameters().get("url").get(0));
        return response;
    }

    private FullHttpResponse createStatusResponse(Channel currentChannel) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        StringBuilder buffer = new StringBuilder();
        int channels = 0;
        buffer.append("<html>" + "<body>");
        buffer.append("<p><b>Total requests count:</b> " + requestUtil.getTotalRequestCount() + "</p>");
        buffer.append("<p><b>Unique requests count:</b> " + requestUtil.getUniqueRequestCount() + "</p>");
        showRequestStatistic(buffer);
        showRedirectionStatistic(buffer);
        for(Channel channel : channelGroup) {
            if(channel.isActive())
                channels++;
        }
        buffer.append("<p><b>Active connections:</b> " + channels + "</p>");
        showConnectionStatistics(buffer, currentChannel);
        buffer.append("</body>" + "</html>");
        response.content().writeBytes(Unpooled.copiedBuffer(buffer, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html");
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    private void showRequestStatistic(StringBuilder buffer) {
        if(requestUtil.getUniqueRequestCount() != 0) {
            buffer.append("<p>Request per IP:</p>");
            buffer.append("<table border=\"1\">");
            buffer.append("<tr><th>IP</th>" + "<th>Request Quantity</th>" +
                "<th>Last Request Time</th></tr>");
            for(String address : requestUtil.getAllAddresses()) {
                buffer.append("<tr><td>" +address + "</td><td>" + requestUtil.getInfo(address).getCount() + "</td><td>" +
                        requestUtil.getInfo(address).getDate() + "</td></tr>");
            }
            buffer.append("</table>");
        }
    }

    private void showRedirectionStatistic(StringBuilder buffer) {
        if(redirectUtil.getAllURLs().size() != 0) {
            buffer.append("<p>Redirections:</p>");
            buffer.append("<table border=\"1\">");
            buffer.append("<tr><th>URL</th>" + "<th>Redirection quantity</th></tr>");
            for(String  url : redirectUtil.getAllURLs()) {
                buffer.append("<tr><td>" + url + "</td><td>" + redirectUtil.getRedirectionCountByURL(url) + "</td></tr>");
            }
            buffer.append("</table>");
        }
    }


    private void showOneConnectionStat(StringBuilder buffer, ConnectionInfo info) {
        buffer.append("<tr><td>" + info.getAddress() + "</td><td>" + info.getUri() + "</td><td>" + info.getDate()
                + "</td><td>" + info.getSentBytes() + "</td><td>" + info.getReadBytes() + "</td><td>"
                + info.getSpeed() + "</td></tr>");
    }

    private void showConnectionStatistics(StringBuilder buffer, Channel currentChannel) {
        List<Channel> channelList = new ArrayList<Channel>(connections.keySet());
        if(channelList.size() != 0) {
            ConnectionInfo currentInfo = connections.get(currentChannel);
            currentInfo.setReadBytes(trafficCounter.cumulativeReadBytes());
            currentInfo.setSentBytes(trafficCounter.cumulativeWrittenBytes());
            currentInfo.setDate();
            currentInfo.setSpeed(trafficCounter.lastWrittenBytes());
            buffer.append("<p>Connections:</p>");
            buffer.append("<table border=\"1\">");
            buffer.append("<tr><th>src_ip</th>" + "<th>URI</th>" + "<th>Timestamp</th></th>" +
                    "<th>sent_bytes</th>" + "<th>received_bytes</th>" + "<th>speed, bytes/s</th></tr>");
            if(channelList.size() < LAST_REQUESTS_TO_SHOW) {
                for(Channel channel: channelList) {
                    ConnectionInfo info = connections.get(channel);
                    showOneConnectionStat(buffer, info);
                }
            } else {
                int size = channelList.size() - 1;
                for(int i = size; i > size - LAST_REQUESTS_TO_SHOW + 1; i--) {
                    ConnectionInfo info = connections.get(channelList.get(i));
                    showOneConnectionStat(buffer, info);
                }
            }
            buffer.append("</table>");
        }
    }
}
