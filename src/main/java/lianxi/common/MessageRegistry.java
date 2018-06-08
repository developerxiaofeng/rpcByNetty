package lianxi.common;

import java.util.HashMap;
import java.util.Map;
//消息类型注册中心和消息处理器注册中心，
// 都是用静态字段和方法，其实也是为了图方便，写成非静态的可能会优雅一些。
public class MessageRegistry {
	private Map<String, Class<?>> clazzes = new HashMap<>();

	public void register(String type, Class<?> clazz) {
		clazzes.put(type, clazz);
	}

	public Class<?> get(String type) {
		return clazzes.get(type);
	}
}
