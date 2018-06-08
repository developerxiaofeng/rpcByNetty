package lianxi.common;

import java.util.UUID;

public class RequestId {
	//简单UUID 64
	public static String next() {
		return UUID.randomUUID().toString();
	}

}
