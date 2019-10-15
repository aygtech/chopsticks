package com.chopsticks.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class HttpServer {
	
	public static void main(String[] args) {
		
		NioEventLoopGroup accept = new NioEventLoopGroup(1);
		NioEventLoopGroup io = new NioEventLoopGroup();
		try {
			ServerBootstrap server = new ServerBootstrap();
			server.group(accept, io)
				  .channel(NioServerSocketChannel.class)
				  .handler(new LoggingHandler(LogLevel.TRACE))
				  .childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast(new HttpServerCodec());
						pipeline.addLast(new HttpObjectAggregator(1024 * 1024));
						pipeline.addLast(new SimpleChannelInboundHandler<HttpObject>() {
							@Override
							protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
								if(msg instanceof HttpRequest) {
									HttpRequest request = (HttpRequest)msg;
									ByteBuf body = Unpooled.wrappedBuffer("ok".getBytes());
									if("/favicon.ico".equalsIgnoreCase(request.getUri())) {
										body = Unpooled.wrappedBuffer("<link rel=\"shortcut icon\" href=\"http://new.xinwaihui.com/favicon.ico\">".getBytes());
									}
									FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, body);
									response.headers().set("Content-Type", "text/plain");
						            HttpHeaders.setContentLength(response, response.content().readableBytes());
									boolean keepAlive = isKeepAlive(request);
									if(keepAlive) {
										response.headers().set("connection", "keep-alive");
										ctx.write(response);
									}else {
										ctx.write(response).addListener(ChannelFutureListener.CLOSE);
									}
								}else {
									System.err.println(222);
								}
							}
							@Override
							public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
								System.out.println("channelReadComplete");
								ctx.flush();
							}
							private boolean isKeepAlive(HttpRequest request) {
								CharSequence connection = request.headers().get("connection");
						        if (connection != null && "close".equalsIgnoreCase(connection.toString())) {
						            return false;
						        }
						        
						        if (request.getProtocolVersion().isKeepAliveDefault()) {
						            return !"close".equalsIgnoreCase(connection.toString());
						        } else {
						            return "keep-alive".equalsIgnoreCase(connection.toString());
						        }
							}
						});
					}  
				  });
			ChannelFuture bindFuture = server.bind(8080).sync();
			System.out.println("bind ok...");
			bindFuture.channel()
					  .closeFuture()
					  .sync();
			System.out.println("close ok...");
		}catch (Throwable e) {
			e.printStackTrace();
		}finally {
			accept.shutdownGracefully();
			io.shutdownGracefully();
		}
	}
}
