package net.osmand;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import net.osmand.plus.R;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.location.Location;
import android.util.Xml;
import android.widget.Toast;

public class GPXUtilities {
	public final static Log log = LogUtil.getLog(GPXUtilities.class);
	
	
	private final static String GPX_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";  //$NON-NLS-1$
	
	private final static NumberFormat latLonFormat = new DecimalFormat("0.00#####");   
	
	public static class TrkPt {
		public double lat;
		public double lon;
		public double ele;
		public double speed;
		public long time;
	}
	
	public static class WptPt {
		public double lat;
		public double lon;
		public String name;
	}
	
	
	
	public static boolean saveToXMLFiles(File fout, List<WptPt> data, Context ctx ){
		try {
			FileOutputStream output = new FileOutputStream(fout);
			XmlSerializer serializer = Xml.newSerializer();
			serializer.setOutput(output, "UTF-8"); //$NON-NLS-1$
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true); //$NON-NLS-1$
			serializer.startDocument("UTF-8", true); //$NON-NLS-1$
			serializer.startTag(null, "gpx"); //$NON-NLS-1$
			serializer.attribute(null, "version", "1.1"); //$NON-NLS-1$ //$NON-NLS-2$
			serializer.attribute(null, "creator", Version.APP_NAME_VERSION); //$NON-NLS-1$
			serializer.attribute("xmlns", "xsi", "http://www.w3.org/2001/XMLSchema-instance"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			serializer.attribute("xsi", "schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			serializer.attribute(null, "xmlns", "http://www.topografix.com/GPX/1/1"); //$NON-NLS-1$ //$NON-NLS-2$

			for (WptPt l : data) {
				serializer.startTag(null, "wpt"); //$NON-NLS-1$
				serializer.attribute(null, "lat", latLonFormat.format(l.lat)); //$NON-NLS-1$ 
				serializer.attribute(null, "lon", latLonFormat.format(l.lon)); //$NON-NLS-1$ //$NON-NLS-2$
				serializer.startTag(null, "name"); //$NON-NLS-1$
				serializer.text(l.name);
				serializer.endTag(null, "name"); //$NON-NLS-1$
				serializer.endTag(null, "wpt"); //$NON-NLS-1$
			}

			serializer.endTag(null, "gpx"); //$NON-NLS-1$
			serializer.flush();
			serializer.endDocument();

			return true;
		} catch (RuntimeException e) {
			log.error("Error saving gpx", e); //$NON-NLS-1$
			Toast.makeText(ctx, ctx.getString(R.string.error_occurred_saving_gpx), Toast.LENGTH_LONG).show();
			return false;
		} catch (IOException e) {
			log.error("Error saving gpx", e); //$NON-NLS-1$
			Toast.makeText(ctx, ctx.getString(R.string.error_occurred_saving_gpx), Toast.LENGTH_LONG).show();
			return false;
		}
		
	}
	
	public static String saveToXMLFiles(File dir, Map<String, List<List<TrkPt>>> data, Context ctx){
		SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_FORMAT);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			for (String f : data.keySet()) {
				File fout = new File(dir, f + ".gpx"); //$NON-NLS-1$
				int ind = 1;
				while(fout.exists()){
					fout = new File(dir, f + "_"+(++ind)+".gpx"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				FileOutputStream output = new FileOutputStream(fout);
				XmlSerializer serializer = Xml.newSerializer();
				serializer.setOutput(output, "UTF-8"); //$NON-NLS-1$
				serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true); //$NON-NLS-1$
				serializer.startDocument("UTF-8", true); //$NON-NLS-1$
				serializer.startTag(null, "gpx"); //$NON-NLS-1$
				serializer.attribute(null, "version", "1.1"); //$NON-NLS-1$ //$NON-NLS-2$
				serializer.attribute(null, "creator", Version.APP_NAME_VERSION); //$NON-NLS-1$
				serializer.attribute("xmlns", "xsi", "http://www.w3.org/2001/XMLSchema-instance"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				serializer.attribute("xsi", "schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				serializer.attribute(null, "xmlns", "http://www.topografix.com/GPX/1/1"); //$NON-NLS-1$ //$NON-NLS-2$
				
				serializer.startTag(null, "trk"); //$NON-NLS-1$
				for(List<TrkPt> l : data.get(f)){
					serializer.startTag(null, "trkseg"); //$NON-NLS-1$
					for(TrkPt p : l){
						serializer.startTag(null, "trkpt"); //$NON-NLS-1$
						serializer.attribute(null, "lat", latLonFormat.format(p.lat)); //$NON-NLS-1$ //$NON-NLS-2$
						serializer.attribute(null, "lon", latLonFormat.format(p.lon)); //$NON-NLS-1$ //$NON-NLS-2$
						serializer.startTag(null, "time"); //$NON-NLS-1$
						serializer.text(format.format(new Date(p.time)));
						serializer.endTag(null, "time"); //$NON-NLS-1$
						serializer.startTag(null, "ele"); //$NON-NLS-1$
						serializer.text(p.ele+""); //$NON-NLS-1$
						serializer.endTag(null, "ele"); //$NON-NLS-1$
						if (p.speed > 0) {
							serializer.startTag(null, "speed"); //$NON-NLS-1$
							serializer.text(p.speed + ""); //$NON-NLS-1$
							serializer.endTag(null, "speed"); //$NON-NLS-1$
						}
						
						serializer.endTag(null, "trkpt"); //$NON-NLS-1$
					}
					serializer.endTag(null, "trkseg"); //$NON-NLS-1$
				}
				serializer.endTag(null, "trk"); //$NON-NLS-1$
				
				serializer.endTag(null, "gpx"); //$NON-NLS-1$
				serializer.flush();
				serializer.endDocument();
			}
			return null;
		} catch (RuntimeException e) {
			log.error("Error saving gpx", e); //$NON-NLS-1$
			return ctx.getString(R.string.error_occurred_saving_gpx);
		} catch (IOException e) {
			log.error("Error saving gpx", e); //$NON-NLS-1$
			return ctx.getString(R.string.error_occurred_saving_gpx);
		}
	}
	
	
	public static class GPXFileResult {
		public ArrayList<List<Location>> locations = new ArrayList<List<Location>>();
		public ArrayList<WptPt> wayPoints = new ArrayList<WptPt>();
		// special case for cloudmate gpx : they discourage common schema
		// by using waypoint as track points and rtept are not very close to real way
		// such as wpt. However they provide additional information into gpx.
		public boolean cloudMadeFile;
		public String error;
	}
	
	public static GPXFileResult loadGPXFile(Context ctx, File f){
		GPXFileResult res = new GPXFileResult();
		try {
			boolean cloudMade = false;
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(new FileInputStream(f), "UTF-8"); //$NON-NLS-1$
			
			int tok;
			Location current = null;
			String currentName = ""; //$NON-NLS-1$
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					if (parser.getName().equals("copyright")) { //$NON-NLS-1$
						cloudMade |= "cloudmade".equalsIgnoreCase(parser.getAttributeValue("", "author")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

					} else if (parser.getName().equals("trkseg")) { //$NON-NLS-1$
						res.locations.add(new ArrayList<Location>());
					} else if (parser.getName().equals("wpt") || parser.getName().equals("trkpt") || //$NON-NLS-1$//$NON-NLS-2$
							(!cloudMade && parser.getName().equals("rtept"))) { //$NON-NLS-1$
						// currently not distinguish different point represents all as a line
						try {
							currentName = ""; //$NON-NLS-1$
							current = new Location("gpx_file"); //$NON-NLS-1$
							current.setLatitude(Double.parseDouble(parser.getAttributeValue("", "lat"))); //$NON-NLS-1$ //$NON-NLS-2$
							current.setLongitude(Double.parseDouble(parser.getAttributeValue("", "lon"))); //$NON-NLS-1$ //$NON-NLS-2$
						} catch (NumberFormatException e) {
							current = null;

						}
					} else if (current != null && parser.getName().equals("name")) { //$NON-NLS-1$
						if (parser.next() == XmlPullParser.TEXT) {
							currentName = parser.getText();
						}
					}
				} else if (tok == XmlPullParser.END_TAG) {
					if (parser.getName().equals("wpt") || //$NON-NLS-1$
							parser.getName().equals("trkpt") || (!cloudMade && parser.getName().equals("rtept"))) { //$NON-NLS-1$ //$NON-NLS-2$ 
						if (current != null) {
							if (parser.getName().equals("wpt") && !cloudMade) { //$NON-NLS-1$
								WptPt pt = new WptPt();
								pt.lat = current.getLatitude();
								pt.lon = current.getLongitude();
								pt.name = currentName;
								res.wayPoints.add(pt);
							} else {
								if (res.locations.isEmpty()) {
									res.locations.add(new ArrayList<Location>());
								}
								res.locations.get(res.locations.size() - 1).add(current);
							}
						}
					}
				}
			}
		} catch (XmlPullParserException e) {
			log.error("Error reading gpx", e); //$NON-NLS-1$
			res.error = ctx.getString(R.string.error_reading_gpx);
		} catch (IOException e) {
			log.error("Error reading gpx", e); //$NON-NLS-1$
			res.error = ctx.getString(R.string.error_reading_gpx);
		}
		
		return res;
	}

}
