package net.osmand.core.samples.android.sample1.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.UTMPoint;

import net.osmand.LocationConvert;
import net.osmand.core.samples.android.sample1.OsmandResources;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.data.LatLon;
import net.osmand.util.Algorithms;

public class PointDescription {
	private String type = "";
	private String name = "";
	private String typeName;
	private String iconName;

	private double lat = 0;
	private double lon = 0;

	public static final String POINT_TYPE_FAVORITE = "favorite";
	public static final String POINT_TYPE_WPT = "wpt";
	public static final String POINT_TYPE_POI = "poi";
	public static final String POINT_TYPE_ADDRESS = "address";
	public static final String POINT_TYPE_OSM_NOTE= "osm_note";
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
	public static final String POINT_TYPE_GPX_ITEM = "gpx_item";
	public static final String POINT_TYPE_WORLD_REGION_SHOW_ON_MAP = "world_region_show_on_map";
	public static final String POINT_TYPE_BLOCKED_ROAD = "blocked_road";
	public static final String POINT_TYPE_TRANSPORT_ROUTE = "transport_route";
	public static final String POINT_TYPE_TRANSPORT_STOP = "transport_stop";


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
	
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public void setLon(double lon) {
		this.lon = lon;
	}

	public void setTypeName(String typeName){
		this.typeName = typeName;
	}

	public void setName(String name){
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
	public String getSimpleName(SampleApplication ctx, boolean addTypeName) {
		if (isLocation()) {
			if (!Algorithms.isEmpty(name) && !name.equals(ctx.getString("no_address_found"))) {
				return name;
			} else {
				return getLocationName(ctx, lat, lon, true).replace('\n', ' ');
			}
		}
		if (!Algorithms.isEmpty(typeName)) {
			if (Algorithms.isEmpty(name)) {
				return typeName;
			} else if(addTypeName){
				return typeName.trim() + ": " + name;
			}
		}
		return name;
	}
	
	public String getFullPlainName(SampleApplication ctx) {
		if (isLocation()) {
			return getLocationName(ctx, lat, lon, false);
		} else {
			String typeName = this.typeName;
			if (isFavorite()) {
				typeName = ctx.getString("favorite");
			} else if (isPoi()) {
				typeName = ctx.getString("poi");
			} else if (isWpt()) {
				return ctx.getString("gpx_wpt");
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

	public static String getLocationName(SampleApplication ctx, double lat, double lon, boolean sh) {
		SampleApplication app = SampleApplication.getApp(ctx);
		int f = app.getCoordinatesFormat();
		if (f == PointDescription.UTM_FORMAT) {
			UTMPoint pnt = new UTMPoint(new LatLonPoint(lat, lon));
			return pnt.zone_number + "" + pnt.zone_letter + " " + ((long) pnt.easting) + " "
					+ ((long) pnt.northing);
		} else {
			try {
				return ctx.getString(sh ? "short_location_on_map" : "location_on_map", LocationConvert.convert(lat, f),
						LocationConvert.convert(lon, f));
			} catch(RuntimeException e) {
				e.printStackTrace();
				return ctx.getString(sh ? "short_location_on_map" : "location_on_map", 0, 0);
			}
		}
	}

	public static String getLocationNamePlain(Context ctx, double lat, double lon) {
		SampleApplication app = SampleApplication.getApp(ctx);
		int f = app.getCoordinatesFormat();
		if (f == PointDescription.UTM_FORMAT) {
			UTMPoint pnt = new UTMPoint(new LatLonPoint(lat, lon));
			return pnt.zone_number + "" + pnt.zone_letter + " " + ((long) pnt.easting) + " "
					+ ((long) pnt.northing);
		} else {
			try {
				return LocationConvert.convert(lat, f) + ", " + LocationConvert.convert(lon, f);
			} catch(RuntimeException e) {
				e.printStackTrace();
				return "0, 0";
			}
		}
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
		result = prime * result + ((lat == 0) ? 0 : new Double(lat).hashCode());
		result = prime * result + ((lon == 0) ? 0 : new Double(lon).hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		PointDescription other = (PointDescription) obj;
		if (Algorithms.objectEquals(other.name, name) 
				&& Algorithms.objectEquals(other.type, type)
				&& Algorithms.objectEquals(other.lat, lat)
				&& Algorithms.objectEquals(other.lon, lon)
				&& Algorithms.objectEquals(other.typeName, typeName)) {
			return true;
		}
		return false;
	}
	
	
	public static String getSimpleName(LocationPoint o, SampleApplication ctx) {
		PointDescription pd = o.getPointDescription(ctx);
		return pd.getSimpleName(ctx, true);
//		return o.getPointDescription(ctx).getFullPlainName(ctx, o.getLatitude(), o.getLongitude());
	}

	public boolean isSearchingAddress(SampleApplication ctx) {
		return !Algorithms.isEmpty(name) && isLocation() && name.equals(getSearchAddressStr(ctx));
	}

	public static String getSearchAddressStr(SampleApplication ctx) {
		return ctx.getString("looking_up_address") + ctx.getString("shared_string_ellipsis");
	}

	public static String getAddressNotFoundStr(SampleApplication ctx) {
		return ctx.getString("no_address_found");
	}

	public static String serializeToString(PointDescription p) {
		if (p == null) {
			return "";
		}
		String tp = p.type ;
		if(!Algorithms.isEmpty(p.typeName)) {
			tp = tp +'.' + p.typeName;
		}
		String res = tp + "#" + p.name;
		if (!Algorithms.isEmpty(p.iconName)) {
			res += "#" + p.iconName;
		}
		return res;
	}

	public static PointDescription deserializeFromString(String s, LatLon l) {
		PointDescription pd = null ;
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
				if(tp.contains(".")) {
					pd = new PointDescription(tp.substring(0, tp.indexOf('.')), tp.substring(tp.indexOf('.') + 1), name);
				} else {
					pd = new PointDescription(tp, name);
				}
				if (!Algorithms.isEmpty(icon)) {
					pd.setIconName(icon);
				}
			}
		}
		if(pd == null) {
			pd = new PointDescription(POINT_TYPE_LOCATION, "");
		}
		if(pd.isLocation() && l != null) {
			pd.setLat(l.getLatitude());
			pd.setLon(l.getLongitude());
		}
		return pd;
	}
	
	public static final int FORMAT_DEGREES = LocationConvert.FORMAT_DEGREES;
	public static final int FORMAT_MINUTES = LocationConvert.FORMAT_MINUTES;
	public static final int FORMAT_SECONDS = LocationConvert.FORMAT_SECONDS;
	public static final int UTM_FORMAT = LocationConvert.UTM_FORMAT;
	
	public static String formatToHumanString(SampleApplication ctx, int format) {
		switch (format) {
			case LocationConvert.FORMAT_DEGREES:
				return ctx.getString("navigate_point_format_D");
			case LocationConvert.FORMAT_MINUTES:
				return ctx.getString("navigate_point_format_DM");
			case LocationConvert.FORMAT_SECONDS:
				return ctx.getString("navigate_point_format_DMS");
			case LocationConvert.UTM_FORMAT:
				return "UTM";
			default:
				return "Unknown format";
		}
	}

	public int getIconId() {
		int iconId;
		if (isAddress()) {
			iconId = OsmandResources.getDrawableId("ic_action_street_name");
		} else if (isFavorite()) {
			iconId = OsmandResources.getDrawableId("ic_action_favorite");
		} else if (isLocation()) {
			iconId = OsmandResources.getDrawableId("ic_action_marker_dark");
		} else if (isPoi()) {
			iconId = OsmandResources.getDrawableId("ic_action_info_dark");
		} else if (isWpt()) {
			iconId = OsmandResources.getDrawableId("ic_action_flag_stroke");
		} else if (isAudioNote()) {
			iconId = OsmandResources.getDrawableId("ic_type_audio");
		} else if (isVideoNote()) {
			iconId = OsmandResources.getDrawableId("ic_type_video");
		}else if (isPhotoNote()) {
			iconId = OsmandResources.getDrawableId("ic_type_img");
		}  else {
			iconId = OsmandResources.getDrawableId("ic_action_street_name");
		}
		return iconId;
	}
}