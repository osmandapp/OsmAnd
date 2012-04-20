package net.osmand;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

import net.osmand.plus.R;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.location.Location;
import android.util.Xml;

public class GPXUtilities {
	public final static Log log = LogUtil.getLog(GPXUtilities.class);

	private final static String GPX_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";  //$NON-NLS-1$
	
	private final static NumberFormat latLonFormat = new DecimalFormat("0.00#####", new DecimalFormatSymbols(Locale.US));
	
	public static class GPXExtensions {
		Map<String, String> extensions = null;
		protected boolean extensionReadMode;
		
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

		public void parse(XmlPullParser parser, Stack<GPXExtensions> parserState, SimpleDateFormat format) throws XmlPullParserException, IOException {
			String tag = parser.getName();
			if (extensionReadMode) {
				String value = readText(parser, tag);
				if (value != null) {
					getExtensionsToWrite().put(tag, value);
				}
			}
			if (tag.equals("extensions" )) {
				extensionReadMode = true;
			}
		}

		public final boolean parseEnd(String tag, Stack<GPXExtensions> parserState) {
			if (tag.equals("extensions") && extensionReadMode) {
				extensionReadMode = false;
				return true;
			} else {
				if (isEndingTag(tag)) {
					parserState.pop();
					return true; 
				} else {
					return false;
				}
			}
		}

		protected boolean isEndingTag(String tag) {
			return false;
		}
		
	}
	
	public static class WptPt extends GPXExtensions {
		public double lat;
		public double lon;
		public String name = null;
		public String category = null;
		public String desc = null;
		// by default
		public long time = 0;
		public double ele = Double.NaN;
		public double speed = 0;
		public double hdop = Double.NaN;
		
		public WptPt() {};

		public WptPt(double lat, double lon, long time, double ele, double speed, double hdop) {
			this.lat = lat;
			this.lon = lon;
			this.time = time;
			this.ele = ele;
			this.speed = speed;
			this.hdop = hdop;
		}
		
		@Override
		public void parse(XmlPullParser parser, Stack<GPXExtensions> parserState, SimpleDateFormat format)
				throws XmlPullParserException, IOException {
			super.parse(parser, parserState, format);
			String tag = parser.getName();
			if (tag.equals("name")) {
				name = readText(parser, "name");
			} else if (tag.equals("category")) {
				category = readText(parser, "category");
			} else if (tag.equals("desc")) {
				desc = readText(parser, "desc");
			} else if (tag.equals("ele")) {
				String text = readText(parser, "ele");
				if (text != null) {
					try {
						ele = Float.parseFloat(text);
					} catch (NumberFormatException e) {
					}
				}
			} else if (tag.equals("hdop")) {
				String text = readText(parser, "hdop");
				if (text != null) {
					try {
						hdop = Float.parseFloat(text);
					} catch (NumberFormatException e) {
					}
				}
			} else if (tag.equals("time")) {
				String text = readText(parser, "time");
				if (text != null) {
					try {
						time = format.parse(text).getTime();
					} catch (ParseException e) {
					}
				}
			} else if (tag.equals("speed") && extensionReadMode) {
				try {
					speed = Float.parseFloat(getExtensionsToWrite().get(tag));
				} catch (NumberFormatException e) {
				}
			}
		}
		
		@Override
		public boolean isEndingTag(String tag) {
			return tag.equals("trkpt") || tag.equals("wpt") || tag.equals("rtept");
		}
	}
	
	public static class TrkSegment extends GPXExtensions {
		public List<WptPt> points = new ArrayList<WptPt>();
		
		@Override
		public void parse(XmlPullParser parser, Stack<GPXExtensions> parserState, SimpleDateFormat format)
				throws XmlPullParserException, IOException {
			super.parse(parser, parserState, format);
			if (parser.getName().equals("trkpt")) {
				WptPt wptPt = parseWptAttributes(parser);
				points.add(wptPt);
				parserState.push(wptPt);
			}
		}
		
		@Override
		protected boolean isEndingTag(String tag) {
			return tag.equals("trkseg");
		}
	}
	
	public static class Track extends GPXExtensions {
		public String name = null;
		public String desc = null;
		public List<TrkSegment> segments = new ArrayList<TrkSegment>();
		
		@Override
		public void parse(XmlPullParser parser, Stack<GPXExtensions> parserState, SimpleDateFormat format)
				throws XmlPullParserException, IOException {
			super.parse(parser, parserState, format);
			String tag = parser.getName();
			if (tag.equals("name")) {
				name = readText(parser, "name");
			} else if (tag.equals("desc")) {
				desc = readText(parser, "desc");
			} else 	if (tag.equals("trkseg")) {
				TrkSegment trkSeg = new TrkSegment();
				segments.add(trkSeg);
				parserState.push(trkSeg);
			}
		}
		
		@Override
		public boolean isEndingTag(String tag) {
			return tag.equals("trk");
		}
	}
	
	public static class Route extends GPXExtensions {
		public String name = null;
		public String desc = null;
		public List<WptPt> points = new ArrayList<WptPt>();
		
		@Override
		public void parse(XmlPullParser parser, Stack<GPXExtensions> parserState, SimpleDateFormat format) 
				throws XmlPullParserException, IOException 
		{
			super.parse(parser, parserState, format);
			String tag = parser.getName();
			if (tag.equals("name")) {
				name = readText(parser, "name");
			} else if (tag.equals("desc")) {
				desc = readText(parser, "desc");
			} else if (tag.equals("rtept")) {
				WptPt wptPt = parseWptAttributes(parser);
				points.add(wptPt);
				parserState.push(wptPt);
			}
		}
		
		@Override
		public boolean isEndingTag(String tag) {
			return tag.equals("trk");
		}
	}

	public static class Extensions extends GPXExtensions {
		
	}
	
	public static class GPXFile extends GPXExtensions {
		public String author;
		public List<Track> tracks = new ArrayList<Track>();
		public List<WptPt> points = new ArrayList<WptPt>();
		public List<Route> routes = new ArrayList<Route>();
		public String warning = null;
		public String path = "";
		
		public boolean isCloudmadeRouteFile(){
			return "cloudmade".equalsIgnoreCase(author);
		}
		
		public WptPt findPointToShow(){
			for(Track t : tracks){
				for(TrkSegment s : t.segments){
					if(s.points.size() > 0){
						return s.points.get(0);
					}
				}
			}
			for (Route s : routes) {
				if (s.points.size() > 0) {
					return s.points.get(0);
				}
			}
			if (points.size() > 0) {
				return points.get(0);
			}
			return null;
		}

		public boolean isEmpty() {
			return tracks.isEmpty() && points.isEmpty() && routes.isEmpty();
		}
		
		@Override
		public void parse(XmlPullParser parser, Stack<GPXExtensions> parserState, SimpleDateFormat format) throws XmlPullParserException, IOException {
			super.parse(parser, parserState, format);
			String tag = parser.getName();
			if (tag.equals("gpx")) {
				this.author = parser.getAttributeValue("", "creator");
			} else if (tag.equals("trk")) {
				Track track = new Track();
				tracks.add(track);
				parserState.push(track);
			} else if (tag.equals("rte")) {
				Route route = new Route();
				routes.add(route);
				parserState.push(route);
			} else if (tag.equals("wpt")) {
				WptPt wptPt = parseWptAttributes(parser);
				points.add(wptPt);
				parserState.push(wptPt);
			} else if (tag.equals("extensions")) {
				Extensions ext = new Extensions();
				parserState.push(ext);
			}
		}
		
		@Override
		protected boolean isEndingTag(String tag) {
			return "gpx".equals(tag);
		}
	}
	
	
	public static String writeGpxFile(File fout, GPXFile file, Context ctx) {
		try {
			SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_FORMAT);
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			FileOutputStream output = new FileOutputStream(fout);
			XmlSerializer serializer = Xml.newSerializer();
			serializer.setOutput(output, "UTF-8"); //$NON-NLS-1$
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true); //$NON-NLS-1$
			serializer.startDocument("UTF-8", true); //$NON-NLS-1$
			serializer.startTag(null, "gpx"); //$NON-NLS-1$
			serializer.attribute(null, "version", "1.1"); //$NON-NLS-1$ //$NON-NLS-2$
			if(file.author == null ){
				serializer.attribute(null, "creator", Version.getAppName(ctx)); //$NON-NLS-1$
			} else {
				serializer.attribute(null, "creator", file.author); //$NON-NLS-1$
			}
			serializer.attribute(null, "xmlns", "http://www.topografix.com/GPX/1/1"); //$NON-NLS-1$ //$NON-NLS-2$
			serializer.attribute(null, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			serializer.attribute(null, "xsi:schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");
			

			for (Track track : file.tracks) {
				serializer.startTag(null, "trk"); //$NON-NLS-1$
				writeNotNullText(serializer, "name", track.name);
				writeNotNullText(serializer, "desc", track.desc);
				for (TrkSegment segment : track.segments) {
					serializer.startTag(null, "trkseg"); //$NON-NLS-1$
					for (WptPt p : segment.points) {
						serializer.startTag(null, "trkpt"); //$NON-NLS-1$
						writeWpt(format, serializer, p);
						serializer.endTag(null, "trkpt"); //$NON-NLS-1$
					}
					serializer.endTag(null, "trkseg"); //$NON-NLS-1$
				}
				writeExtensions(serializer, track);
				serializer.endTag(null, "trk"); //$NON-NLS-1$
			}
			
			for (Route track : file.routes) {
				serializer.startTag(null, "rte"); //$NON-NLS-1$
				writeNotNullText(serializer, "name", track.name);
				writeNotNullText(serializer, "desc", track.desc);

				for (WptPt p : track.points) {
					serializer.startTag(null, "rtept"); //$NON-NLS-1$
					writeWpt(format, serializer, p);
					serializer.endTag(null, "rtept"); //$NON-NLS-1$
				}
				writeExtensions(serializer, track);
				serializer.endTag(null, "rte"); //$NON-NLS-1$
			}
			
			for (WptPt l : file.points) {
				serializer.startTag(null, "wpt"); //$NON-NLS-1$
				writeWpt(format, serializer, l);
				serializer.endTag(null, "wpt"); //$NON-NLS-1$
			}

			serializer.endTag(null, "gpx"); //$NON-NLS-1$
			serializer.flush();
			serializer.endDocument();
		} catch (RuntimeException e) {
			log.error("Error saving gpx", e); //$NON-NLS-1$
			return ctx.getString(R.string.error_occurred_saving_gpx);
		} catch (IOException e) {
			log.error("Error saving gpx", e); //$NON-NLS-1$
			return ctx.getString(R.string.error_occurred_saving_gpx);
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
	
	private static void writeExtensions(XmlSerializer serializer, GPXExtensions p) throws IOException {
		if (!p.getExtensionsToRead().isEmpty()) {
			serializer.startTag(null, "extensions");
			for (Map.Entry<String, String> s : p.getExtensionsToRead().entrySet()) {
				writeNotNullText(serializer, s.getKey(), s.getValue());
			}
			serializer.endTag(null, "extensions");
		}
	}

	private static void writeWpt(SimpleDateFormat format, XmlSerializer serializer, WptPt p) throws IOException {
		serializer.attribute(null, "lat", latLonFormat.format(p.lat)); //$NON-NLS-1$ //$NON-NLS-2$
		serializer.attribute(null, "lon", latLonFormat.format(p.lon)); //$NON-NLS-1$ //$NON-NLS-2$

		if(!Double.isNaN(p.ele)){
			writeNotNullText(serializer, "ele", p.ele+"");
		}
		writeNotNullText(serializer, "name", p.name);
		writeNotNullText(serializer, "category", p.category);
		writeNotNullText(serializer, "desc", p.desc);
		if(!Double.isNaN(p.hdop)){
			writeNotNullText(serializer, "hdop", p.hdop+"");
		}
		if(p.time != 0){
			writeNotNullText(serializer, "time", format.format(new Date(p.time)));
		}
		if (p.speed > 0) {
			p.getExtensionsToWrite().put("speed", p.speed+"");
		}
		writeExtensions(serializer, p);
	}
	
	
	public static class GPXFileResult {
		public ArrayList<List<Location>> locations = new ArrayList<List<Location>>();
		public ArrayList<WptPt> wayPoints = new ArrayList<WptPt>();
		// special case for cloudmate gpx : they discourage common schema
		// by using waypoint as track points and rtept are not very close to real way
		// such as wpt. However they provide additional information into gpx.
		public boolean cloudMadeFile;
		public String error;
		
		public Location findFistLocation(){
			for(List<Location> l : locations){
				for(Location ls : l){
					if(ls != null){
						return ls;
					}
				}
			}
			return null;
		}
	}
	
	
	private static String readText(XmlPullParser parser, String key) throws XmlPullParserException, IOException {
		int tok;
		String text = null;
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if(tok == XmlPullParser.END_TAG && parser.getName().equals(key)){
				break;
			} else if(tok == XmlPullParser.TEXT){
				if(text == null){
					text = parser.getText();
				} else {
					text += parser.getText();
				}
			}
			
		}
		return text;
	}
	
	public static GPXFile loadGPXFile(Context ctx, File f, boolean convertCloudmadeSource) {
		try {
			GPXFile file = loadGPXFile(ctx, new FileInputStream(f), convertCloudmadeSource);
			file.path = f.getAbsolutePath();
			return file;
		} catch (FileNotFoundException e) {
			GPXFile res = new GPXFile();
			res.path = f.getAbsolutePath();
			log.error("Error reading gpx", e); //$NON-NLS-1$
			res.warning = ctx.getString(R.string.error_reading_gpx);
			return res;
		}
	}
	
	public static GPXFile loadGPXFile(Context ctx, InputStream f, boolean convertCloudmadeSource) {
		GPXFile res = new GPXFile();
		SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_FORMAT);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(f, "UTF-8"); //$NON-NLS-1$
			Stack<GPXExtensions> parserState = new Stack<GPXExtensions>();
			parserState.push(res);
			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					GPXExtensions element = parserState.peek();
					element.parse(parser, parserState,format);
				} else if (tok == XmlPullParser.END_TAG) {
					GPXExtensions parse = parserState.peek();
					String tag = parser.getName();
					if (!parse.parseEnd(tag,parserState)) {
						log.error("Bad ending tag: " + tag + " for element " + parse.getClass());
					}
				}
			}
			if(convertCloudmadeSource && res.isCloudmadeRouteFile()){
				Track tk = new Track();
				res.tracks.add(tk);
				TrkSegment segment = new TrkSegment();
				tk.segments.add(segment);
				
				for(WptPt wp : res.points){
					segment.points.add(wp);
				}
			    res.points.clear();
			}
		} catch (XmlPullParserException e) {
			log.error("Error reading gpx", e); //$NON-NLS-1$
			res.warning = ctx.getString(R.string.error_reading_gpx);
		} catch (IOException e) {
			log.error("Error reading gpx", e); //$NON-NLS-1$
			res.warning = ctx.getString(R.string.error_reading_gpx);
		}

		return res;
	}
	

	private static WptPt parseWptAttributes(XmlPullParser parser) {
		WptPt wpt = new WptPt();
		try {
			wpt.lat = Double.parseDouble(parser.getAttributeValue("", "lat")); //$NON-NLS-1$ //$NON-NLS-2$
			wpt.lon = Double.parseDouble(parser.getAttributeValue("", "lon")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (NumberFormatException e) {
		}
		return wpt;
	}


}