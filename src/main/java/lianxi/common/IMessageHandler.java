package lianxi.common;

import io.netty.channel.ChannelHandlerContext;
//消息处理器接口，每个自定义服务必须实现handle方法
@FunctionalInterface
public interface IMessageHandler<T> {

	void handle(ChannelHandlerContext ctx, String requestId, T message);

}
