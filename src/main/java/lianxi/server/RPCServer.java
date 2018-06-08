package lianxi.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lianxi.common.IMessageHandler;
import lianxi.common.MessageDecoder;
import lianxi.common.MessageEncoder;
import lianxi.common.MessageHandlers;
import lianxi.common.MessageRegistry;

public class RPCServer {

	private final static Logger LOG = LoggerFactory.getLogger(RPCServer.class);

	private String ip;
	private int port;
	private int ioThreads;  //用来处理网络流的读写线程
	private int workerThreads;  //用来业务处理的计算线程
	private MessageHandlers handlers = new MessageHandlers();
	private MessageRegistry registry = new MessageRegistry();

	{
		handlers.defaultHandler(new DefaultHandler());
	}

	public RPCServer(String ip, int port, int ioThreads, int workerThreads) {
		this.ip = ip;
		this.port = port;
		this.ioThreads = ioThreads;
		this.workerThreads = workerThreads;
	}

	private ServerBootstrap bootstrap;
	private EventLoopGroup group;
	private MessageCollector collector;
	private Channel serverChannel;
	//注册服务的快捷方式
	public RPCServer service(String type, Class<?> reqClass, IMessageHandler<?> handler) {
		registry.register(type, reqClass);
		handlers.register(type, handler);
		return this;
	}
	//启动RPC服务
	public void start() {
		System.out.println("=========1============="+"RPCServer.start");
		//启动NIO服务的辅助启动类
		bootstrap = new ServerBootstrap();
		//用来接收进来的连接
		group = new NioEventLoopGroup(ioThreads);
		bootstrap.group(group);
		collector = new MessageCollector(handlers, registry, workerThreads);
		MessageEncoder encoder = new MessageEncoder();
		//配置Channel
		bootstrap.channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				//注册hander
				ChannelPipeline pipe = ch.pipeline();
				//如果客户端60秒没有任何请求,就关闭客户端连接
				pipe.addLast(new ReadTimeoutHandler(60));
				//加解码器
				pipe.addLast(new MessageDecoder());
				//编码器
				pipe.addLast(encoder);
				//将业务处理器放到最后
				pipe.addLast(collector);
			}
		});
		bootstrap.option(ChannelOption.SO_BACKLOG, 100)  //客户端套接字默认接受队列的大小
				.option(ChannelOption.SO_REUSEADDR, true) //reuse addr 避免端口冲突
				.option(ChannelOption.TCP_NODELAY, true)  //关闭小流合并,保证消息的及时性
				.childOption(ChannelOption.SO_KEEPALIVE, true);  //长时间没动静的连接自动关闭
		//绑定端口,开始接收进来的连接
		serverChannel = bootstrap.bind(this.ip, this.port).channel();

		LOG.warn("server started @ {}:{}\n", ip, port);
	}

	public void stop() {
		// 先关闭服务端套件字
		serverChannel.close();
		// 再斩断消息来源，停止io线程池
		group.shutdownGracefully();
		// 最后停止业务线程
		collector.closeGracefully();
	}

}