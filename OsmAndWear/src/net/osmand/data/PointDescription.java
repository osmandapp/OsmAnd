package net.osmand.data;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.openlocationcode.OpenLocationCode;

import net.osmand.LocationConvert;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.LinkedHashMap;
import java.util.Map;

public class PointDescription {

	private static final Log log = PlatformUtil.getLog(PointDescription.class);

	private String type = "";
	private String name = "";
	private String typeName;
	private String iconName;

	private double lat;
	private double lon;

	public static final String POINT_TYPE_FAVORITE = "favorite";
	public static final String POINT_TYPE_WPT = "wpt";
	public static final String POINT_TYPE_GPX = "gpx";
	public static final String POINT_TYPE_ROUTE = "route";
	public static final String POINT_TYPE_POI = "poi";
	public static final String POINT_TYPE_ADDRESS = "address";
	public static final String POINT_TYPE_OSM_NOTE = "osm_note";
	public static final String POINT_TYPE_MARKER = "marker";
	public static final String POINT_TYPE_PARKING_MARKER = "parking_marker";
	public static final String POINT_TYPE_AUDIO_NOTE = "audionote";
	public static final String POINT_TYPE_VIDEO_NOTE = "videonote";
	public static final String POINT_TYPE_PHOTO_NOTE = "photonote";
	public static final String POINT_TYPE_LOCATION = "location";
	public static final String POINT_TYPE_MY_LOCATION = "my_location";
	public static final String POINT_TYPE_ALARM = "alarm";
	public static final String POINT_TYPE_TARGET = "destination";
	public static final String POINT_TYPE_MAP_MARKER = "map_marker";
	public static final String POINT_TYPE_OSM_BUG = "bug";
	public static final String POINT_TYPE_WORLD_REGION = "world_region";
	public static final String POINT_TYPE_GPX_FILE = "gpx_file";
	public static final String POINT_TYPE_WORLD_REGION_SHOW_ON_MAP = "world_region_show_on_map";
	public static final String POINT_TYPE_BLOCKED_ROAD = "blocked_road";
	public static final String POINT_TYPE_TRANSPORT_ROUTE = "transport_route";
	public static final String POINT_TYPE_TRANSPORT_STOP = "transport_stop";
	public static final String POINT_TYPE_MAPILLARY_IMAGE = "mapillary_image";
	public static final String POINT_TYPE_POI_TYPE = "poi_type";
	public static final String POINT_TYPE_CUSTOM_POI_FILTER = "custom_poi_filter";

	public static final int LOCATION_URL = 200;
	public static final int OSM_LOCATION_URL = 210;
	public static final int LOCATION_LIST_HEADER = 201;


	public static final PointDescription LOCATION_POINT = new PointDescription(POINT_TYPE_LOCATION, "");

	public PointDescription(double lat, double lon) {
		this(POINT_TYPE_LOCATION, "");
		this.lat = lat;
		this.lon = lon;
	}

	public PointDescription(String type, String name) {
		this.type = type;
		this.name = name;
		if (this.name == null) {
			this.name = "";
		}
	}

	public PointDescription(String type, String typeName, String name) {
		this.type = type;
		this.name = name;
		this.typeName = typeName;
		if (this.name == null) {
			this.name = "";
		}
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public void setName(String name) {
		this.name = name;
		if (this.name == null) {
			this.name = "";
		}
	}

	public String getTypeName() {
		return typeName;
	}

	@Nullable
	public String getIconName() {
		return iconName;
	}

	public void setIconName(String iconName) {
		this.iconName = iconName;
	}

	@NonNull
	public String getName() {
		return name;
	}

	@NonNull
	public String getSimpleName(Context ctx, boolean addTypeName) {
		if (isLocation()) {
			if (!Algorithms.isEmpty(name) && !name.equals(ctx.getString(R.string.no_address_found))) {
				return name;
			} else {
				return getLocationName(ctx, lat, lon, true).replace('\n', ' ');
			}
		}
		if (!Algorithms.isEmpty(typeName)) {
			if (Algorithms.isEmpty(name)) {
				return typeName;
			} else if (addTypeName) {
				return typeName.trim() + ": " + name;
			}
		}
		return name;
	}

	public String getFullPlainName(Context ctx) {
		if (isLocation()) {
			return getLocationName(ctx, lat, lon, false);
		} else {
			String typeName = this.typeName;
			if (isFavorite()) {
				typeName = ctx.getString(R.string.favorite);
			} else if (isPoi()) {
				typeName = ctx.getString(R.string.poi);
			} else if (isWpt()) {
				return ctx.getString(R.string.shared_string_waypoint);
			}
			if (!Algorithms.isEmpty(typeName)) {
				if (Algorithms.isEmpty(name)) {
					return typeName;
				} else {
					return typeName.trim() + ": " + name;
				}
			}
			return name;
		}
	}

	public static String getLocationName(Context ctx, double lat, double lon, boolean sh) {
		OsmandSettings st = ((OsmandApplication) ctx.getApplicationContext()).getSettings();
		int f = st.COORDINATES_FORMAT.get();
		return OsmAndFormatter.getFormattedCoordinates(lat, lon, f);
	}

	public static Map<Integer, String> getLocationData(MapActivity ctx, double lat, double lon, boolean sh) {
		OsmandSettings settings = ((OsmandApplication) ctx.getApplicationContext()).getSettings();
		Map<Integer, String> results = new LinkedHashMap<>();

		String latLonString;
		String latLonDeg;
		String latLonMin;
		String latLonSec;

		String utm = OsmAndFormatter.getFormattedCoordinates(lat, lon, OsmAndFormatter.UTM_FORMAT);
		String olc = OsmAndFormatter.getFormattedCoordinates(lat, lon, OsmAndFormatter.OLC_FORMAT);
		String mgrs = OsmAndFormatter.getFormattedCoordinates(lat, lon, OsmAndFormatter.MGRS_FORMAT);
		String swissGrid = OsmAndFormatter.getFormattedCoordinates(lat, lon, OsmAndFormatter.SWISS_GRID_FORMAT);
		String swissGridPlus = OsmAndFormatter.getFormattedCoordinates(lat, lon, OsmAndFormatter.SWISS_GRID_PLUS_FORMAT);

		try {
			latLonString = OsmAndFormatter.getFormattedCoordinates(lat, lon, OsmAndFormatter.FORMAT_DEGREES_SHORT);
			latLonDeg = OsmAndFormatter.getFormattedCoordinates(lat, lon, OsmAndFormatter.FORMAT_DEGREES);
			latLonMin = OsmAndFormatter.getFormattedCoordinates(lat, lon, OsmAndFormatter.FORMAT_MINUTES);
			latLonSec = OsmAndFormatter.getFormattedCoordinates(lat, lon, OsmAndFormatter.FORMAT_SECONDS);
		} catch (RuntimeException e) {
			latLonString = "0, 0";
			latLonDeg = "0°, 0°";
			latLonMin = "0° 0′, 0° 0′";
			latLonSec = "0° 0′ 0″, 0° 0′ 0″";
		}

		results.put(OsmAndFormatter.FORMAT_DEGREES_SHORT, latLonString);
		results.put(OsmAndFormatter.FORMAT_DEGREES, latLonDeg);
		results.put(OsmAndFormatter.FORMAT_MINUTES, latLonMin);
		results.put(OsmAndFormatter.FORMAT_SECONDS, latLonSec);
		results.put(OsmAndFormatter.UTM_FORMAT, utm);
		results.put(OsmAndFormatter.OLC_FORMAT, olc);
		results.put(OsmAndFormatter.MGRS_FORMAT, mgrs);
		results.put(OsmAndFormatter.SWISS_GRID_FORMAT, swissGrid);
		results.put(OsmAndFormatter.SWISS_GRID_PLUS_FORMAT, swissGridPlus);

		try {
			int zoom = ctx.getMapView().getZoom();
			String latUrl = LocationConvert.convertLatitude(lat, LocationConvert.FORMAT_DEGREES, false);
			String lonUrl = LocationConvert.convertLongitude(lon, LocationConvert.FORMAT_DEGREES, false);
			latUrl = latUrl.substring(0, latUrl.length() - 1);
			lonUrl = lonUrl.substring(0, lonUrl.length() - 1);
			String httpUrl = "https://osmand.net/map?pin=" + latUrl + "," + lonUrl + "#" + zoom + "/" + latUrl + "/" + lonUrl;
			results.put(LOCATION_URL, httpUrl);

			if (PluginsHelper.isEnabled(OsmEditingPlugin.class)) {
				String osmUrl = "https://www.openstreetmap.org/?mlat=" + latUrl + "&mlon=" + lonUrl + "#map=" + zoom + "/" + latUrl + "/" + lonUrl;
				results.put(OSM_LOCATION_URL, osmUrl);
			}
		} catch (RuntimeException e) {
			log.error("Failed to convert coordinates", e);
		}

		int format = settings.COORDINATES_FORMAT.get();

		if (format == PointDescription.UTM_FORMAT) {
			results.put(LOCATION_LIST_HEADER, utm);
		} else if (format == PointDescription.OLC_FORMAT) {
			results.put(LOCATION_LIST_HEADER, olc);
		} else if (format == PointDescription.MGRS_FORMAT) {
			results.put(LOCATION_LIST_HEADER, mgrs);
		} else if (format == PointDescription.SWISS_GRID_FORMAT) {
			results.put(LOCATION_LIST_HEADER, swissGrid);
		} else if (format == PointDescription.SWISS_GRID_PLUS_FORMAT) {
			results.put(LOCATION_LIST_HEADER, swissGridPlus);
		} else if (format == PointDescription.FORMAT_DEGREES) {
			results.put(LOCATION_LIST_HEADER, latLonDeg);
		} else if (format == PointDescription.FORMAT_MINUTES) {
			results.put(LOCATION_LIST_HEADER, latLonMin);
		} else if (format == PointDescription.FORMAT_SECONDS) {
			results.put(LOCATION_LIST_HEADER, latLonSec);
		}
		return results;
	}

	public static String getLocationNamePlain(Context ctx, double lat, double lon) {
		OsmandSettings st = ((OsmandApplication) ctx.getApplicationContext()).getSettings();
		int f = st.COORDINATES_FORMAT.get();
		return OsmAndFormatter.getFormattedCoordinates(lat, lon, f);

	}

	public static String getLocationOlcName(double lat, double lon) {
		return OpenLocationCode.encode(lat, lon);
	}

	public boolean contextMenuDisabled() {
		return POINT_TYPE_WORLD_REGION_SHOW_ON_MAP.equals(type);
	}

	public boolean isLocation() {
		return POINT_TYPE_LOCATION.equals(type);
	}

	public boolean isAddress() {
		return POINT_TYPE_ADDRESS.equals(type);
	}

	public boolean isWpt() {
		return POINT_TYPE_WPT.equals(type);
	}

	public boolean isPoi() {
		return POINT_TYPE_POI.equals(type);
	}

	public boolean isFavorite() {
		return POINT_TYPE_FAVORITE.equals(type);
	}

	public boolean isAudioNote() {
		return POINT_TYPE_AUDIO_NOTE.equals(type);
	}

	public boolean isVideoNote() {
		return POINT_TYPE_VIDEO_NOTE.equals(type);
	}

	public boolean isPhotoNote() {
		return POINT_TYPE_PHOTO_NOTE.equals(type);
	}

	public boolean isDestination() {
		return POINT_TYPE_TARGET.equals(type);
	}

	public boolean isMapMarker() {
		return POINT_TYPE_MAP_MARKER.equals(type);
	}

	public boolean isParking() {
		return POINT_TYPE_PARKING_MARKER.equals(type);
	}

	public boolean isMyLocation() {
		return POINT_TYPE_MY_LOCATION.equals(type);
	}

	public boolean isPoiType() {
		return POINT_TYPE_POI_TYPE.equals(type);
	}

	public boolean isCustomPoiFilter() {
		return POINT_TYPE_CUSTOM_POI_FILTER.equals(type);
	}

	public boolean isGpxPoint() {
		return POINT_TYPE_GPX.equals(type);
	}

	public boolean isRoutePoint() {
		return POINT_TYPE_ROUTE.equals(type);
	}

	public boolean isGpxFile() {
		return POINT_TYPE_GPX_FILE.equals(type);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
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
		PointDescription other = (PointDescription) obj;
		return Algorithms.objectEquals(other.name, name)
				&& Algorithms.objectEquals(other.type, type)
				&& Algorithms.objectEquals(other.lat, lat)
				&& Algorithms.objectEquals(other.lon, lon)
				&& Algorithms.objectEquals(other.typeName, typeName);
	}


	public static String getSimpleName(LocationPoint o, Context ctx) {
		PointDescription pd = o.getPointDescription(ctx);
		return pd.getSimpleName(ctx, true);
	}

	public boolean isSearchingAddress(Context ctx) {
		return !Algorithms.isEmpty(name) && isLocation() && name.equals(getSearchAddressStr(ctx));
	}

	public static String getSearchAddressStr(Context ctx) {
		return ctx.getString(R.string.looking_up_address) + ctx.getString(R.string.shared_string_ellipsis);
	}

	public static String getAddressNotFoundStr(Context ctx) {
		return ctx.getString(R.string.no_address_found);
	}

	public static String serializeToString(PointDescription p) {
		if (p == null) {
			return "";
		}
		String tp = p.type;
		if (!Algorithms.isEmpty(p.typeName)) {
			tp = tp + '.' + p.typeName;
		}
		String res = tp + "#" + p.name;
		if (!Algorithms.isEmpty(p.iconName)) {
			res += "#" + p.iconName;
		}
		return res;
	}

	public static PointDescription deserializeFromString(String s, LatLon l) {
		PointDescription pd = null;
		if (s != null && s.length() > 0) {
			int in = s.indexOf('#');
			if (in >= 0) {
				int ii = s.indexOf('#', in + 1);
				String name;
				String icon = null;
				if (ii > 0) {
					name = s.substring(in + 1, ii).trim();
					icon = s.substring(ii + 1).trim();
				} else {
					name = s.substring(in + 1).trim();
				}
				String tp = s.substring(0, in);
				if (tp.contains(".")) {
					pd = new PointDescription(tp.substring(0, tp.indexOf('.')), tp.substring(tp.indexOf('.') + 1), name);
				} else {
					pd = new PointDescription(tp, name);
				}
				if (!Algorithms.isEmpty(icon)) {
					pd.setIconName(icon);
				}
			}
		}
		if (pd == null) {
			pd = new PointDescription(POINT_TYPE_LOCATION, "");
		}
		if (pd.isLocation() && l != null) {
			pd.lat = l.getLatitude();
			pd.lon = l.getLongitude();
		}
		return pd;
	}

	public static final int FORMAT_DEGREES = LocationConvert.FORMAT_DEGREES;
	public static final int FORMAT_MINUTES = LocationConvert.FORMAT_MINUTES;
	public static final int FORMAT_SECONDS = LocationConvert.FORMAT_SECONDS;
	public static final int UTM_FORMAT = LocationConvert.UTM_FORMAT;
	public static final int OLC_FORMAT = LocationConvert.OLC_FORMAT;
	public static final int MGRS_FORMAT = LocationConvert.MGRS_FORMAT;
	public static final int SWISS_GRID_FORMAT = LocationConvert.SWISS_GRID_FORMAT;
	public static final int SWISS_GRID_PLUS_FORMAT = LocationConvert.SWISS_GRID_PLUS_FORMAT;

	public static String formatToHumanString(Context ctx, int format) {
		switch (format) {
			case LocationConvert.FORMAT_DEGREES:
				return ctx.getString(R.string.navigate_point_format_D);
			case LocationConvert.FORMAT_MINUTES:
				return ctx.getString(R.string.navigate_point_format_DM);
			case LocationConvert.FORMAT_SECONDS:
				return ctx.getString(R.string.navigate_point_format_DMS);
			case LocationConvert.UTM_FORMAT:
				return "UTM";
			case LocationConvert.OLC_FORMAT:
				return "OLC";
			case LocationConvert.MGRS_FORMAT:
				return "MGRS";
			case LocationConvert.SWISS_GRID_FORMAT:
				return ctx.getString(R.string.navigate_point_format_swiss_grid);
			case LocationConvert.SWISS_GRID_PLUS_FORMAT:
				return ctx.getString(R.string.navigate_point_format_swiss_grid_plus);
			default:
				return "Unknown format";
		}
	}

	@DrawableRes
	public int getItemIcon() {
		if (isAddress()) {
			return R.drawable.ic_action_street_name;
		} else if (isFavorite()) {
			return R.drawable.ic_action_favorite;
		} else if (isLocation()) {
			return R.drawable.ic_action_marker_dark;
		} else if (isPoi()) {
			return R.drawable.ic_action_info_dark;
		} else if (isGpxFile() || isGpxPoint()) {
			return R.drawable.ic_action_polygom_dark;
		} else if (isWpt()) {
			return R.drawable.ic_action_flag_stroke;
		} else if (isAudioNote()) {
			return R.drawable.ic_type_audio;
		} else if (isVideoNote()) {
			return R.drawable.ic_type_video;
		} else if (isPhotoNote()) {
			return R.drawable.ic_type_img;
		} else {
			return R.drawable.ic_action_street_name;
		}
	}
}
