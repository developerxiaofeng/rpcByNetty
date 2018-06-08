package lianxi.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lianxi.common.IMessageHandler;
import lianxi.common.MessageHandlers;
import lianxi.common.MessageInput;
import lianxi.common.MessageRegistry;
//Netty的事件回调类
@Sharable             //标注一个channel handler可以被多个channel安全地共享。
public class MessageCollector extends ChannelInboundHandlerAdapter {

	private final static Logger LOG = LoggerFactory.getLogger(MessageCollector.class);
	//业务线程池
	private ThreadPoolExecutor executor;
	private MessageHandlers handlers;
	private MessageRegistry registry;

	public MessageCollector(MessageHandlers handlers, MessageRegistry registry, int workerThreads) {
		System.out.println("=========2============="+"MessageCollector.构造");
		//业务队列最大1000,避免堆积
		//如果子线程处理不过来,io线程也会加入业务逻辑(callerRunsPolicy)
		BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1000);
		//给业务线程命名
		ThreadFactory factory = new ThreadFactory() {

			AtomicInteger seq = new AtomicInteger();

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("rpc-" + seq.getAndIncrement());
				return t;
			}

		};
		//闲置时间超过30秒的线程就自动销毁
		this.executor = new ThreadPoolExecutor(1, workerThreads, 30, TimeUnit.SECONDS, queue, factory,
				new CallerRunsPolicy());
		this.handlers = handlers;
		this.registry = registry;
	}

	public void closeGracefully() {
		//优雅一点关闭,先通知,再等待,最后强制关闭
		this.executor.shutdown();
		try {
			this.executor.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		this.executor.shutdownNow();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//客户端来了一个新的连接
		LOG.debug("connection comes");
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		//客户端走了一个
		LOG.debug("connection leaves");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof MessageInput) {
			//用业务线程处理消息
			this.executor.execute(() -> {
				this.handleMessage(ctx, (MessageInput) msg);
			});
		}
	}

	private void handleMessage(ChannelHandlerContext ctx, MessageInput input) {
		// 业务逻辑在这里
		Class<?> clazz = registry.get(input.getType());
		if (clazz == null) {
			//没注册的消息用默认的处理器处理
			handlers.defaultHandler().handle(ctx, input.getRequestId(), input);
			return;
		}
		Object o = input.getPayload(clazz);
		//这里有问题
		@SuppressWarnings("unchecked")
		IMessageHandler<Object> handler = (IMessageHandler<Object>) handlers.get(input.getType());
		if (handler != null) {
			handler.handle(ctx, input.getRequestId(), o);
		} else {
			handlers.defaultHandler().handle(ctx, input.getRequestId(), input);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		//此处可能因为客户机器突发重启
		//也可能客户端连接时间超时,后面的REadTimeoutHandle抛出异常
		//也可能消息协议错误,序列化异常
		LOG.warn("connection error", cause);
	}

}
