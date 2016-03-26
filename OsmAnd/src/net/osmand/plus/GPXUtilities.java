
package net.osmand.plus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;

import gnu.trove.list.array.TIntArrayList;


public class GPXUtilities {

	public final static Log log = PlatformUtil.getLog(GPXUtilities.class);

	private final static String GPX_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"; //$NON-NLS-1$


	public List<GPXTrackAnalysis> splitByDistance(double meters) {
		return split(getDistanceMetric(), getTimeSplit(), meters);
	}

	public List<GPXTrackAnalysis> splitByTime(int seconds) {
		return split(getTimeSplit(), getDistanceMetric(), seconds);
	}

	private List<GPXTrackAnalysis> split(SplitMetric metric, SplitMetric secondaryMetric, double metricLimit) {
		List<SplitSegment> splitSegments = new ArrayList<SplitSegment>();
	//TODO BOO????	splitSegment(metric, secondaryMetric, metricLimit, splitSegments, this);			//<<???
		return convert(splitSegments);
	}


	
	private SplitMetric getDistanceMetric() {
		return new SplitMetric() {
			
			private float[] calculations = new float[1];

			@Override
			public double metric(WptPt p1, WptPt p2) {
				net.osmand.Location.distanceBetween(p1.lat, p1.lon, p2.lat, p2.lon, calculations);
				return calculations[0];
			}
		};
	}
	
	private SplitMetric getTimeSplit() {
		return new SplitMetric() {
			
			@Override
			public double metric(WptPt p1, WptPt p2) {
				if(p1.time != 0 && p2.time != 0) {
					return (int) Math.abs((p2.time - p1.time) / 1000l);
				}
				return 0;
			}
		};
	}
	
	private abstract class SplitMetric {

		public abstract double metric(WptPt p1, WptPt p2);

	}
	
	private void splitSegment(SplitMetric metric, SplitMetric secondaryMetric,
			double metricLimit, List<SplitSegment> splitSegments,
			TrkSegment segment) {
		double currentMetricEnd = metricLimit;
		double secondaryMetricEnd = 0;
		SplitSegment sp = new SplitSegment(segment, 0, 0);
		double total = 0;
		WptPt prev = null ;
		for (int k = 0; k < segment.points.size(); k++) {
			WptPt point = segment.points.get(k);
			if (k > 0) {
				double currentSegment = metric.metric(prev, point);
				secondaryMetricEnd += secondaryMetric.metric(prev, point);
				while (total + currentSegment > currentMetricEnd) {
					double p = currentMetricEnd - total;
					double cf = (p / currentSegment); 
					sp.setLastPoint(k - 1, cf);
					sp.metricEnd = currentMetricEnd;
					sp.secondaryMetricEnd = secondaryMetricEnd;
					splitSegments.add(sp);
					
					sp = new SplitSegment(segment, k - 1, cf);
					currentMetricEnd += metricLimit;
					prev = sp.get(0);
				}
				total += currentSegment;
			}
			prev = point;
		}
		if (segment.points.size() > 0
				&& !(sp.endPointInd == segment.points.size() - 1 && sp.startCoeff == 1)) {
			sp.metricEnd = total;
			sp.secondaryMetricEnd = secondaryMetricEnd;
			sp.setLastPoint(segment.points.size() - 2, 1);
			splitSegments.add(sp);
		}
	}

	private List<GPXTrackAnalysis> convert(List<SplitSegment> splitSegments) {
		List<GPXTrackAnalysis> ls = new ArrayList<GPXTrackAnalysis>();
		for(SplitSegment s : splitSegments) {
			GPXTrackAnalysis a = new GPXTrackAnalysis();
			a.prepareInformation(0, s);
			ls.add(a);
		}
		return ls;
	}
	

    public String asString(GPXFile file, OsmandApplication ctx) {
           final Writer writer = new StringWriter();
           writeGpx(writer, file, ctx);
           return writer.toString();
       }

	public String writeGpxFile(File fout, GPXFile file, OsmandApplication ctx) {
		Writer output = null;
		try {
			output = new OutputStreamWriter(new FileOutputStream(fout), "UTF-8"); //$NON-NLS-1$
			return writeGpx(output, file, ctx);
		} catch (IOException e) {
			log.error("Error saving gpx", e); //$NON-NLS-1$
			return ctx.getString(R.string.error_occurred_saving_gpx);
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException ignore) {
					// ignore
				}
			}
		}
	}

	public String writeGpx(Writer output, GPXFile file, OsmandApplication ctx) {
		try {
			SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_FORMAT, Locale.US);
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			XmlSerializer serializer = PlatformUtil.newSerializer();
			serializer.setOutput(output);
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true); //$NON-NLS-1$
			serializer.startDocument("UTF-8", true); //$NON-NLS-1$
			serializer.startTag(null, "gpx"); //$NON-NLS-1$
			serializer.attribute(null, "version", "1.1"); //$NON-NLS-1$ //$NON-NLS-2$
			if (file.author == null) {
				serializer.attribute(null, "creator", Version.getAppName(ctx)); //$NON-NLS-1$
			} else {
				serializer.attribute(null, "creator", file.author); //$NON-NLS-1$
			}
			serializer.attribute(null, "xmlns", "http://www.topografix.com/GPX/1/1"); //$NON-NLS-1$ //$NON-NLS-2$
			serializer.attribute(null, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			serializer.attribute(null, "xsi:schemaLocation",
					"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");

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

	private void writeNotNullText(XmlSerializer serializer, String tag, String value) throws IOException {
		if (value != null) {
			serializer.startTag(null, tag);
			serializer.text(value);
			serializer.endTag(null, tag);
		}
	}

	private void writeExtensions(XmlSerializer serializer, GPXExtensions p) throws IOException {
		if (!p.getExtensionsToRead().isEmpty()) {
			serializer.startTag(null, "extensions");
			for (Map.Entry<String, String> s : p.getExtensionsToRead().entrySet()) {
				writeNotNullText(serializer, s.getKey(), s.getValue());
			}
			serializer.endTag(null, "extensions");
		}
	}

	private void writeWpt(SimpleDateFormat format, XmlSerializer serializer, WptPt p) throws IOException {

		final NumberFormat latLonFormat = new DecimalFormat("0.00#####", new DecimalFormatSymbols(new Locale("EN", "US")));

		serializer.attribute(null, "lat", latLonFormat.format(p.lat)); //$NON-NLS-1$ //$NON-NLS-2$
		serializer.attribute(null, "lon", latLonFormat.format(p.lon)); //$NON-NLS-1$ //$NON-NLS-2$

		if (!Double.isNaN(p.ele)) {
			writeNotNullText(serializer, "ele", p.ele + "");
		}
		if (p.time != 0) {
			writeNotNullText(serializer, "time", format.format(new Date(p.time)));
		}
		writeNotNullText(serializer, "name", p.name);
		writeNotNullText(serializer, "desc", p.desc);
		if(p.link != null) {
			serializer.startTag(null, "link");
			serializer.attribute(null, "href", p.link);
			serializer.endTag(null, "link");
		}
		writeNotNullText(serializer, "type", p.category);
		if (!Double.isNaN(p.hdop)) {
			writeNotNullText(serializer, "hdop", p.hdop + "");
		}
		if (p.speed > 0) {
			p.getExtensionsToWrite().put("speed", p.speed + "");
		}
		writeExtensions(serializer, p);
	}

	public class GPXFileResult {
		public ArrayList<List<Location>> locations = new ArrayList<List<Location>>();
		public ArrayList<WptPt> wayPoints = new ArrayList<WptPt>();
		// special case for cloudmate gpx : they discourage common schema
		// by using waypoint as track points and rtept are not very close to real way
		// such as wpt. However they provide additional information into gpx.
		public boolean cloudMadeFile;
		public String error;

		public Location findFistLocation() {
			for (List<Location> l : locations) {
				for (Location ls : l) {
					if (ls != null) {
						return ls;
					}
				}
			}
			return null;
		}
	}

	private String readText(XmlPullParser parser, String key) throws XmlPullParserException, IOException {
		int tok;
		String text = null;
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.END_TAG && parser.getName().equals(key)) {
				break;
			} else if (tok == XmlPullParser.TEXT) {
				if (text == null) {
					text = parser.getText();
				} else {
					text += parser.getText();
				}
			}

		}
		return text;
	}

	public GPXFile loadGPXFile(Context ctx, File f) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			GPXFile file = loadGPXFile(ctx, fis);
			file.path = f.getAbsolutePath();
			try {
				fis.close();
			} catch (IOException e) {
			}
			return file;
		} catch (FileNotFoundException e) {
			GPXFile res = new GPXFile();
			res.path = f.getAbsolutePath();
			log.error("Error reading gpx", e); //$NON-NLS-1$
			res.warning = ctx.getString(R.string.error_reading_gpx);
			return res;
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException ignore) {
				// ignore
			}
		}
	}

	/* original version
	public static GPXFile loadGPXFile(Context ctx, InputStream f) {
		GPXFile res = new GPXFile();
		SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_FORMAT, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {

			XmlPullParser parser = PlatformUtil.newXMLPullParser();
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
							} else if (parser.getName().equals("link")) {
								((WptPt) parse).link = parser.getAttributeValue("", "href");
							} else if (tag.equals("category")) {								//TODO: is this a bug?  should be parser.getName() not tag
								((WptPt) parse).category = readText(parser, "category");
							} else if (tag.equals("type")) {									//TODO: is this a bug?  should be parser.getName() not tag
								if(((WptPt) parse).category == null) {
									((WptPt) parse).category = readText(parser, "type");
								}
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

					if (tag.equals("trkpt")) {
						Object pop = parserState.pop();
						assert pop instanceof WptPt;
					} else if (tag.equals("wpt")) {
						Object pop = parserState.pop();
						assert pop instanceof WptPt;
					} else if (tag.equals("rtept")) {
						Object pop = parserState.pop();
						assert pop instanceof WptPt;
					} else if (tag.equals("trk")) {
						Object pop = parserState.pop();
						assert pop instanceof Track;
					} else if (tag.equals("rte")) {
						Object pop = parserState.pop();
						assert pop instanceof Route;
					} else if (tag.equals("trkseg")) {
						Object pop = parserState.pop();
						assert pop instanceof TrkSegment;
					}
				}
			}
//			if (convertCloudmadeSource && res.isCloudmadeRouteFile()) {
//				Track tk = new Track();
//				res.tracks.add(tk);
//				TrkSegment segment = new TrkSegment();
//				tk.segments.add(segment);
//
//				for (WptPt wp : res.points) {
//					segment.points.add(wp);
//				}
//				res.points.clear();
//			}
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
	*/

	public GPXFile loadGPXFile(Context ctx, InputStream f) {
		GPXFile res = new GPXFile();
		SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_FORMAT, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {

			XmlPullParser parser = PlatformUtil.newXMLPullParser();
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

						if (parse instanceof TrkSegment) {						// 1st for speed
							if (tag.equals("trkpt")) {
								WptPt wptPt = parseWptAttributes(parser);
								((TrkSegment) parse).points.add(wptPt);
								parserState.push(wptPt);
							}
						} else if (parse instanceof WptPt) {
							if (tag.equals("name")) {
								((WptPt) parse).name = readText(parser, tag);
							} else if (tag.equals("desc")) {
								((WptPt) parse).desc = readText(parser, tag);
							} else if (tag.equals("link")) {
								((WptPt) parse).link = parser.getAttributeValue("", "href");
							} else if (tag.equals("category")) {
								((WptPt) parse).category = readText(parser, tag);
							} else if (tag.equals("type")) {
								if(((WptPt) parse).category == null) {
									((WptPt) parse).category = readText(parser, tag);
								}
							} else if (tag.equals("ele")) {
								String text = readText(parser, tag);
								if (text != null) {
									try {
										((WptPt) parse).ele = Float.parseFloat(text);
									} catch (NumberFormatException e) {
									}
								}
							} else if (tag.equals("hdop")) {
								String text = readText(parser, tag);
								if (text != null) {
									try {
										((WptPt) parse).hdop = Float.parseFloat(text);
									} catch (NumberFormatException e) {
									}
								}
							} else if (tag.equals("time")) {
								String text = readText(parser, tag);
								if (text != null) {
									try {
										((WptPt) parse).time = format.parse(text).getTime();
									} catch (ParseException e) {
									}
								}
							}
						} else if (parse instanceof GPXFile) {
							if (tag.equals("wpt")) {							// moved 1st for speed
								WptPt wptPt = parseWptAttributes(parser);
								((GPXFile) parse).points.add(wptPt);
								parserState.push(wptPt);
							} else if (tag.equals("gpx")) {
								((GPXFile) parse).author = parser.getAttributeValue("", "creator");
							} else if (tag.equals("trk")) {
								Track track = new Track();
								((GPXFile) parse).tracks.add(track);
								parserState.push(track);
							} else if (tag.equals("rte")) {
								Route route = new Route();
								((GPXFile) parse).routes.add(route);
								parserState.push(route);
							}
						} else if (parse instanceof Route) {
							if (tag.equals("name")) {
								((Route) parse).name = readText(parser, tag);
							} else if (tag.equals("desc")) {
								((Route) parse).desc = readText(parser, tag);
							} else if (tag.equals("rtept")) {
								WptPt wptPt = parseWptAttributes(parser);
								((Route) parse).points.add(wptPt);
								parserState.push(wptPt);
							}
						} else if (parse instanceof Track) {
							if (tag.equals("name")) {
								((Track) parse).name = readText(parser, tag);
							} else if (tag.equals("desc")) {
								((Track) parse).desc = readText(parser, tag);
							} else if (tag.equals("trkseg")) {
								TrkSegment trkSeg = new TrkSegment();
								((Track) parse).segments.add(trkSeg);
								parserState.push(trkSeg);
							}
						}
					}

				} else if (tok == XmlPullParser.END_TAG) {
					String tag = parser.getName();
					if (tag.equals("trkpt")) {					// moved first for speed!
						Object pop = parserState.pop();
						assert pop instanceof WptPt;
					} else if (tag.equals("wpt")) {
						Object pop = parserState.pop();
						assert pop instanceof WptPt;
					} else if (tag.equals("rtept")) {
						Object pop = parserState.pop();
						assert pop instanceof WptPt;
					} else if (tag.equals("trk")) {
						Object pop = parserState.pop();
						assert pop instanceof Track;
					} else if (tag.equals("rte")) {
						Object pop = parserState.pop();
						assert pop instanceof Route;
					} else if (tag.equals("trkseg")) {
						Object pop = parserState.pop();
						assert pop instanceof TrkSegment;
					} else if (tag.equals("extensions")) {
						if (parserState.peek() instanceof GPXExtensions)
							extensionReadMode = false;
					}
				}
			}
//			if (convertCloudmadeSource && res.isCloudmadeRouteFile()) {
//				Track tk = new Track();
//				res.tracks.add(tk);
//				TrkSegment segment = new TrkSegment();
//				tk.segments.add(segment);
//
//				for (WptPt wp : res.points) {
//					segment.points.add(wp);
//				}
//				res.points.clear();
//			}
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


	private Reader getUTF8Reader(InputStream f) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(f);
		assert bis.markSupported();
		bis.mark(3);
		boolean reset = true;
		byte[] t = new byte[3];
		bis.read(t);
		if (t[0] == ((byte) 0xef) && t[1] == ((byte) 0xbb) && t[2] == ((byte) 0xbf)) {
			reset = false;
		}
		if (reset) {
			bis.reset();
		}
		return new InputStreamReader(bis, "UTF-8");
	}

	private WptPt parseWptAttributes(XmlPullParser parser) {
		WptPt wpt = new WptPt();
		try {
			wpt.lat = Double.parseDouble(parser.getAttributeValue("", "lat")); //$NON-NLS-1$ //$NON-NLS-2$
			wpt.lon = Double.parseDouble(parser.getAttributeValue("", "lon")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (NumberFormatException e) {
		}
		return wpt;
	}

	public void mergeGPXFileInto(GPXFile to, GPXFile from) {
		if (from == null) {
			return;
		}
		if (from.showCurrentTrack) {
			to.showCurrentTrack = true;
		}
		if (from.points != null) {
			to.points.addAll(from.points);
		}
		if (from.tracks != null) {
			to.tracks.addAll(from.tracks);
		}
		if (from.routes != null) {
			to.routes.addAll(from.routes);
		}
		if (from.warning != null) {
			to.warning = from.warning;
		}

	}

}


