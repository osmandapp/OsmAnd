package net.osmand;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;

import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.location.Location;
import android.util.Xml;

public class DiagnosticsUtilities {
	public final static Log log = LogUtil.getLog(DiagnosticsUtilities.class);

	private final static String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";  //$NON-NLS-1$
	
	public static class DiagnosticsExtensions {
		Map<String, String> extensions = null;
		
		public Map<String, String> getExtensionsToRead() {
			if(extensions == null){
				return Collections.emptyMap();
			}
			return extensions;
		}
		
		public Map<String, String> getExtensionsToWrite() {
			if(extensions == null){
				extensions = new LinkedHashMap<String, String>();
			}
			return extensions;
		}

	}
	
	public static class DiagnosticsData extends DiagnosticsExtensions {
		public String command;
		public String value;
		// by default
		public long time = 0;
		
		public DiagnosticsData() {};

		public DiagnosticsData(String command, String value, long time) {
			this.command = command;
			this.value = value;
			this.time = time;
		}
		
	}
	
	public static class DiagnosticsFile extends DiagnosticsExtensions {
	    public String author;
	    public List<DiagnosticsData> data = new ArrayList<DiagnosticsData>();
	    public String warning = null;
	    public String path = "";

	    public boolean isEmpty() {
	        return data.isEmpty();
	    }
	}
	
	
	public static String writeDiagnosticsFile(File fout, DiagnosticsFile file, Context ctx) {
		try {
			SimpleDateFormat format = new SimpleDateFormat(TIME_FORMAT);
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			FileOutputStream output = new FileOutputStream(fout);
			XmlSerializer serializer = Xml.newSerializer();
			serializer.setOutput(output, "UTF-8"); //$NON-NLS-1$
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true); //$NON-NLS-1$
			serializer.startDocument("UTF-8", true); //$NON-NLS-1$
			serializer.startTag(null, "diagnostics"); //$NON-NLS-1$
			serializer.attribute(null, "version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
			if (file.author == null ){
				serializer.attribute(null, "creator", Version.getAppName(ctx)); //$NON-NLS-1$
			} else {
				serializer.attribute(null, "creator", file.author); //$NON-NLS-1$
			}
			serializer.attribute(null, "xmlns", "http://www.pinpointtech.com/Diagnostics/1/0"); //$NON-NLS-1$ //$NON-NLS-2$
			serializer.attribute(null, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			//serializer.attribute(null, "xsi:schemaLocation", "http://www.pinpointtech.com/Diagnostics/1/0 http://www.pinpointtech.com/Diagnostics/1/0/diagnostics.xsd");
			
			for (DiagnosticsData data : file.data) {
				serializer.startTag(null, "data"); //$NON-NLS-1$
				writeDiagnosticsData(format, serializer, data);
				serializer.endTag(null, "data"); //$NON-NLS-1$
			}
			
			serializer.endTag(null, "diagnostics"); //$NON-NLS-1$
			serializer.flush();
			serializer.endDocument();
		} catch (RuntimeException e) {
			log.error("Error saving diagnostics data", e); //$NON-NLS-1$
			return ctx.getString(R.string.error_occurred_saving_diagnostic_data);
		} catch (IOException e) {
			log.error("Error saving diagnostics data", e); //$NON-NLS-1$
			return ctx.getString(R.string.error_occurred_saving_diagnostic_data);
		}
		return null;
	}
	
	private static void writeNotNullText(XmlSerializer serializer, String tag, String value) throws  IOException {
		if(value != null){
			serializer.startTag(null, tag);
			serializer.text(value);
			serializer.endTag(null, tag);
		}
	}
	
	private static void writeExtensions(XmlSerializer serializer, DiagnosticsExtensions p) throws IOException {
		if (!p.getExtensionsToRead().isEmpty()) {
			serializer.startTag(null, "extensions");
			for (Map.Entry<String, String> s : p.getExtensionsToRead().entrySet()) {
				writeNotNullText(serializer, s.getKey(), s.getValue());
			}
			serializer.endTag(null, "extensions");
		}
	}

	private static void writeDiagnosticsData(SimpleDateFormat format, XmlSerializer serializer, DiagnosticsData data) throws IOException {
		writeNotNullText(serializer, "cmd", data.command);
		writeNotNullText(serializer, "val", data.value);
		if(data.time != 0){
		    writeNotNullText(serializer, "time", format.format(new Date(data.time)));
		}
		writeExtensions(serializer, data);
	}

	// TODO(natashaj): Currently, there are no scenarios for reading and displaying diagnostics data in app.
	//                 It is intended for consumption server-side. Write deserialization code if needed.
}