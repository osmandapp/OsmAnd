package org.apache.commons.logging;

public class LogFactory {
	public static String TAG = "com.osmand";
	
	public static Log getLog(Class<?> cl){
		final String name = cl.getName();
		return new Log() {
			@Override
			public void debug(Object message) {
				android.util.Log.d(TAG, name + " " + message);
				
			}

			@Override
			public void debug(Object message, Throwable t) {
				android.util.Log.d(TAG, name + " " + message, t);
			}

			@Override
			public void error(Object message) {
				android.util.Log.e(TAG, name + " " + message);
			}

			@Override
			public void error(Object message, Throwable t) {
				android.util.Log.e(TAG, name + " " + message, t);
			}

			@Override
			public void fatal(Object message) {
				android.util.Log.e(TAG, name + " " + message);
			}

			@Override
			public void fatal(Object message, Throwable t) {
				android.util.Log.e(TAG, name + " " + message, t);
			}

			@Override
			public void info(Object message) {
				android.util.Log.i(TAG, name + " " + message);				
			}

			@Override
			public void info(Object message, Throwable t) {
				android.util.Log.i(TAG, name + " " + message, t);
				
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
				android.util.Log.d(TAG, name + " " + message);
			}

			@Override
			public void trace(Object message, Throwable t) {
				android.util.Log.d(TAG, name + " " + message, t);				
			}

			@Override
			public void warn(Object message) {
				android.util.Log.w(TAG, name + " " + message);
				
			}

			@Override
			public void warn(Object message, Throwable t) {
				android.util.Log.w(TAG, name + " " + message, t);
			}
			
		};
	}

}
