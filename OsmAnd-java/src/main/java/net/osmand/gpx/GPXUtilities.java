
package net.osmand.gpx;


import static net.osmand.gpx.GPXUtilities.RouteSegment.START_TRKPT_IDX_ATTR;

import net.osmand.IProgress;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.StringBundle;
import net.osmand.binary.StringBundleWriter;
import net.osmand.binary.StringBundleXmlReader;
import net.osmand.binary.StringBundleXmlWriter;
import net.osmand.data.Amenity;
import net.osmand.data.QuadRect;
import net.osmand.gpx.SplitMetric.DistanceSplitMetric;
import net.osmand.gpx.SplitMetric.TimeSplitMetric;
import net.osmand.router.RouteColorize.ColorizationType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TimeZone;

public class GPXUtilities {

	public static final Log log = PlatformUtil.getLog(GPXUtilities.class);

	public static final String ICON_NAME_EXTENSION = "icon";
	public static final String BACKGROUND_TYPE_EXTENSION = "background";
	public static final String COLOR_NAME_EXTENSION = "color";
	public static final String PROFILE_TYPE_EXTENSION = "profile";
	public static final String ADDRESS_EXTENSION = "address";
	public static final String HIDDEN_EXTENSION = "hidden";

	public static final String GPXTPX_PREFIX = "gpxtpx:";
	public static final String OSMAND_EXTENSIONS_PREFIX = "osmand:";
	public static final String OSM_PREFIX = "osm_tag_";
	public static final String AMENITY_PREFIX = "amenity_";
	public static final String AMENITY_ORIGIN_EXTENSION = "amenity_origin";

	public static final String GAP_PROFILE_TYPE = "gap";
	public static final String TRKPT_INDEX_EXTENSION = "trkpt_idx";
	public static final String DEFAULT_ICON_NAME = "special_star";

	public static final String POINT_ELEVATION = "ele";
	public static final String POINT_SPEED = "speed";
	public static final String POINT_BEARING = "bearing";

	public static final char TRAVEL_GPX_CONVERT_FIRST_LETTER = 'A';
	public static final int TRAVEL_GPX_CONVERT_FIRST_DIST = 5000;
	public static final int TRAVEL_GPX_CONVERT_MULT_1 = 2;
	public static final int TRAVEL_GPX_CONVERT_MULT_2 = 5;

	public static boolean GPX_TIME_OLD_FORMAT = false;
	private static final String GPX_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private static final String GPX_TIME_NO_TIMEZONE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
	private static final String GPX_TIME_PATTERN_TZ = "yyyy-MM-dd'T'HH:mm:ssXXX";
	private static final String GPX_TIME_MILLIS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	private static final String GPX_TIME_MILLIS_PATTERN_OLD = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	private static final Map<String, String> SUPPORTED_EXTENSION_TAGS = new HashMap<String, String>() {{
		put("heartrate", PointAttributes.SENSOR_TAG_HEART_RATE);
		put("osmand:hr", PointAttributes.SENSOR_TAG_HEART_RATE);
		put("hr", PointAttributes.SENSOR_TAG_HEART_RATE);
		put("speed_sensor", PointAttributes.SENSOR_TAG_SPEED);
		put("cadence", PointAttributes.SENSOR_TAG_CADENCE);
		put("temp", PointAttributes.SENSOR_TAG_TEMPERATURE_W);
		put("wtemp", PointAttributes.SENSOR_TAG_TEMPERATURE_W);
		put("atemp", PointAttributes.SENSOR_TAG_TEMPERATURE_A);
	}};

	private static final NumberFormat LAT_LON_FORMAT = new DecimalFormat("0.00#####", new DecimalFormatSymbols(Locale.US));
	// speed, ele, hdop
	public static final NumberFormat DECIMAL_FORMAT = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));

	public static final int RADIUS_DIVIDER = 5000;
	public static final double PRIME_MERIDIAN = 179.999991234;

	public enum GPXColor {
		BLACK(0xFF000000),
		DARKGRAY(0xFF444444),
		GRAY(0xFF888888),
		LIGHTGRAY(0xFFCCCCCC),
		WHITE(0xFFFFFFFF),
		RED(0xFFFF0000),
		GREEN(0xFF00FF00),
		DARKGREEN(0xFF006400),
		BLUE(0xFF0000FF),
		YELLOW(0xFFFFFF00),
		CYAN(0xFF00FFFF),
		MAGENTA(0xFFFF00FF),
		AQUA(0xFF00FFFF),
		FUCHSIA(0xFFFF00FF),
		DARKGREY(0xFF444444),
		GREY(0xFF888888),
		LIGHTGREY(0xFFCCCCCC),
		LIME(0xFF00FF00),
		MAROON(0xFF800000),
		NAVY(0xFF000080),
		OLIVE(0xFF808000),
		PURPLE(0xFF800080),
		SILVER(0xFFC0C0C0),
		TEAL(0xFF008080);

		public final int color;

		GPXColor(int color) {
			this.color = color;
		}

		public static GPXColor getColorFromName(String name) {
			for (GPXColor c : values()) {
				if (c.name().equalsIgnoreCase(name)) {
					return c;
				}
			}
			return null;
		}
	}

	public interface GPXExtensionsWriter {
		void writeExtensions(XmlSerializer serializer);
	}

	public interface GPXExtensionsReader {
		boolean readExtensions(GPXFile res, XmlPullParser parser) throws IOException, XmlPullParserException;
	}

	public static class GPXExtensions {
		public Map<String, String> extensions = null;
		GPXExtensionsWriter extensionsWriter = null;
		GPXExtensionsWriter additionalExtensionsWriter = null;

		public Map<String, String> getExtensionsToRead() {
			if (extensions == null) {
				return Collections.emptyMap();
			}
			return extensions;
		}

		public Map<String, String> getExtensionsToWrite() {
			if (extensions == null) {
				extensions = new LinkedHashMap<>();
			}
			return extensions;
		}

		public void copyExtensions(GPXExtensions e) {
			Map<String, String> extensionsToRead = e.getExtensionsToRead();
			if (!extensionsToRead.isEmpty()) {
				getExtensionsToWrite().putAll(extensionsToRead);
			}
		}

		public GPXExtensionsWriter getAdditionalExtensionsWriter() {
			return additionalExtensionsWriter;
		}

		public GPXExtensionsWriter getExtensionsWriter() {
			return extensionsWriter;
		}

		public void setExtensionsWriter(GPXExtensionsWriter extensionsWriter) {
			this.extensionsWriter = extensionsWriter;
		}

		public void setAdditionalExtensionsWriter(GPXExtensionsWriter additionalExtensionsWriter) {
			this.additionalExtensionsWriter = additionalExtensionsWriter;
		}

		public int getColor(int defColor) {
			String clrValue = null;
			if (extensions != null) {
				clrValue = extensions.get(COLOR_NAME_EXTENSION);
				if (clrValue == null) {
					clrValue = extensions.get("colour");
				}
				if (clrValue == null) {
					clrValue = extensions.get("displaycolor");
				}
				if (clrValue == null) {
					clrValue = extensions.get("displaycolour");
				}
			}
			return parseColor(clrValue, defColor);
		}

		public void setColor(int color) {
			setColor(Algorithms.colorToString(color));
		}

		public void setColor(String color) {
			getExtensionsToWrite().put(COLOR_NAME_EXTENSION, color);
		}

		public void removeColor() {
			getExtensionsToWrite().remove(COLOR_NAME_EXTENSION);
		}
	}

	public static int parseColor(String colorString, int defColor) {
		if (!Algorithms.isEmpty(colorString)) {
			if (colorString.charAt(0) == '#') {
				try {
					return Algorithms.parseColor(colorString);
				} catch (IllegalArgumentException e) {
					return defColor;
				}
			} else {
				GPXColor gpxColor = GPXColor.getColorFromName(colorString);
				if (gpxColor != null) {
					return gpxColor.color;
				}
			}
		}
		return defColor;
	}

	public static class WptPt extends GPXExtensions {
		public boolean firstPoint = false;
		public boolean lastPoint = false;
		public double lat;
		public double lon;
		public String name = null;
		public String link = null;
		// previous undocumented feature 'category' ,now 'type'
		public String category = null;
		public String desc = null;
		public String comment = null;
		// by default
		public long time = 0;
		public double ele = Double.NaN;
		public double speed = 0;
		public double hdop = Double.NaN;
		public float heading = Float.NaN;
		public float bearing = Float.NaN;
		public boolean deleted = false;
		public int speedColor = 0;
		public int altitudeColor = 0;
		public int slopeColor = 0;
		public int colourARGB = 0;    // point colour (used for altitude/speed colouring)
		public double distance = 0.0; // cumulative distance, if in a track; depends on split type of GPX-file

		public WptPt() {
		}

		public WptPt(WptPt wptPt) {
			this.lat = wptPt.lat;
			this.lon = wptPt.lon;
			this.name = wptPt.name;
			this.link = wptPt.link;

			this.category = wptPt.category;
			this.desc = wptPt.desc;
			this.comment = wptPt.comment;

			this.time = wptPt.time;
			this.ele = wptPt.ele;
			this.speed = wptPt.speed;
			this.hdop = wptPt.hdop;
			this.heading = wptPt.heading;
			this.deleted = wptPt.deleted;
			this.speedColor = wptPt.speedColor;
			this.altitudeColor = wptPt.altitudeColor;
			this.slopeColor = wptPt.slopeColor;
			this.colourARGB = wptPt.colourARGB;
			this.distance = wptPt.distance;
			getExtensionsToWrite().putAll(wptPt.getExtensionsToWrite());
		}

		public void setDistance(double dist) {
			distance = dist;
		}

		public double getDistance() {
			return distance;
		}

		public int getColor() {
			return getColor(0);
		}

		public double getLatitude() {
			return lat;
		}

		public double getLongitude() {
			return lon;
		}

		public float getHeading() {
			return heading;
		}

		public WptPt(double lat, double lon, long time, double ele, double speed, double hdop) {
			this(lat, lon, time, ele, speed, hdop, Float.NaN);
		}

		public WptPt(double lat, double lon, long time, double ele, double speed, double hdop, float heading) {
			this.lat = lat;
			this.lon = lon;
			this.time = time;
			this.ele = ele;
			this.speed = speed;
			this.hdop = hdop;
			this.heading = heading;
		}

		public WptPt(double lat, double lon, String desc, String name, String category, String color, String icon, String background) {
			this.lat = lat;
			this.lon = lon;
			this.desc = desc;
			this.name = name;
			this.category = category;
			setColor(color);
			setIconName(icon);
			setBackgroundType(background);
		}

		public boolean isVisible() {
			return true;
		}

		public String getIconName() {
			return getExtensionsToRead().get(ICON_NAME_EXTENSION);
		}

		public String getIconNameOrDefault() {
			String iconName = getIconName();
			if (iconName == null) {
				iconName = DEFAULT_ICON_NAME;
			}
			return iconName;
		}

		public void setIconName(String iconName) {
			getExtensionsToWrite().put(ICON_NAME_EXTENSION, iconName);
		}

		public String getAmenityOriginName() {
			Map<String, String> extensionsToRead = getExtensionsToRead();
			String amenityOrigin = extensionsToRead.get(AMENITY_ORIGIN_EXTENSION);
			if (amenityOrigin == null && comment != null && comment.startsWith(Amenity.class.getSimpleName())) {
				amenityOrigin = comment;
			}
			return amenityOrigin;
		}

		public void setAmenityOriginName(String originName) {
			getExtensionsToWrite().put(AMENITY_ORIGIN_EXTENSION, originName);
		}

		public int getColor(ColorizationType type) {
			if (type == ColorizationType.SPEED) {
				return speedColor;
			} else if (type == ColorizationType.ELEVATION) {
				return altitudeColor;
			} else {
				return slopeColor;
			}
		}

		public void setColor(ColorizationType type, int color) {
			if (type == ColorizationType.SPEED) {
				speedColor = color;
			} else if (type == ColorizationType.ELEVATION) {
				altitudeColor = color;
			} else if (type == ColorizationType.SLOPE) {
				slopeColor = color;
			}
		}

		public String getBackgroundType() {
			return getExtensionsToRead().get(BACKGROUND_TYPE_EXTENSION);
		}

		public void setBackgroundType(String backType) {
			getExtensionsToWrite().put(BACKGROUND_TYPE_EXTENSION, backType);
		}

		public String getProfileType() {
			return getExtensionsToRead().get(PROFILE_TYPE_EXTENSION);
		}

		public String getAddress() {
			return getExtensionsToRead().get(ADDRESS_EXTENSION);
		}

		public void setAddress(String address) {
			if (Algorithms.isBlank(address)) {
				getExtensionsToWrite().remove(ADDRESS_EXTENSION);
			} else {
				getExtensionsToWrite().put(ADDRESS_EXTENSION, address);
			}
		}

		public void setHidden(String hidden) {
			getExtensionsToWrite().put(HIDDEN_EXTENSION, hidden);
		}

		public void setProfileType(String profileType) {
			getExtensionsToWrite().put(PROFILE_TYPE_EXTENSION, profileType);
		}

		public boolean hasProfile() {
			String profileType = getProfileType();
			return profileType != null && !GAP_PROFILE_TYPE.equals(profileType);
		}

		public boolean isGap() {
			String profileType = getProfileType();
			return GAP_PROFILE_TYPE.equals(profileType);
		}

		public void setGap() {
			setProfileType(GAP_PROFILE_TYPE);
		}

		public void removeProfileType() {
			getExtensionsToWrite().remove(PROFILE_TYPE_EXTENSION);
		}

		public int getTrkPtIndex() {
			try {
				return Integer.parseInt(getExtensionsToRead().get(TRKPT_INDEX_EXTENSION));
			} catch (NumberFormatException e) {
				return -1;
			}
		}

		public void setTrkPtIndex(int index) {
			getExtensionsToWrite().put(TRKPT_INDEX_EXTENSION, String.valueOf(index));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((category == null) ? 0 : category.hashCode());
			result = prime * result + ((desc == null) ? 0 : desc.hashCode());
			result = prime * result + ((comment == null) ? 0 : comment.hashCode());
			result = prime * result + ((lat == 0) ? 0 : Double.valueOf(lat).hashCode());
			result = prime * result + ((lon == 0) ? 0 : Double.valueOf(lon).hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			WptPt other = (WptPt) obj;
			return Algorithms.objectEquals(other.name, name)
					&& Algorithms.objectEquals(other.category, category)
					&& Algorithms.objectEquals(other.lat, lat)
					&& Algorithms.objectEquals(other.lon, lon)
					&& Algorithms.objectEquals(other.desc, desc);
		}

		public boolean hasLocation() {
			return (lat != 0 && lon != 0);
		}

		public static WptPt createAdjustedPoint(double lat, double lon, String description,
		                                        String name, String category, int color,
		                                        String iconName, String backgroundType,
		                                        String amenityOriginName, Map<String, String> amenityExtensions) {
			double latAdjusted = Double.parseDouble(LAT_LON_FORMAT.format(lat));
			double lonAdjusted = Double.parseDouble(LAT_LON_FORMAT.format(lon));
			WptPt point = new WptPt(latAdjusted, lonAdjusted, System.currentTimeMillis(), Double.NaN, 0, Double.NaN);
			point.name = name;
			point.category = category;
			point.desc = description;

			if (color != 0) {
				point.setColor(color);
			}
			if (iconName != null) {
				point.setIconName(iconName);
			}
			if (backgroundType != null) {
				point.setBackgroundType(backgroundType);
			}
			if (amenityOriginName != null) {
				point.setAmenityOriginName(amenityOriginName);
			}
			if (amenityExtensions != null) {
				point.getExtensionsToWrite().putAll(amenityExtensions);
			}
			return point;
		}

		void updatePoint(WptPt pt) {
			this.lat = Double.parseDouble(LAT_LON_FORMAT.format(pt.lat));
			this.lon = Double.parseDouble(LAT_LON_FORMAT.format(pt.lon));
			this.time = System.currentTimeMillis();
			this.desc = pt.desc;
			this.name = pt.name;
			this.category = pt.category;
			String color = pt.extensions.get(COLOR_NAME_EXTENSION);
			if (color != null) {
				setColor(color);
			}
			String iconName = pt.extensions.get(ICON_NAME_EXTENSION);
			if (iconName != null) {
				setIconName(iconName);
			}
			String backgroundType = pt.extensions.get(BACKGROUND_TYPE_EXTENSION);
			if (backgroundType != null) {
				setBackgroundType(backgroundType);
			}
			String address = pt.extensions.get(ADDRESS_EXTENSION);
			if (address != null) {
				setAddress(address);
			}
			String hidden = pt.extensions.get(HIDDEN_EXTENSION);
			if (hidden != null) {
				setHidden(hidden);
			}
		}
	}

	public static class TrkSegment extends GPXExtensions {

		public String name = null;
		public boolean generalSegment = false;
		public List<WptPt> points = new ArrayList<>();

		public Object renderer;

		public List<RouteSegment> routeSegments = new ArrayList<>();
		public List<RouteType> routeTypes = new ArrayList<>();

		public boolean hasRoute() {
			return !routeSegments.isEmpty() && !routeTypes.isEmpty();
		}

		public List<GPXTrackAnalysis> splitByDistance(double meters, boolean joinSegments) {
			return split(new DistanceSplitMetric(), new TimeSplitMetric(), meters, joinSegments);
		}

		public List<GPXTrackAnalysis> splitByTime(int seconds, boolean joinSegments) {
			return split(new TimeSplitMetric(), new DistanceSplitMetric(), seconds, joinSegments);
		}

		private List<GPXTrackAnalysis> split(SplitMetric metric, SplitMetric secondaryMetric, double metricLimit, boolean joinSegments) {
			List<SplitSegment> splitSegments = new ArrayList<>();
			SplitMetric.splitSegment(metric, secondaryMetric, metricLimit, splitSegments, this, joinSegments);
			return convert(splitSegments);
		}
	}

	public static class Track extends GPXExtensions {
		public String name = null;
		public String desc = null;
		public List<TrkSegment> segments = new ArrayList<>();
		public boolean generalTrack = false;

	}

	public static class Route extends GPXExtensions {
		public String name = null;
		public String desc = null;
		public List<WptPt> points = new ArrayList<>();

	}

	public static class Metadata extends GPXExtensions {

		public String name;
		public String desc;
		public String link;
		public String keywords;
		public long time = 0;
		public Author author = null;
		public Copyright copyright = null;
		public Bounds bounds = null;

		public Metadata() {
		}

		public Metadata(Metadata source) {
			name = source.name;
			desc = source.desc;
			link = source.link;
			keywords = source.keywords;
			time = source.time;

			if (source.author != null) {
				author = new Author(source.author);
			}

			if (source.copyright != null) {
				copyright = new Copyright(source.copyright);
			}

			if (source.bounds != null) {
				bounds = new Bounds(source.bounds);
			}

			copyExtensions(source);
		}

		public String getArticleTitle() {
			return getExtensionsToRead().get("article_title");
		}

		public String getArticleLang() {
			return getExtensionsToRead().get("article_lang");
		}

		public String getDescription() {
			return getExtensionsToRead().get("desc");
		}
	}

	public static class Author extends GPXExtensions {
		public String name;
		public String email;
		public String link;

		public Author() {
		}

		public Author(Author author) {
			name = author.name;
			email = author.email;
			link = author.link;
			copyExtensions(author);
		}
	}

	public static class Copyright extends GPXExtensions {
		public String author;
		public String year;
		public String license;

		public Copyright() {
		}

		public Copyright(Copyright copyright) {
			author = copyright.author;
			year = copyright.year;
			license = copyright.license;
			copyExtensions(copyright);
		}
	}

	public static class Bounds extends GPXExtensions {
		public double minlat;
		public double minlon;
		public double maxlat;
		public double maxlon;

		public Bounds() {
		}

		public Bounds(Bounds source) {
			minlat = source.minlat;
			minlon = source.minlon;
			maxlat = source.maxlat;
			maxlon = source.maxlon;
			copyExtensions(source);
		}
	}

	public static class RouteSegment {

		public static final String START_TRKPT_IDX_ATTR = "startTrkptIdx";

		public String id;
		public String length;
		public String startTrackPointIndex;
		public String segmentTime;
		public String speed;
		public String turnType;
		public String turnLanes;
		public String turnAngle;
		public String skipTurn;
		public String types;
		public String pointTypes;
		public String names;

		public static RouteSegment fromStringBundle(StringBundle bundle) {
			RouteSegment s = new RouteSegment();
			s.id = bundle.getString("id", null);
			s.length = bundle.getString("length", null);
			s.startTrackPointIndex = bundle.getString(START_TRKPT_IDX_ATTR, null);
			s.segmentTime = bundle.getString("segmentTime", null);
			s.speed = bundle.getString("speed", null);
			s.turnType = bundle.getString("turnType", null);
			s.turnLanes = bundle.getString("turnLanes", null);
			s.turnAngle = bundle.getString("turnAngle", null);
			s.skipTurn = bundle.getString("skipTurn", null);
			s.types = bundle.getString("types", null);
			s.pointTypes = bundle.getString("pointTypes", null);
			s.names = bundle.getString("names", null);
			return s;
		}

		public StringBundle toStringBundle() {
			StringBundle bundle = new StringBundle();
			bundle.putString("id", id);
			bundle.putString("length", length);
			bundle.putString(START_TRKPT_IDX_ATTR, startTrackPointIndex);
			bundle.putString("segmentTime", segmentTime);
			bundle.putString("speed", speed);
			bundle.putString("turnType", turnType);
			bundle.putString("turnLanes", turnLanes);
			bundle.putString("turnAngle", turnAngle);
			bundle.putString("skipTurn", skipTurn);
			bundle.putString("types", types);
			bundle.putString("pointTypes", pointTypes);
			bundle.putString("names", names);
			return bundle;
		}
	}

	public static class RouteType {
		public String tag;
		public String value;

		public static RouteType fromStringBundle(StringBundle bundle) {
			RouteType t = new RouteType();
			t.tag = bundle.getString("t", null);
			t.value = bundle.getString("v", null);
			return t;
		}

		public StringBundle toStringBundle() {
			StringBundle bundle = new StringBundle();
			bundle.putString("t", tag);
			bundle.putString("v", value);
			return bundle;
		}
	}

	public static class PointsGroup {

		public String name;
		public String iconName;
		public String backgroundType;
		public List<WptPt> points = new ArrayList<>();
		public int color;

		public PointsGroup(String name) {
			this.name = name != null ? name : "";
		}

		public PointsGroup(String name, String iconName, String backgroundType, int color) {
			this(name);
			this.color = color;
			this.iconName = iconName;
			this.backgroundType = backgroundType;
		}

		public PointsGroup(WptPt point) {
			this(point.category);
			this.color = point.getColor();
			this.iconName = point.getIconName();
			this.backgroundType = point.getBackgroundType();
		}

		@Override
		public int hashCode() {
			return Algorithms.hash(name, iconName, backgroundType, color, points);
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			PointsGroup that = (PointsGroup) o;

			return color == that.color
					&& Algorithms.objectEquals(points, that.points)
					&& Algorithms.stringsEqual(name, that.name)
					&& Algorithms.stringsEqual(iconName, that.iconName)
					&& Algorithms.stringsEqual(backgroundType, that.backgroundType);
		}

		public StringBundle toStringBundle() {
			StringBundle bundle = new StringBundle();
			bundle.putString("name", name != null ? name : "");

			if (color != 0) {
				bundle.putString("color", Algorithms.colorToString(color));
			}
			if (!Algorithms.isEmpty(iconName)) {
				bundle.putString(ICON_NAME_EXTENSION, iconName);
			}
			if (!Algorithms.isEmpty(backgroundType)) {
				bundle.putString(BACKGROUND_TYPE_EXTENSION, backgroundType);
			}
			return bundle;
		}

		private static PointsGroup parsePointsGroupAttributes(XmlPullParser parser) {
			String name = parser.getAttributeValue("", "name");
			PointsGroup category = new PointsGroup(name != null ? name : "");
			category.color = parseColor(parser.getAttributeValue("", "color"), 0);
			category.iconName = parser.getAttributeValue("", ICON_NAME_EXTENSION);
			category.backgroundType = parser.getAttributeValue("", BACKGROUND_TYPE_EXTENSION);
			return category;
		}
	}


	private static List<GPXTrackAnalysis> convert(List<SplitSegment> splitSegments) {
		List<GPXTrackAnalysis> list = new ArrayList<>();
		for (SplitSegment segment : splitSegments) {
			GPXTrackAnalysis analysis = new GPXTrackAnalysis();
			analysis.prepareInformation(0, null, segment);
			list.add(analysis);
		}
		return list;
	}

	public static QuadRect calculateBounds(List<WptPt> pts) {
		QuadRect trackBounds = new QuadRect(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		updateBounds(trackBounds, pts, 0);

		return trackBounds;
	}

	public static QuadRect calculateTrackBounds(List<TrkSegment> segments) {
		QuadRect trackBounds = new QuadRect(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		boolean updated = false;
		for (TrkSegment segment : segments) {
			if (segment.points.size() > 0) {
				updateBounds(trackBounds, segment.points, 0);
				updated = true;
			}
		}
		return updated ? trackBounds : new QuadRect();
	}

	public static void updateBounds(QuadRect trackBounds, List<WptPt> pts, int startIndex) {
		for (int i = startIndex; i < pts.size(); i++) {
			WptPt pt = pts.get(i);
			trackBounds.right = Math.max(trackBounds.right, pt.lon);
			trackBounds.left = Math.min(trackBounds.left, pt.lon);
			trackBounds.top = Math.max(trackBounds.top, pt.lat);
			trackBounds.bottom = Math.min(trackBounds.bottom, pt.lat);
		}
	}

	public static int calculateTrackPoints(List<TrkSegment> segments) {
		int result = 0;
		for (TrkSegment segment : segments) {
			result += segment.points.size();
		}
		return result;
	}

	public static void updateQR(QuadRect q, WptPt p, double defLat, double defLon) {
		if (q.left == defLon && q.top == defLat &&
				q.right == defLon && q.bottom == defLat) {
			q.left = p.getLongitude();
			q.right = p.getLongitude();
			q.top = p.getLatitude();
			q.bottom = p.getLatitude();
		} else {
			q.left = Math.min(q.left, p.getLongitude());
			q.right = Math.max(q.right, p.getLongitude());
			q.top = Math.max(q.top, p.getLatitude());
			q.bottom = Math.min(q.bottom, p.getLatitude());
		}
	}

	public static String asString(GPXFile file) {
		Writer writer = new StringWriter();
		writeGpx(writer, file, null);
		return writer.toString();
	}

	public static Exception writeGpxFile(File fout, GPXFile file) {
		Writer output = null;
		try {
			if (fout.getParentFile() != null) {
				fout.getParentFile().mkdirs();
			}
			output = new OutputStreamWriter(new FileOutputStream(fout), "UTF-8"); //$NON-NLS-1$
			if (Algorithms.isEmpty(file.path)) {
				file.path = fout.getAbsolutePath();
			}
			return writeGpx(output, file, null);
		} catch (Exception e) {
			log.error("Error saving gpx", e); //$NON-NLS-1$
			return e;
		} finally {
			Algorithms.closeStream(output);
		}
	}

	public static Exception writeGpx(Writer output, GPXFile file, IProgress progress) {
		if (progress != null) {
			progress.startWork(file.getItemsToWriteSize());
		}
		try {
			XmlSerializer serializer = PlatformUtil.newSerializer();
			serializer.setOutput(output);
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true); //$NON-NLS-1$
			serializer.startDocument("UTF-8", true); //$NON-NLS-1$
			serializer.startTag(null, "gpx"); //$NON-NLS-1$
			serializer.attribute(null, "version", "1.1"); //$NON-NLS-1$ //$NON-NLS-2$
			if (file.author != null) {
				serializer.attribute(null, "creator", file.author); //$NON-NLS-1$
			}
			serializer.attribute(null, "xmlns", "http://www.topografix.com/GPX/1/1"); //$NON-NLS-1$ //$NON-NLS-2$
			serializer.attribute(null, "xmlns:osmand", "https://osmand.net");
			serializer.attribute(null, "xmlns:gpxtpx", "http://www.garmin.com/xmlschemas/TrackPointExtension/v1");
			serializer.attribute(null, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			serializer.attribute(null, "xsi:schemaLocation",
					"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");

			assignPointsGroupsExtensionWriter(file);
			writeMetadata(serializer, file, progress);
			writePoints(serializer, file, progress);
			writeRoutes(serializer, file, progress);
			writeTracks(serializer, file, progress);
			writeExtensions(serializer, file, progress);

			serializer.endTag(null, "gpx"); //$NON-NLS-1$
			serializer.endDocument();
			serializer.flush();
		} catch (Exception e) {
			log.error("Error saving gpx", e); //$NON-NLS-1$
			return e;
		}
		return null;
	}

	public static GPXExtensionsWriter createNetworkRouteExtensionWriter(final Map<String, String> networkRouteTags) {
		return new GPXExtensionsWriter() {

			@Override
			public void writeExtensions(XmlSerializer serializer) {
				StringBundle bundle = new StringBundle();
				StringBundle tagsBundle = new StringBundle();
				tagsBundle.putString("type", networkRouteTags.get("type"));
				for (Map.Entry<String, String> tag : networkRouteTags.entrySet()) {
					tagsBundle.putString(tag.getKey(), tag.getValue());
				}
				List<StringBundle> routeKeyBundle = new ArrayList<>();
				routeKeyBundle.add(tagsBundle);
				bundle.putBundleList("network_route", OSMAND_EXTENSIONS_PREFIX + "route_key", routeKeyBundle);
				StringBundleWriter bundleWriter = new StringBundleXmlWriter(bundle, serializer);
				bundleWriter.writeBundle();
			}
		};
	}

	private static void assignPointsGroupsExtensionWriter(final GPXFile gpxFile) {
		if (!Algorithms.isEmpty(gpxFile.pointsGroups) && gpxFile.getExtensionsWriter() == null) {
			gpxFile.setExtensionsWriter(new GPXExtensionsWriter() {

				@Override
				public void writeExtensions(XmlSerializer serializer) {
					StringBundle bundle = new StringBundle();
					List<StringBundle> categoriesBundle = new ArrayList<>();
					for (PointsGroup group : gpxFile.pointsGroups.values()) {
						categoriesBundle.add(group.toStringBundle());
					}
					bundle.putBundleList("points_groups", "group", categoriesBundle);
					StringBundleWriter bundleWriter = new StringBundleXmlWriter(bundle, serializer);
					bundleWriter.writeBundle();
				}
			});
		}
	}

	private static void writeMetadata(XmlSerializer serializer, GPXFile file, IProgress progress) throws IOException {
		String defName = file.metadata.name;
		String trackName = !Algorithms.isEmpty(defName) ? defName : getFilename(file.path);
		serializer.startTag(null, "metadata");
		writeNotNullText(serializer, "name", trackName);
		writeNotNullText(serializer, "desc", file.metadata.desc);
		if (file.metadata.author != null) {
			serializer.startTag(null, "author");
			writeAuthor(serializer, file.metadata.author);
			serializer.endTag(null, "author");
		}
		if (file.metadata.copyright != null) {
			serializer.startTag(null, "copyright");
			writeCopyright(serializer, file.metadata.copyright);
			serializer.endTag(null, "copyright");
		}
		writeNotNullTextWithAttribute(serializer, "link", "href", file.metadata.link);
		if (file.metadata.time != 0) {
			writeNotNullText(serializer, "time", formatTime(file.metadata.time));
		}
		writeNotNullText(serializer, "keywords", file.metadata.keywords);
		if (file.metadata.bounds != null) {
			writeBounds(serializer, file.metadata.bounds);
		}
		writeExtensions(serializer, file.metadata, null);
		if (progress != null) {
			progress.progress(1);
		}
		serializer.endTag(null, "metadata");
	}

	private static void writePoints(XmlSerializer serializer, GPXFile file, IProgress progress) throws IOException {
		for (WptPt l : file.points) {
			serializer.startTag(null, "wpt"); //$NON-NLS-1$
			writeWpt(serializer, l, progress);
			serializer.endTag(null, "wpt"); //$NON-NLS-1$
		}
	}

	private static void writeRoutes(XmlSerializer serializer, GPXFile file, IProgress progress) throws IOException {
		for (Route route : file.routes) {
			serializer.startTag(null, "rte"); //$NON-NLS-1$
			writeNotNullText(serializer, "name", route.name);
			writeNotNullText(serializer, "desc", route.desc);

			for (WptPt p : route.points) {
				serializer.startTag(null, "rtept"); //$NON-NLS-1$
				writeWpt(serializer, p, progress);
				serializer.endTag(null, "rtept"); //$NON-NLS-1$
			}
			writeExtensions(serializer, route, null);
			serializer.endTag(null, "rte"); //$NON-NLS-1$
		}
	}

	private static void writeTracks(XmlSerializer serializer, GPXFile file, IProgress progress) throws IOException {
		for (Track track : file.tracks) {
			if (!track.generalTrack) {
				serializer.startTag(null, "trk"); //$NON-NLS-1$
				writeNotNullText(serializer, "name", track.name);
				writeNotNullText(serializer, "desc", track.desc);
				for (TrkSegment segment : track.segments) {
					serializer.startTag(null, "trkseg"); //$NON-NLS-1$
					writeNotNullText(serializer, "name", segment.name);
					for (WptPt p : segment.points) {
						serializer.startTag(null, "trkpt"); //$NON-NLS-1$
						writeWpt(serializer, p, progress);
						serializer.endTag(null, "trkpt"); //$NON-NLS-1$
					}
					assignRouteExtensionWriter(segment);
					writeExtensions(serializer, segment, null);
					serializer.endTag(null, "trkseg"); //$NON-NLS-1$
				}
				writeExtensions(serializer, track, null);
				serializer.endTag(null, "trk"); //$NON-NLS-1$
			}
		}
	}

	private static void assignRouteExtensionWriter(final TrkSegment segment) {
		if (segment.hasRoute() && segment.getExtensionsWriter() == null) {
			segment.setExtensionsWriter(new GPXExtensionsWriter() {
				@Override
				public void writeExtensions(XmlSerializer serializer) {
					StringBundle bundle = new StringBundle();
					List<StringBundle> segmentsBundle = new ArrayList<>();
					for (RouteSegment segment : segment.routeSegments) {
						segmentsBundle.add(segment.toStringBundle());
					}
					bundle.putBundleList("route", "segment", segmentsBundle);
					List<StringBundle> typesBundle = new ArrayList<>();
					for (RouteType routeType : segment.routeTypes) {
						typesBundle.add(routeType.toStringBundle());
					}
					bundle.putBundleList("types", "type", typesBundle);
					StringBundleWriter bundleWriter = new StringBundleXmlWriter(bundle, serializer);
					bundleWriter.writeBundle();
				}
			});
		}
	}

	private static String getFilename(String path) {
		if (path != null) {
			int i = path.lastIndexOf('/');
			if (i > 0) {
				path = path.substring(i + 1);
			}
			i = path.lastIndexOf('.');
			if (i > 0) {
				path = path.substring(0, i);
			}
		}
		return path;
	}

	private static void writeNotNullTextWithAttribute(XmlSerializer serializer, String tag, String attribute, String value) throws IOException {
		if (value != null) {
			serializer.startTag(null, tag);
			serializer.attribute(null, attribute, value);
			serializer.endTag(null, tag);
		}
	}

	public static void writeNotNullText(XmlSerializer serializer, String tag, String value) throws IOException {
		if (value != null) {
			serializer.startTag(null, tag);
			serializer.text(value);
			serializer.endTag(null, tag);
		}
	}

	private static void writeExtensions(XmlSerializer serializer, GPXExtensions p, IProgress progress) throws IOException {
		writeExtensions(serializer, p.getExtensionsToRead(), p, progress);
	}

	private static void writeExtensions(XmlSerializer serializer, Map<String, String> extensions, GPXExtensions p, IProgress progress) throws IOException {
		GPXExtensionsWriter extensionsWriter = p.getExtensionsWriter();
		GPXExtensionsWriter additionalExtensionsWriter = p.getAdditionalExtensionsWriter();
		boolean hasExtensions = !Algorithms.isEmpty(extensions);
		if (hasExtensions || extensionsWriter != null) {
			serializer.startTag(null, "extensions");
			if (hasExtensions) {
				for (Entry<String, String> entry : extensions.entrySet()) {
					writeNotNullText(serializer, getOsmandTagKey(entry), entry.getValue());
				}
			}
			if (additionalExtensionsWriter != null) {
				serializer.startTag(null, "gpxtpx:TrackPointExtension");
				additionalExtensionsWriter.writeExtensions(serializer);
				serializer.endTag(null, "gpxtpx:TrackPointExtension");
			}
			if (extensionsWriter != null) {
				extensionsWriter.writeExtensions(serializer);
			}
			serializer.endTag(null, "extensions");
			if (progress != null) {
				progress.progress(1);
			}
		}
	}

	private static void writeWpt(XmlSerializer serializer, WptPt p, IProgress progress) throws IOException {
		serializer.attribute(null, "lat", LAT_LON_FORMAT.format(p.lat));
		serializer.attribute(null, "lon", LAT_LON_FORMAT.format(p.lon));

		if (!Double.isNaN(p.ele)) {
			writeNotNullText(serializer, POINT_ELEVATION, DECIMAL_FORMAT.format(p.ele));
		}
		if (p.time != 0) {
			writeNotNullText(serializer, "time", formatTime(p.time));
		}
		writeNotNullText(serializer, "name", p.name);
		writeNotNullText(serializer, "desc", p.desc);
		writeNotNullTextWithAttribute(serializer, "link", "href", p.link);
		writeNotNullText(serializer, "type", p.category);
		writeNotNullText(serializer, "cmt", p.comment);

		if (!Double.isNaN(p.hdop)) {
			writeNotNullText(serializer, "hdop", DECIMAL_FORMAT.format(p.hdop));
		}
		if (p.speed > 0) {
			p.getExtensionsToWrite().put(POINT_SPEED, DECIMAL_FORMAT.format(p.speed));
		}
		if (!Float.isNaN(p.heading)) {
			p.getExtensionsToWrite().put("heading", String.valueOf(Math.round(p.heading)));
		}
		Map<String, String> extensions = p.getExtensionsToRead();
		if (!"rtept".equals(serializer.getName())) {
			// Leave "profile" and "trkpt" tags for rtept only
			extensions.remove(PROFILE_TYPE_EXTENSION);
			extensions.remove(TRKPT_INDEX_EXTENSION);
		} else {
			// Remove "gap" profile
			String profile = extensions.get(PROFILE_TYPE_EXTENSION);
			if (GAP_PROFILE_TYPE.equals(profile)) {
				extensions.remove(PROFILE_TYPE_EXTENSION);
			}
		}
		assignExtensionWriter(p, extensions);
		writeExtensions(serializer, null, p, null);
		if (progress != null) {
			progress.progress(1);
		}
	}

	public static void assignExtensionWriter(WptPt wptPt, Map<String, String> pluginsExtensions) {
		if (wptPt.getExtensionsWriter() == null) {
			HashMap<String, String> regularExtensions = new HashMap<>();
			HashMap<String, String> gpxtpxExtensions = new HashMap<>();

			for (Entry<String, String> entry : pluginsExtensions.entrySet()) {
				if (entry.getKey().startsWith(GPXTPX_PREFIX)) {
					gpxtpxExtensions.put(entry.getKey(), entry.getValue());
				} else {
					regularExtensions.put(entry.getKey(), entry.getValue());
				}
			}
			wptPt.setExtensionsWriter(createExtensionsWriter(regularExtensions, true));
			if (!Algorithms.isEmpty(gpxtpxExtensions)) {
				wptPt.setAdditionalExtensionsWriter(createExtensionsWriter(gpxtpxExtensions, false));
			}
		}
	}

	private static GPXUtilities.GPXExtensionsWriter createExtensionsWriter(final Map<String, String> extensions, final boolean addOsmandPrefix) {
		return new GPXExtensionsWriter() {
			@Override
			public void writeExtensions(XmlSerializer serializer) {
				for (Entry<String, String> entry : extensions.entrySet()) {
					try {
						GPXUtilities.writeNotNullText(serializer, addOsmandPrefix ? getOsmandTagKey(entry) : entry.getKey(), entry.getValue());
					} catch (IOException e) {
						log.error(e);
					}
				}
			}
		};
	}

	private static String getOsmandTagKey(final Entry<String, String> entry) {
		String key = entry.getKey().replace(":", "_-_");
		if (!key.startsWith(OSMAND_EXTENSIONS_PREFIX)) {
			key = OSMAND_EXTENSIONS_PREFIX + key;
		}
		return key;
	}

	private static void writeAuthor(XmlSerializer serializer, Author author) throws IOException {
		writeNotNullText(serializer, "name", author.name);
		if (author.email != null && author.email.contains("@")) {
			String[] idAndDomain = author.email.split("@");
			if (idAndDomain.length == 2 && !idAndDomain[0].isEmpty() && !idAndDomain[1].isEmpty()) {
				serializer.startTag(null, "email");
				serializer.attribute(null, "id", idAndDomain[0]);
				serializer.attribute(null, "domain", idAndDomain[1]);
				serializer.endTag(null, "email");
			}
		}
		writeNotNullTextWithAttribute(serializer, "link", "href", author.link);
	}

	private static void writeCopyright(XmlSerializer serializer, Copyright copyright) throws IOException {
		if (copyright.author != null) {
			serializer.attribute(null, "author", copyright.author);
		}
		writeNotNullText(serializer, "year", copyright.year);
		writeNotNullText(serializer, "license", copyright.license);
	}

	private static void writeBounds(XmlSerializer serializer, Bounds bounds) throws IOException {
		serializer.startTag(null, "bounds");
		serializer.attribute(null, "minlat", LAT_LON_FORMAT.format(bounds.minlat));
		serializer.attribute(null, "minlon", LAT_LON_FORMAT.format(bounds.minlon));
		serializer.attribute(null, "maxlat", LAT_LON_FORMAT.format(bounds.maxlat));
		serializer.attribute(null, "maxlon", LAT_LON_FORMAT.format(bounds.maxlon));
		serializer.endTag(null, "bounds");
	}

	public static class GPXFileResult {
		public ArrayList<List<Location>> locations = new ArrayList<List<Location>>();
		public ArrayList<WptPt> wayPoints = new ArrayList<>();
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

	public static String readText(XmlPullParser parser, String key) throws XmlPullParserException, IOException {
		int tok;
		StringBuilder text = null;
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.END_TAG && parser.getName().equals(key)) {
				break;
			} else if (tok == XmlPullParser.TEXT) {
				if (text == null) {
					text = new StringBuilder(parser.getText());
				} else {
					text.append(parser.getText());
				}
			}
		}
		return text == null ? null : text.toString();
	}

	public static Map<String, String> readTextMap(XmlPullParser parser, String key)
			throws XmlPullParserException, IOException {
		int tok;
		StringBuilder text = null;
		Map<String, String> result = new HashMap<>();
		while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.END_TAG) {
				String tag = parser.getName();
				if (text != null && !Algorithms.isEmpty(text.toString().trim())) {
					result.put(tag, text.toString());
				}
				if (tag.equals(key)) {
					break;
				}
				text = null;
			} else if (tok == XmlPullParser.START_TAG) {
				text = null;
			} else if (tok == XmlPullParser.TEXT) {
				if (text == null) {
					text = new StringBuilder(parser.getText());
				} else {
					text.append(parser.getText());
				}
			}
		}
		return result;
	}

	public static String formatTime(long time) {
		SimpleDateFormat format = getTimeFormatter();
		return format.format(new Date(time));
	}

	public static long parseTime(String text) {
		if (GPX_TIME_OLD_FORMAT) {
			return parseTime(text, getTimeFormatter(), getTimeFormatterMills());
		} else {
			return parseTime(text, getTimeFormatterTZ(), getTimeFormatterMills());
		}
	}

	public static long parseTime(String text, SimpleDateFormat format, SimpleDateFormat formatMillis) {
		long time = 0;
		if (text != null) {
			try {
				time = format.parse(text).getTime();
			} catch (ParseException e1) {
				try {
					time = formatMillis.parse(text).getTime();
				} catch (ParseException e2) {
					try {
						time = getTimeNoTimeZoneFormatter().parse(text).getTime();
					} catch (ParseException e3) {
						log.error("Failed to parse date " + text);
					}
				}
			}
		}
		return time;
	}

	public static long getCreationTime(GPXFile gpxFile) {
		long time = 0;
		if (gpxFile != null) {
			if (gpxFile.metadata != null && gpxFile.metadata.time > 0) {
				time = gpxFile.metadata.time;
			} else {
				time = gpxFile.getLastPointTime();
			}
			if (time == 0) {
				time = gpxFile.modifiedTime;
			}
		}
		if (time == 0) {
			time = System.currentTimeMillis();
		}
		return time;
	}

	private static SimpleDateFormat getTimeFormatter() {
		SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_PATTERN, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format;
	}

	private static SimpleDateFormat getTimeNoTimeZoneFormatter() {
		SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_NO_TIMEZONE_PATTERN, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format;
	}

	private static SimpleDateFormat getTimeFormatterTZ() {
		SimpleDateFormat format = new SimpleDateFormat(GPX_TIME_PATTERN_TZ, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format;
	}

	private static SimpleDateFormat getTimeFormatterMills() {
		String pattern = GPX_TIME_OLD_FORMAT ? GPX_TIME_MILLIS_PATTERN_OLD : GPX_TIME_MILLIS_PATTERN;
		SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format;
	}

	public static GPXFile loadGPXFile(File file) {
		return loadGPXFile(file, null, true);
	}

	public static GPXFile loadGPXFile(File file, GPXExtensionsReader extensionsReader, boolean addGeneralTrack) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			GPXFile gpxFile = loadGPXFile(fis, extensionsReader, addGeneralTrack);
			gpxFile.path = file.getAbsolutePath();
			gpxFile.modifiedTime = file.lastModified();
			gpxFile.pointsModifiedTime = gpxFile.modifiedTime;

			Algorithms.closeStream(fis);
			if (gpxFile.error != null) {
				log.info("Error reading gpx " + gpxFile.path);
			}
			return gpxFile;
		} catch (IOException e) {
			GPXFile gpxFile = new GPXFile(null);
			gpxFile.path = file.getAbsolutePath();
			log.error("Error reading gpx " + gpxFile.path, e); //$NON-NLS-1$
			gpxFile.error = e;
			return gpxFile;
		} finally {
			Algorithms.closeStream(fis);
		}
	}

	public static GPXFile loadGPXFile(InputStream stream) {
		return loadGPXFile(stream, null, true);
	}

	public static GPXFile loadGPXFile(InputStream stream, GPXExtensionsReader extensionsReader, boolean addGeneralTrack) {
		GPXFile gpxFile = new GPXFile(null);
		gpxFile.metadata.time = 0;
		try {
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(getUTF8Reader(stream));
			Track routeTrack = new Track();
			TrkSegment routeTrackSegment = new TrkSegment();
			routeTrack.segments.add(routeTrackSegment);
			Stack<GPXExtensions> parserState = new Stack<>();
			TrkSegment firstSegment = null;
			boolean extensionReadMode = false;
			boolean routePointExtension = false;
			List<RouteSegment> routeSegments = new ArrayList<>();
			List<RouteType> routeTypes = new ArrayList<>();
			List<PointsGroup> pointsGroups = new ArrayList<>();
			boolean routeExtension = false;
			boolean typesExtension = false;
			boolean pointsGroupsExtension = false;
			boolean networkRoute = false;
			parserState.push(gpxFile);
			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					GPXExtensions parse = parserState.peek();
					String tag = parser.getName();
					if (extensionReadMode && parse != null && !routePointExtension) {
						String tagName = tag.toLowerCase();
						if (routeExtension && tagName.equals("segment")) {
							RouteSegment segment = parseRouteSegmentAttributes(parser);
							routeSegments.add(segment);
						} else if (typesExtension && tagName.equals("type")) {
							RouteType type = parseRouteTypeAttributes(parser);
							routeTypes.add(type);
						} else if (pointsGroupsExtension && tagName.equals("group")) {
							PointsGroup pointsGroup = PointsGroup.parsePointsGroupAttributes(parser);
							pointsGroups.add(pointsGroup);
						} else if (networkRoute && tagName.equals("route_key")) {
							gpxFile.addRouteKeyTags(parseRouteKeyAttributes(parser));
						}
						switch (tagName) {
							case "routepointextension":
								routePointExtension = true;
								if (parse instanceof WptPt) {
									parse.getExtensionsToWrite().put("offset", routeTrackSegment.points.size() + "");
								}
								break;
							case "route":
								routeExtension = true;
								break;
							case "types":
								typesExtension = true;
								break;
							case "points_groups":
								pointsGroupsExtension = true;
								break;
							case "network_route":
								networkRoute = true;
								break;

							default:
								if (extensionsReader == null || !extensionsReader.readExtensions(gpxFile, parser)) {
									Map<String, String> values = readTextMap(parser, tag);
									if (values.size() > 0) {
										for (Entry<String, String> entry : values.entrySet()) {
											String t = entry.getKey().toLowerCase();
											String supportedTag = getExtensionsSupportedTag(t);
											String value = entry.getValue();
											parse.getExtensionsToWrite().put(supportedTag, value);
											if (parse instanceof WptPt) {
												WptPt wptPt = (WptPt) parse;
												if (POINT_SPEED.equals(tag)) {
													try {
														wptPt.speed = Float.parseFloat(value);
													} catch (NumberFormatException e) {
														log.debug(e.getMessage(), e);
													}
												} else if (POINT_BEARING.equals(tag)) {
													try {
														wptPt.bearing = Float.parseFloat(value);
													} catch (NumberFormatException ignored) {
													}
												}
											}
										}
									}
								}
								break;
						}
					} else if (parse != null && tag.equals("extensions")) {
						extensionReadMode = true;
					} else if (routePointExtension) {
						if (tag.equals("rpt")) {
							WptPt wptPt = parseWptAttributes(parser);
							routeTrackSegment.points.add(wptPt);
							parserState.push(wptPt);
						}
					} else {
						if (parse instanceof GPXFile) {
							if (tag.equals("gpx")) {
								((GPXFile) parse).author = parser.getAttributeValue("", "creator");
							}
							if (tag.equals("metadata")) {
								Metadata metadata = new Metadata();
								((GPXFile) parse).metadata = metadata;
								parserState.push(metadata);
							}
							if (tag.equals("trk")) {
								Track track = new Track();
								((GPXFile) parse).tracks.add(track);
								parserState.push(track);
							}
							if (tag.equals("rte")) {
								Route route = new Route();
								((GPXFile) parse).routes.add(route);
								parserState.push(route);
							}
							if (tag.equals("wpt")) {
								WptPt wptPt = parseWptAttributes(parser);
								((GPXFile) parse).points.add(wptPt);
								parserState.push(wptPt);
							}
						} else if (parse instanceof Metadata) {
							if (tag.equals("name")) {
								((Metadata) parse).name = readText(parser, "name");
							}
							if (tag.equals("desc")) {
								((Metadata) parse).desc = readText(parser, "desc");
							}
							if (tag.equals("author")) {
								Author author = new Author();
								author.name = parser.getText();
								((Metadata) parse).author = author;
								parserState.push(author);
							}
							if (tag.equals("copyright")) {
								Copyright copyright = new Copyright();
								copyright.license = parser.getText();
								copyright.author = parser.getAttributeValue("", "author");
								((Metadata) parse).copyright = copyright;
								parserState.push(copyright);
							}
							if (tag.equals("link")) {
								((Metadata) parse).link = parser.getAttributeValue("", "href");
							}
							if (tag.equals("time")) {
								String text = readText(parser, "time");
								((Metadata) parse).time = parseTime(text);
							}
							if (tag.equals("keywords")) {
								((Metadata) parse).keywords = readText(parser, "keywords");
							}
							if (tag.equals("bounds")) {
								Bounds bounds = parseBoundsAttributes(parser);
								((Metadata) parse).bounds = bounds;
								parserState.push(bounds);
							}
						} else if (parse instanceof Author) {
							if (tag.equals("name")) {
								((Author) parse).name = readText(parser, "name");
							}
							if (tag.equals("email")) {
								String id = parser.getAttributeValue("", "id");
								String domain = parser.getAttributeValue("", "domain");
								if (!Algorithms.isEmpty(id) && !Algorithms.isEmpty(domain)) {
									((Author) parse).email = id + "@" + domain;
								}
							}
							if (tag.equals("link")) {
								((Author) parse).link = parser.getAttributeValue("", "href");
							}
						} else if (parse instanceof Copyright) {
							if (tag.equals("year")) {
								((Copyright) parse).year = readText(parser, "year");
							}
							if (tag.equals("license")) {
								((Copyright) parse).license = readText(parser, "license");
							}
						} else if (parse instanceof Route) {
							if (tag.equals("name")) {
								((Route) parse).name = readText(parser, "name");
							}
							if (tag.equals("desc")) {
								((Route) parse).desc = readText(parser, "desc");
							}
							if (tag.equals("rtept")) {
								WptPt wptPt = parseWptAttributes(parser);
								((Route) parse).points.add(wptPt);
								parserState.push(wptPt);
							}
						} else if (parse instanceof Track) {
							if (tag.equals("name")) {
								((Track) parse).name = readText(parser, "name");
							} else if (tag.equals("desc")) {
								((Track) parse).desc = readText(parser, "desc");
							} else if (tag.equals("trkseg")) {
								TrkSegment trkSeg = new TrkSegment();
								((Track) parse).segments.add(trkSeg);
								parserState.push(trkSeg);
							} else if (tag.equals("trkpt") || tag.equals("rpt")) {
								WptPt wptPt = parseWptAttributes(parser);
								int size = ((Track) parse).segments.size();
								if (size == 0) {
									((Track) parse).segments.add(new TrkSegment());
									size++;
								}
								((Track) parse).segments.get(size - 1).points.add(wptPt);
								parserState.push(wptPt);
							}
						} else if (parse instanceof TrkSegment) {
							if (tag.equals("name")) {
								((TrkSegment) parse).name = readText(parser, "name");
							} else if (tag.equals("trkpt") || tag.equals("rpt")) {
								WptPt wptPt = parseWptAttributes(parser);
								((TrkSegment) parse).points.add(wptPt);
								parserState.push(wptPt);
							}
							if (tag.equals("csvattributes")) {
								String segmentPoints = readText(parser, "csvattributes");
								String[] pointsArr = segmentPoints.split("\n");
								for (int i = 0; i < pointsArr.length; i++) {
									String[] pointAttrs = pointsArr[i].split(",");
									try {
										int arrLength = pointsArr.length;
										if (arrLength > 1) {
											WptPt wptPt = new WptPt();
											wptPt.lon = Double.parseDouble(pointAttrs[0]);
											wptPt.lat = Double.parseDouble(pointAttrs[1]);
											((TrkSegment) parse).points.add(wptPt);
											if (arrLength > 2) {
												wptPt.ele = Double.parseDouble(pointAttrs[2]);
											}
										}
									} catch (NumberFormatException e) {
									}
								}
							}
							// main object to parse
						} else if (parse instanceof WptPt) {
							if (tag.equals("name")) {
								((WptPt) parse).name = readText(parser, "name");
							} else if (tag.equals("desc")) {
								((WptPt) parse).desc = readText(parser, "desc");
							} else if (tag.equals("cmt")) {
								((WptPt) parse).comment = readText(parser, "cmt");
							} else if (tag.equals(POINT_SPEED)) {
								try {
									String value = readText(parser, POINT_SPEED);
									if (!Algorithms.isEmpty(value)) {
										((WptPt) parse).speed = Float.parseFloat(value);
										parse.getExtensionsToWrite().put(POINT_SPEED, value);
									}
								} catch (NumberFormatException e) {
								}
							} else if (tag.equals("link")) {
								((WptPt) parse).link = parser.getAttributeValue("", "href");
							} else if (tag.equals("category")) {
								((WptPt) parse).category = readText(parser, "category");
							} else if (tag.equals("type")) {
								if (((WptPt) parse).category == null) {
									((WptPt) parse).category = readText(parser, "type");
								}
							} else if (tag.equals(POINT_ELEVATION)) {
								String text = readText(parser, POINT_ELEVATION);
								if (text != null) {
									try {
										((WptPt) parse).ele = Float.parseFloat(text);
									} catch (NumberFormatException e) {
									}
								}
							} else if (tag.equals("hdop")) {
								String text = readText(parser, "hdop");
								if (text != null) {
									try {
										((WptPt) parse).hdop = Float.parseFloat(text);
									} catch (NumberFormatException e) {
									}
								}
							} else if (tag.equals("time")) {
								String text = readText(parser, "time");
								((WptPt) parse).time = parseTime(text);
							}
						}
					}

				} else if (tok == XmlPullParser.END_TAG) {
					Object parse = parserState.peek();
					String tag = parser.getName();

					if (tag.equalsIgnoreCase("routepointextension")) {
						routePointExtension = false;
					}
					if (parse != null && tag.equals("extensions")) {
						extensionReadMode = false;
					}
					if (extensionReadMode && tag.equals("route")) {
						routeExtension = false;
						continue;
					}
					if (extensionReadMode && tag.equals("types")) {
						typesExtension = false;
						continue;
					}
					if (extensionReadMode && tag.equals("network_route")) {
						networkRoute = false;
						continue;
					}

					if (tag.equals("metadata")) {
						Object pop = parserState.pop();
						assert pop instanceof Metadata;
					} else if (tag.equals("author")) {
						if (parse instanceof Author) {
							parserState.pop();
						}
					} else if (tag.equals("copyright")) {
						if (parse instanceof Copyright) {
							parserState.pop();
						}
					} else if (tag.equals("bounds")) {
						if (parse instanceof Bounds) {
							parserState.pop();
						}
					} else if (tag.equals("trkpt")) {
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
						if (pop instanceof TrkSegment) {
							TrkSegment segment = (TrkSegment) pop;
							segment.routeSegments = routeSegments;
							segment.routeTypes = routeTypes;
							routeSegments = new ArrayList<>();
							routeTypes = new ArrayList<>();
							if (firstSegment == null) {
								firstSegment = segment;
							}
						}
						assert pop instanceof TrkSegment;
					} else if (tag.equals("rpt")) {
						Object pop = parserState.pop();
						assert pop instanceof WptPt;
					}
				}
			}
			if (!routeTrackSegment.points.isEmpty()) {
				gpxFile.tracks.add(routeTrack);
			}
			if (!routeSegments.isEmpty() && !routeTypes.isEmpty() && firstSegment != null) {
				firstSegment.routeSegments = routeSegments;
				firstSegment.routeTypes = routeTypes;
			}
			if (!pointsGroups.isEmpty() || !gpxFile.points.isEmpty()) {
				gpxFile.pointsGroups.putAll(mergePointsGroups(pointsGroups, gpxFile.points));
			}
			if (addGeneralTrack) {
				gpxFile.addGeneralTrack();
			}
			if (gpxFile.metadata.time == 0) {
				gpxFile.metadata.time = getCreationTime(gpxFile);
			}
		} catch (Exception e) {
			gpxFile.error = e;
			log.error("Error reading gpx", e); //$NON-NLS-1$
		}

		return gpxFile;
	}

	private static String getExtensionsSupportedTag(String tag) {
		String supportedTag = SUPPORTED_EXTENSION_TAGS.get(tag);
		return supportedTag == null ? tag : supportedTag;
	}

	private static Map<String, String> parseRouteKeyAttributes(XmlPullParser parser) {
		Map<String, String> networkRouteKeyTags = new LinkedHashMap<>();
		StringBundleXmlReader reader = new StringBundleXmlReader(parser);
		reader.readBundle();
		StringBundle bundle = reader.getBundle();
		if (!bundle.isEmpty()) {
			for (StringBundle.Item<?> item : bundle.getMap().values()) {
				if (item.getType() == StringBundle.ItemType.STRING) {
					networkRouteKeyTags.put(item.getName(), (String) item.getValue());
				}
			}
		}
		return networkRouteKeyTags;
	}

	private static Map<String, PointsGroup> mergePointsGroups(List<PointsGroup> groups, List<WptPt> points) {
		Map<String, PointsGroup> pointsGroups = new LinkedHashMap<>();
		for (PointsGroup category : groups) {
			pointsGroups.put(category.name, category);
		}

		for (WptPt point : points) {
			String categoryName = point.category != null ? point.category : "";

			PointsGroup pointsGroup = pointsGroups.get(categoryName);
			if (pointsGroup == null) {
				pointsGroup = new PointsGroup(point);
				pointsGroups.put(categoryName, pointsGroup);
			}
			int color = point.getColor();
			if (pointsGroup.color == 0 && color != 0) {
				pointsGroup.color = color;
			}
			String iconName = point.getIconName();
			if (Algorithms.isEmpty(pointsGroup.iconName) && !Algorithms.isEmpty(iconName)) {
				pointsGroup.iconName = iconName;
			}
			String backgroundType = point.getBackgroundType();
			if (Algorithms.isEmpty(pointsGroup.backgroundType) && !Algorithms.isEmpty(backgroundType)) {
				pointsGroup.backgroundType = backgroundType;
			}
			pointsGroup.points.add(point);
		}
		return pointsGroups;
	}

	private static Reader getUTF8Reader(InputStream f) throws IOException {
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

	private static WptPt parseWptAttributes(XmlPullParser parser) {
		WptPt wpt = new WptPt();
		try {
			wpt.lat = Double.parseDouble(parser.getAttributeValue("", "lat")); //$NON-NLS-1$ //$NON-NLS-2$
			wpt.lon = Double.parseDouble(parser.getAttributeValue("", "lon")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (NumberFormatException e) {
			// ignore
		}
		return wpt;
	}

	private static RouteSegment parseRouteSegmentAttributes(XmlPullParser parser) {
		RouteSegment segment = new RouteSegment();
		segment.id = parser.getAttributeValue("", "id");
		segment.length = parser.getAttributeValue("", "length");
		segment.startTrackPointIndex = parser.getAttributeValue("", START_TRKPT_IDX_ATTR);
		segment.segmentTime = parser.getAttributeValue("", "segmentTime");
		segment.speed = parser.getAttributeValue("", "speed");
		segment.turnType = parser.getAttributeValue("", "turnType");
		segment.turnLanes = parser.getAttributeValue("", "turnLanes");
		segment.turnAngle = parser.getAttributeValue("", "turnAngle");
		segment.skipTurn = parser.getAttributeValue("", "skipTurn");
		segment.types = parser.getAttributeValue("", "types");
		segment.pointTypes = parser.getAttributeValue("", "pointTypes");
		segment.names = parser.getAttributeValue("", "names");
		return segment;
	}

	private static RouteType parseRouteTypeAttributes(XmlPullParser parser) {
		RouteType type = new RouteType();
		type.tag = parser.getAttributeValue("", "t");
		type.value = parser.getAttributeValue("", "v");
		return type;
	}

	private static Bounds parseBoundsAttributes(XmlPullParser parser) {
		Bounds bounds = new Bounds();
		try {
			String minlat = parser.getAttributeValue("", "minlat");
			String minlon = parser.getAttributeValue("", "minlon");
			String maxlat = parser.getAttributeValue("", "maxlat");
			String maxlon = parser.getAttributeValue("", "maxlon");

			if (minlat == null) {
				minlat = parser.getAttributeValue("", "minLat");
			}
			if (minlon == null) {
				minlon = parser.getAttributeValue("", "minLon");
			}
			if (maxlat == null) {
				maxlat = parser.getAttributeValue("", "maxLat");
			}
			if (maxlon == null) {
				maxlon = parser.getAttributeValue("", "maxLon");
			}

			if (minlat != null) {
				bounds.minlat = Double.parseDouble(minlat);
			}
			if (minlon != null) {
				bounds.minlon = Double.parseDouble(minlon);
			}
			if (maxlat != null) {
				bounds.maxlat = Double.parseDouble(maxlat);
			}
			if (maxlon != null) {
				bounds.maxlon = Double.parseDouble(maxlon);
			}
		} catch (NumberFormatException e) {
			// ignore
		}
		return bounds;
	}

	public static void mergeGPXFileInto(GPXFile to, GPXFile from) {
		if (from == null) {
			return;
		}
		if (from.showCurrentTrack) {
			to.showCurrentTrack = true;
		}
		if (!Algorithms.isEmpty(from.points)) {
			to.addPoints(from.points);
		}
		if (from.tracks != null) {
			to.tracks.addAll(from.tracks);
		}
		if (from.routes != null) {
			to.routes.addAll(from.routes);
		}
		if (from.error != null) {
			to.error = from.error;
		}
	}

	public static WptPt projectionOnPrimeMeridian(WptPt previous, WptPt next) {
		double lat = MapUtils.getProjection(0, 0, previous.lat, previous.lon, next.lat, next.lon)
				.getLatitude();
		double lon = previous.lon < 0 ? -PRIME_MERIDIAN : PRIME_MERIDIAN;
		double projectionCoeff = MapUtils.getProjectionCoeff(0, 0, previous.lat, previous.lon,
				next.lat, next.lon);
		long time = (long) (previous.time + (next.time - previous.time) * projectionCoeff);
		double ele = Double.isNaN(previous.ele + next.ele)
				? Double.NaN
				: previous.ele + (next.ele - previous.ele) * projectionCoeff;
		double speed = previous.speed + (next.speed - previous.speed) * projectionCoeff;
		return new WptPt(lat, lon, time, ele, speed, Double.NaN);
	}
}
