package lianxi.common;

import com.alibaba.fastjson.JSON;

//定义消息输入输出格式，消息类型、消息唯一ID和消息的json序列化字符串内容。
// 消息唯一ID是用来客户端验证服务器请求和响应是否匹配。
public class MessageInput {
	private String type;
	private String requestId;
	private String payload;

	public MessageInput(String type, String requestId, String payload) {
		this.type = type;
		this.requestId = requestId;
		this.payload = payload;
	}

	public String getType() {
		return type;
	}

	public String getRequestId() {
		return requestId;
	}
//因为我们想直接拿到对象,所以要提供对象的类型参数
	public <T> T getPayload(Class<T> clazz) {
		if (payload == null) {
			return null;
		}
		return JSON.parseObject(payload, clazz);
	}

}
