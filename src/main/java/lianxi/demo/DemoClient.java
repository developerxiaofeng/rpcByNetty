package lianxi.demo;

import lianxi.client.RPCClient;
import lianxi.client.RPCException;
//构建RPC客户端
public class DemoClient {

	private RPCClient client;

	public DemoClient(RPCClient client) {
		this.client = client;
		this.client.rpc("fib_res", Long.class).rpc("exp_res", ExpResponse.class);
	}

	public long fib(int n) {
		return (Long) client.send("fib", n);
	}

	public ExpResponse exp(int base, int exp) {
		return (ExpResponse) client.send("exp", new ExpRequest(base, exp));
	}
//RPC客户端要链接远程IP端口，并注册服务输出类(RPC响应类)，
// 然后分别调用20次斐波那契服务和指数服务，输出结果

	public static void main(String[] args) throws InterruptedException {
		RPCClient client = new RPCClient("localhost", 8888);
		DemoClient demo = new DemoClient(client);
		for (int i = 0; i < 30; i++) {
			try {
				System.out.printf("fib(%d) = %d\n", i, demo.fib(i));
				Thread.sleep(100);
			} catch (RPCException e) {
				i--; // retry
			}
		}
		Thread.sleep(3000);
		for (int i = 0; i < 30; i++) {
			try {
				ExpResponse res = demo.exp(2, i);
				Thread.sleep(100);
				System.out.printf("exp2(%d) = %d cost=%dns\n", i, res.getValue(), res.getCostInNanos());
			} catch (RPCException e) {
				i--; // retry
			}
		}

		client.close();
	}

}
