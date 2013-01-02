package net.osmand;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;

public class ReflectionUtils {
	private static final Log log = LogUtil.getLog(ReflectionUtils.class);
	
	public static int callIntMethod(Object o, Class cl, String methodName, int defValue, Class[] parameterTypes, 
			Object... args){
		try {
			Method m = cl.getDeclaredMethod(methodName, parameterTypes);
			Integer i = (Integer) m.invoke(o, args);
			return i.intValue();
		} catch (Exception e) {
			log.debug("Reflection fails " + e.getMessage(), e);
		}
		return defValue;
	}

}
