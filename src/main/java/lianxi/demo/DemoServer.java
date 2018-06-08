package lianxi.demo;

import java.util.ArrayList;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import lianxi.common.IMessageHandler;
import lianxi.common.MessageOutput;
import lianxi.server.RPCServer;

//斐波那契和指数计算处理
class FibRequestHandler implements IMessageHandler<Integer> {

	private List<Long> fibs = new ArrayList<>();

	{
		fibs.add(1L); // fib(0) = 1
		fibs.add(1L); // fib(1) = 1
	}

	@Override
	public void handle(ChannelHandlerContext ctx, String requestId, Integer n) {
		for (int i = fibs.size(); i < n + 1; i++) {
			long value = fibs.get(i - 2) + fibs.get(i - 1);
			fibs.add(value);
		}
		//响应输出
		ctx.writeAndFlush(new MessageOutput(requestId, "fib_res", fibs.get(n)));
	}

}

class ExpRequestHandler implements IMessageHandler<ExpRequest> {

	@Override
	public void handle(ChannelHandlerContext ctx, String requestId, ExpRequest message) {
		int base = message.getBase();
		int exp = message.getExp();
		long start = System.nanoTime();
		long res = 1;
		for (int i = 0; i < exp; i++) {
			res *= base;
		}
		long cost = System.nanoTime() - start;
		//响应输出
		ctx.writeAndFlush(new MessageOutput(requestId, "exp_res", new ExpResponse(res, cost)));
	}

}

//构建RPC服务器
//RPC服务类要监听指定IP端口，设定io线程数和业务计算线程数，
//然后注册斐波那契服务输入类和指数服务输入类，还有相应的计算处理器。
public class DemoServer {

	public static void main(String[] args) {
		RPCServer server = new RPCServer("localhost", 8888, 2, 16);
		server.service("fib", Integer.class, new FibRequestHandler()).service("exp", ExpRequest.class,
				new ExpRequestHandler());
		server.start();
	}

}
