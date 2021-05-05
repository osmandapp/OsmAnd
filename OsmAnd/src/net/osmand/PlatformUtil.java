package net.osmand;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * That class is replacing of standard LogFactory due to 
 * problems with Android implementation of LogFactory.
 * 
 * 1. It is impossible to replace standard LogFactory (that is hidden in android.jar)
 * 2. Implementation of LogFactory always creates Logger.getLogger(String name)
 * 3. + It is possible to enable logger level by calling 
 * 		Logger.getLogger("net.osmand").setLevel(Level.ALL);
 * 4. Logger goes to low level android.util.Log where android.util.Log#isLoggable(String, int) is checked
 *    String tag -> is string of length 23 (stripped full class name)
 * 5. It is impossible to set for all tags debug level (info is default) - android.util.Log#isLoggable(String, int).
 *  
 */
public class PlatformUtil {

	public static String TAG = "net.osmand"; //$NON-NLS-1$

	private static WeakReference<Context> ctxRef;

	private static class OsmandLogImplementation implements Log {
		
		private final String fullName;
		private final String name;

		public OsmandLogImplementation(String name){
			this.fullName = name;
			this.name = fullName.substring(fullName.lastIndexOf('.') + 1);
		}
		
		@Override
		public void trace(Object message) {
			if(isTraceEnabled()){
				android.util.Log.d(TAG, name + " " + message); //$NON-NLS-1$
			}
		}
		
		@Override
		public void trace(Object message, Throwable t) {
			if(isTraceEnabled()){
				android.util.Log.d(TAG, name + " " + message, t); //$NON-NLS-1$
			}
		}

		
		
		@Override
		public void debug(Object message) {
			if(isDebugEnabled()){
				android.util.Log.d(TAG, name + " " + message); //$NON-NLS-1$
			}
		}

		@Override
		public void debug(Object message, Throwable t) {
			if(isDebugEnabled()){
				android.util.Log.d(TAG, name + " " + message, t); //$NON-NLS-1$
			}
		}

		@Override
		public void error(Object message) {
			if(isErrorEnabled()){
				android.util.Log.e(TAG, name + " " + message); //$NON-NLS-1$
			}
		}

		@Override
		public void error(Object message, Throwable t) {
			if(isErrorEnabled()){
				android.util.Log.e(TAG, name + " " + message, t); //$NON-NLS-1$
			}
		}

		@Override
		public void fatal(Object message) {
			if(isFatalEnabled()){
				android.util.Log.e(TAG, name + " " + message); //$NON-NLS-1$
			}
			
		}

		@Override
		public void fatal(Object message, Throwable t) {
			if(isFatalEnabled()){
				android.util.Log.e(TAG, name + " " + message, t); //$NON-NLS-1$
			}
		}

		@Override
		public void info(Object message) {
			if(isInfoEnabled()){
				android.util.Log.i(TAG, name + " " + message); //$NON-NLS-1$
			}
		}

		@Override
		public void info(Object message, Throwable t) {
			if(isInfoEnabled()){
				android.util.Log.i(TAG, name + " " + message, t); //$NON-NLS-1$
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
			if(isWarnEnabled()){
				android.util.Log.w(TAG, name + " " + message); //$NON-NLS-1$
			}
		}

		@Override
		public void warn(Object message, Throwable t) {
			if(isWarnEnabled()){
				android.util.Log.w(TAG, name + " " + message, t); //$NON-NLS-1$
			}
		}
	}
	
	public static Log getLog(String name){
		return new OsmandLogImplementation(name);
	}
	
	public static Log getLog(Class<?> cl){
		return getLog(cl.getName());
	}
	
	public static XmlPullParser newXMLPullParser() throws XmlPullParserException {
		// return XmlPullParserFactory.newInstance().newPullParser();
		return Xml.newPullParser();
	}
	
	public static XmlSerializer newSerializer() {
		return Xml.newSerializer();
	}

	public static void setContext(@Nullable Context ctx) {
		if (ctx == null) {
			ctxRef = null;
		} else {
			ctxRef = new WeakReference<>(ctx);
		}
	}

	@NonNull
	public static FileInputStream getFileInputStream(@NonNull String path) throws FileNotFoundException {
		return getFileInputStream(new File(path));
	}

	@NonNull
	public static FileInputStream getFileInputStream(@NonNull File file) throws FileNotFoundException {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
			Context ctx = ctxRef != null ? ctxRef.get() : null;
			if (ctx == null) {
				throw new FileNotFoundException("Cannot create FileInputStream for " + file + ". Empty context.");
			} else {
				OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
				if (app.getSettings().isScopedStorageInUse(ctx)) {
					DocumentFile docFile = DocumentFileCompat.fromFullPath(ctx, file.getAbsolutePath());
					if (docFile != null) {
						Uri uri = docFile.getUri();
						ParcelFileDescriptor r = ctx.getContentResolver().openFileDescriptor(uri, "r");
						return new AutoCloseInputStream(r);
					} else {
						throw new FileNotFoundException("Cannot create FileInputStream for " + file + ". DocumentFile is null.");
					}
				}
			}
		}
		return new FileInputStream(file);
	}
}
