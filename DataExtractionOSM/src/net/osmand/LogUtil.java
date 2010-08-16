package net.osmand;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * That class is replacing of standard LogFactory due to 
 * problems with Android implementation of LogFactory.
 * See Android analog of  LogUtil
 *
 * That class should be very simple & always use LogFactory methods,
 * there is an intention to delegate all static methods to LogFactory.
 */
public class LogUtil {
	
	public static Log getLog(Class<?> cl){
		return LogFactory.getLog(cl);
	}

}
