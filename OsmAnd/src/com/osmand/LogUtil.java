package com.osmand;

import org.apache.commons.logging.Log;

/**
 * That class is replacing of standard LogFactory due to 
 * problems with Android implementation of LogFactory.
 * 
 * 1. It is impossible to replace standard LogFactory (that is hidden in android.jar)
 * 2. Implementation of LogFactory always creates Logger.getLogger(String name)
 * 3. + It is possible to enable logger level by calling 
 * 		Logger.getLogger("com.osmand").setLevel(Level.ALL);
 * 4. Logger goes to low level android.util.Log where android.util.Log#isLoggable(String, int) is checked
 *    String tag -> is string of length 23 (stripped full class name)
 * 5. It is impossible to set for all tags debug level (info is default) - android.util.Log#isLoggable(String, int).
 *  
 */
public class LogUtil {
	public static String TAG = "com.osmand";
	private static class OsmandLogImplementation implements Log {
		
		private final String fullName;
		private final String name;

		public OsmandLogImplementation(String name){
			this.fullName = name;
			this.name = fullName.substring(fullName.lastIndexOf('.') + 1);
		}
		@Override
		public void debug(Object message) {
			if(isDebugEnabled()){
				android.util.Log.d(TAG, name + " " + message);
			}
		}

		@Override
		public void debug(Object message, Throwable t) {
			if(isDebugEnabled()){
				android.util.Log.d(TAG, name + " " + message, t);
			}
		}

		@Override
		public void error(Object message) {
			if(isErrorEnabled()){
				android.util.Log.e(TAG, name + " " + message);
			}
		}

		@Override
		public void error(Object message, Throwable t) {
			if(isErrorEnabled()){
				android.util.Log.e(TAG, name + " " + message, t);
			}
		}

		@Override
		public void fatal(Object message) {
			if(isFatalEnabled()){
				android.util.Log.e(TAG, name + " " + message);
			}
			
		}

		@Override
		public void fatal(Object message, Throwable t) {
			if(isFatalEnabled()){
				android.util.Log.e(TAG, name + " " + message, t);
			}
		}

		@Override
		public void info(Object message) {
			if(isInfoEnabled()){
				android.util.Log.i(TAG, name + " " + message);
			}
		}

		@Override
		public void info(Object message, Throwable t) {
			if(isInfoEnabled()){
				android.util.Log.i(TAG, name + " " + message, t);
			}
		}

		@Override
		public boolean isDebugEnabled() {
			return android.util.Log.isLoggable(TAG, android.util.Log.DEBUG);
		}

		@Override
		public boolean isErrorEnabled() {
			return android.util.Log.isLoggable(TAG, android.util.Log.ERROR);
		}

		@Override
		public boolean isFatalEnabled() {
			return android.util.Log.isLoggable(TAG, android.util.Log.ERROR);
		}

		@Override
		public boolean isInfoEnabled() {
			return android.util.Log.isLoggable(TAG, android.util.Log.INFO);
		}

		@Override
		public boolean isTraceEnabled() {
			return android.util.Log.isLoggable(TAG, android.util.Log.DEBUG);
		}

		@Override
		public boolean isWarnEnabled() {
			return android.util.Log.isLoggable(TAG, android.util.Log.WARN);
		}

		@Override
		public void trace(Object message) {
			if(isTraceEnabled()){
				android.util.Log.d(TAG, name + " " + message);
			}
		}

		@Override
		public void trace(Object message, Throwable t) {
			if(isTraceEnabled()){
				android.util.Log.d(TAG, name + " " + message, t);
			}	
		}

		@Override
		public void warn(Object message) {
			if(isWarnEnabled()){
				android.util.Log.w(TAG, name + " " + message);
			}
		}

		@Override
		public void warn(Object message, Throwable t) {
			if(isWarnEnabled()){
				android.util.Log.w(TAG, name + " " + message, t);
			}
		}
	}
	
	public static Log getLog(String name){
		return new OsmandLogImplementation(name);
	}
	
	public static Log getLog(Class<?> cl){
		return getLog(cl.getName());
	}
}
