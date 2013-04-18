package net.osmand.plus;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.GPXUtilities.GPXFile;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;


public class GPXUtilities {
	public final static Log log = PlatformUtil.getLog(GPXUtilities.class);

	private final static String GPX_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";  //$NON-NLS-1$
	
	private final static NumberFormat latLonFormat = new DecimalFormat("0.00#####", new DecimalFormatSymbols(new Locale("EN", "US")));
	
	public static class GPXExtensions {
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
		
	}
	
	public static class TrkSegment extends GPXExtensions {
		public List<WptPt> points = new ArrayList<WptPt>();
		
	}
	
	public static class Track extends GPXExtensions {
		public String name = null;
		public String desc = null;
		public List<TrkSegment> segments = new ArrayList<TrkSegment>();
		
	}
	
	public static class Route extends GPXExtensions {
		public String name = null;
		public String desc = null;
		public List<WptPt> points = new ArrayList<WptPt>();
		
	}

	public static class GPXFile extends GPXExtensions {
		public String author;
		public List<Track> tracks = new ArrayList<Track>();
		public List<WptPt> points = new ArrayList<WptPt>();
		public List<Route> routes = new ArrayList<Route>();
		public String warning = null;
		public String path = "";
		public boolean showCurrentTrack;
		
		public List<List<WptPt>> processedPointsToDisplay = new ArrayList<List<WptPt>>();
		
		public boolean isCloudmadeRouteFile(){
			return "cloudmade".equalsIgnoreCase(author);
		}
		
		public void proccessPoints(){
			List<List<WptPt>> tpoints = new ArrayList<List<WptPt>>();
			boolean created = false;
			for (Track t : tracks) {
				for (TrkSegment ts : t.segments) {
					if(ts.points.size() > 0) {
						created = true;
						tpoints.add(ts.points);
					}
				}
			}
			if(!created && routes.size() > 0) {
				for(Route r : routes) {
					tpoints.add(r.points);
				}
			}
			processedPointsToDisplay = tpoints;
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
			for (Track t : tracks) {
				if (t.segments != null) {
					for (TrkSegment s : t.segments) {
						boolean tracksEmpty = s.points.isEmpty();
						if (!tracksEmpty) {
							return false;
						}
					}
				}
			}
			return points.isEmpty() && routes.isEmpty();
		}
		
		
	}
	
	
	public static String writeGpxFile(File fout, GPXFile file, ClientContext ctx) {
		try {
			SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_FORMAT);
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			FileOutputStream output = new FileOutputStream(fout);
			XmlSerializer serializer = ctx.getInternalAPI().newSerializer();
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
	
	public static GPXFile loadGPXFile(ClientContext ctx, File f, boolean convertCloudmadeSource) {
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
	
	public static GPXFile loadGPXFile(ClientContext ctx, InputStream f, boolean convertCloudmadeSource) {
		GPXFile res = new GPXFile();
		SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_FORMAT);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			XmlPullParser parser = ctx.getInternalAPI().newPullParser();
			parser.setInput(getUTF8Reader(f)); //$NON-NLS-1$
			Stack<GPXExtensions> parserState = new Stack<GPXExtensions>();
			boolean extensionReadMode = false;
			parserState.push(res);
			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					Object parse = parserState.peek();
					String tag = parser.getName();
					if (extensionReadMode && parse instanceof GPXExtensions) {
						String value = readText(parser, tag);
						if (value != null) {
							((GPXExtensions) parse).getExtensionsToWrite().put(tag, value);
							if (tag.equals("speed") && parse instanceof WptPt) {
								try {
									((WptPt) parse).speed = Float.parseFloat(value);
								} catch (NumberFormatException e) {
								}
							}
						}

					} else if (parse instanceof GPXExtensions && tag.equals("extensions")) {
						extensionReadMode = true;
					} else {
						if (parse instanceof GPXFile) {
							if (parser.getName().equals("gpx")) {
								((GPXFile) parse).author = parser.getAttributeValue("", "creator");
							}
							if (parser.getName().equals("trk")) {
								Track track = new Track();
								((GPXFile) parse).tracks.add(track);
								parserState.push(track);
							}
							if (parser.getName().equals("rte")) {
								Route route = new Route();
								((GPXFile) parse).routes.add(route);
								parserState.push(route);
							}
							if (parser.getName().equals("wpt")) {
								WptPt wptPt = parseWptAttributes(parser);
								((GPXFile) parse).points.add(wptPt);
								parserState.push(wptPt);
							}
						} else if (parse instanceof Route) {
							if (parser.getName().equals("name")) {
								((Route) parse).name = readText(parser, "name");
							}
							if (parser.getName().equals("desc")) {
								((Route) parse).desc = readText(parser, "desc");
							}
							if (parser.getName().equals("rtept")) {
								WptPt wptPt = parseWptAttributes(parser);
								((Route) parse).points.add(wptPt);
								parserState.push(wptPt);
							}
						} else if (parse instanceof Track) {
							if (parser.getName().equals("name")) {
								((Track) parse).name = readText(parser, "name");
							}
							if (parser.getName().equals("desc")) {
								((Track) parse).desc = readText(parser, "desc");
							}
							if (parser.getName().equals("trkseg")) {
								TrkSegment trkSeg = new TrkSegment();
								((Track) parse).segments.add(trkSeg);
								parserState.push(trkSeg);
							}
						} else if (parse instanceof TrkSegment) {
							if (parser.getName().equals("trkpt")) {
								WptPt wptPt = parseWptAttributes(parser);
								((TrkSegment) parse).points.add(wptPt);
								parserState.push(wptPt);
							}
							// main object to parse
						} else if (parse instanceof WptPt) {
							if (parser.getName().equals("name")) {
								((WptPt) parse).name = readText(parser, "name");
							} else if (parser.getName().equals("desc")) {
								((WptPt) parse).desc = readText(parser, "desc");
							} else if (tag.equals("category")) {
								((WptPt) parse).category = readText(parser, "category");
							} else if (parser.getName().equals("ele")) {
								String text = readText(parser, "ele");
								if (text != null) {
									try {
										((WptPt) parse).ele = Float.parseFloat(text);
									} catch (NumberFormatException e) {
									}
								}
							} else if (parser.getName().equals("hdop")) {
								String text = readText(parser, "hdop");
								if (text != null) {
									try {
										((WptPt) parse).hdop = Float.parseFloat(text);
									} catch (NumberFormatException e) {
									}
								}
							} else if (parser.getName().equals("time")) {
								String text = readText(parser, "time");
								if (text != null) {
									try {
										((WptPt) parse).time = format.parse(text).getTime();
									} catch (ParseException e) {
									}
								}
							}
						}
					}

				} else if (tok == XmlPullParser.END_TAG) {
					Object parse = parserState.peek();
					String tag = parser.getName();
					if (parse instanceof GPXExtensions && tag.equals("extensions")) {
						extensionReadMode = false;
					}
					
					if(tag.equals("trkpt")){
						Object pop = parserState.pop();
						assert pop instanceof WptPt;
					} else if(tag.equals("wpt")){
						Object pop = parserState.pop();
						assert pop instanceof WptPt;
					} else if(tag.equals("rtept")){
						Object pop = parserState.pop();
						assert pop instanceof WptPt;
					} else if(tag.equals("trk")){
						Object pop = parserState.pop();
						assert pop instanceof Track;
					} else if(tag.equals("rte")){
						Object pop = parserState.pop();
						assert pop instanceof Route;
					} else if(tag.equals("trkseg")){
						Object pop = parserState.pop();
						assert pop instanceof TrkSegment;
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
		} catch (RuntimeException e) {
			log.error("Error reading gpx", e); //$NON-NLS-1$
			res.warning = ctx.getString(R.string.error_reading_gpx) + " " + e.getMessage();
		} catch (XmlPullParserException e) {
			log.error("Error reading gpx", e); //$NON-NLS-1$
			res.warning = ctx.getString(R.string.error_reading_gpx) + " " + e.getMessage();
		} catch (IOException e) {
			log.error("Error reading gpx", e); //$NON-NLS-1$
			res.warning = ctx.getString(R.string.error_reading_gpx) + " " + e.getMessage();
		}

		return res;
	}

	private static Reader getUTF8Reader(InputStream f) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(f);
		assert bis.markSupported();
		bis.mark(3);
		boolean reset = true;
		byte[] t = new byte[3]; 
		bis.read(t); 
		if (t[0] == ((byte)0xef) && t[1]== ((byte)0xbb) && t[2] == ((byte)0xbf)) {
			reset = false;
		}
		if(reset) {
			bis.reset();
		}
		return new InputStreamReader(bis, "UTF-8");
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

	public static void mergeGPXFileInto(GPXFile to, GPXFile from) {
		if(from == null) {
			return;
		}
		if(from.showCurrentTrack) {
			to.showCurrentTrack = true;
		}
		if(from.points != null) { 
			to.points.addAll(from.points);
		}
		if(from.tracks != null) {
			to.tracks.addAll(from.tracks);
		}
		if(from.routes != null) {
			to.routes.addAll(from.routes);
		}
		if(from.warning != null) {
			to.warning = from.warning;
		}
		
	}


}