package net.osmand;

import android.util.Xml;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PlatformUtil {

	public static String TAG = "net.osmand";

	private static class OsmandLogImplementation implements Log {

		private final String fullName;
		private final String name;

		public OsmandLogImplementation(String name) {
			this.fullName = name;
			this.name = fullName.substring(fullName.lastIndexOf('.') + 1);
		}

		@Override
		public void trace(Object message) {
			if (isTraceEnabled()) {
				android.util.Log.d(TAG, name + " " + message);
			}
		}

		@Override
		public void trace(Object message, Throwable t) {
			if (isTraceEnabled()) {
				android.util.Log.d(TAG, name + " " + message, t);
			}
		}

		@Override
		public void debug(Object message) {
			if (isDebugEnabled()) {
				android.util.Log.d(TAG, name + " " + message);
			}
		}

		@Override
		public void debug(Object message, Throwable t) {
			if (isDebugEnabled()) {
				android.util.Log.d(TAG, name + " " + message, t);
			}
		}

		@Override
		public void error(Object message) {
			if (isErrorEnabled()) {
				android.util.Log.e(TAG, name + " " + message);
			}
		}

		@Override
		public void error(Object message, Throwable t) {
			if (isErrorEnabled()) {
				android.util.Log.e(TAG, name + " " + message, t);
			}
		}

		@Override
		public void fatal(Object message) {
			if (isFatalEnabled()) {
				android.util.Log.e(TAG, name + " " + message);
			}

		}

		@Override
		public void fatal(Object message, Throwable t) {
			if (isFatalEnabled()) {
				android.util.Log.e(TAG, name + " " + message, t);
			}
		}

		@Override
		public void info(Object message) {
			if (isInfoEnabled()) {
				android.util.Log.i(TAG, name + " " + message);
			}
		}

		@Override
		public void info(Object message, Throwable t) {
			if (isInfoEnabled()) {
				android.util.Log.i(TAG, name + " " + message, t);
			}
		}

		@Override
		public boolean isTraceEnabled() {
			return android.util.Log.isLoggable(TAG, android.util.Log.VERBOSE);
		}


		@Override
		public boolean isDebugEnabled() {
			// For debug purposes always true
			// return android.util.Log.isLoggable(TAG, android.util.Log.DEBUG);
			return true;
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
		public boolean isWarnEnabled() {
			return android.util.Log.isLoggable(TAG, android.util.Log.WARN);
		}

		@Override
		public void warn(Object message) {
			if (isWarnEnabled()) {
				android.util.Log.w(TAG, name + " " + message);
			}
		}

		@Override
		public void warn(Object message, Throwable t) {
			if (isWarnEnabled()) {
				android.util.Log.w(TAG, name + " " + message, t);
			}
		}
	}

	public static Log getLog(String name) {
		return new OsmandLogImplementation(name);
	}

	public static Log getLog(Class<?> cl) {
		return getLog(cl.getName());
	}

	public static XmlPullParser newXMLPullParser() throws XmlPullParserException {
		return Xml.newPullParser();
	}

	public static XmlSerializer newSerializer() {
		return Xml.newSerializer();
	}
}
