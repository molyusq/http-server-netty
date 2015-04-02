package zhuk.andriy.http.server;

import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.CharsetUtil;
import zhuk.andriy.http.server.util.*;

import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * Created by Andrew on 28.03.15.
 */
public class SimpleHttpServerHandler extends SimpleChannelInboundHandler<Object> {

    private static RequestUtil requestUtil = new RequestUtil();
    private static RedirectUtil redirectUtil = new RedirectUtil();
    private static ConnectionUtil connectionUtil = new ConnectionUtil();
    private TrafficCounter trafficCounter;
    private static final int LAST_REQUESTS_TO_SHOW = 16;

    private int connections = 0;

    public SimpleHttpServerHandler(int connections, TrafficCounter trafficCounter) {
        this.connections = connections;
        this.trafficCounter = trafficCounter;
        this.trafficCounter.start();

    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object message) throws ExecutionException, InterruptedException {
        if(message instanceof DefaultHttpRequest) {
            connectionUtil.addConnection(((InetSocketAddress)channelHandlerContext.channel().remoteAddress()).getHostName(),
                    ((DefaultHttpRequest) message).getUri());
            QueryStringDecoder decoder = new QueryStringDecoder(((DefaultHttpRequest)message).getUri());
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
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
                response = createStatusResponse();
            }
            if(!decoder.path().equals("/favicon.ico")) {
                requestUtil.incrementTotalRequestCount();
                requestUtil.setRequestInfo(((InetSocketAddress) channelHandlerContext.channel().remoteAddress()).getHostName());
            }
            channelHandlerContext.writeAndFlush(response);
        }

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

    private FullHttpResponse createStatusResponse() {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        StringBuilder buffer = new StringBuilder();
        buffer.append("<html>" + "<body>");
        buffer.append("<p><b>Total requests count:</b> " + requestUtil.getTotalRequestCount() + "</p>");
        buffer.append("<p><b>Unique requests count:</b> " + requestUtil.getUniqueRequestCount() + "</p>");
        showRequestStatistic(buffer);
        showRedirectionStatistic(buffer);
        buffer.append("<p><b>Active connections:</b> " + connections + "</p>");
        showConnectionStatistics(buffer);
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

    private void showConnectionStatistics(StringBuilder buffer) {
        if(connectionUtil.getConnectionQuantity() != 0) {
            buffer.append("<p>Connections:</p>");
            buffer.append("<table border=\"1\">");
            buffer.append("<tr><th>src_ip</th>" + "<th>URI</th>" + "<th>Timestamp</th></th>" +
                    "<th>sent_bytes</th>" + "<th>received_bytes</th>" + "<th>speed, bytes</th></tr>");
            if(connectionUtil.getConnectionQuantity() < LAST_REQUESTS_TO_SHOW) {
                for(String  ip : connectionUtil.getAllIPs()) {
                    ConnectionInfo info = connectionUtil.getConnectionInfo(ip);
                    buffer.append("<tr><td>" + ip + "</td><td>" + info.getUri() + "</td><td>" + info.getDate()
                            + "</td><td>" + trafficCounter.cumulativeWrittenBytes() + "</td><td>"
                            + trafficCounter.cumulativeReadBytes() + "</td><td>"
                            + trafficCounter.cumulativeWrittenBytes()/trafficCounter.lastCumulativeTime() + "</td></tr>");
                }
            } else {
                List<String> ipList = new ArrayList<String>(connectionUtil.getAllIPs());
                for(int i=0; i < LAST_REQUESTS_TO_SHOW; i++) {
                    ConnectionInfo info = connectionUtil.getConnectionInfo(ipList.get(i));
                    buffer.append("<tr><td>" + ipList.get(i) + "</td><td>" + info.getUri() + "</td><td>" + info.getDate()
                            + "</td><td>" + trafficCounter.cumulativeWrittenBytes() + "</td><td>"
                            + trafficCounter.cumulativeReadBytes() + "</td><td>"
                            + trafficCounter.lastWrittenBytes() + "</td></tr>");
                }
            }
            buffer.append("</table>");
        }
    }
}
