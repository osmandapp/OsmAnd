package net.osmand.data;

import static net.osmand.GPXUtilities.DEFAULT_ICON_NAME;
import static net.osmand.plus.mapmarkers.ItineraryDataHelper.CREATION_DATE;
import static net.osmand.plus.mapmarkers.ItineraryDataHelper.VISITED_DATE;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class FavouritePoint implements Serializable, LocationPoint {
	private static final long serialVersionUID = 729654300829771466L;

	private static final String DELIMITER = "__";
	private static final String HIDDEN = "hidden";
	private static final String CALENDAR_EXTENSION = "calendar_event";
	public static final String PICKUP_DATE = "pickup_date";

	public static final BackgroundType DEFAULT_BACKGROUND_TYPE = BackgroundType.CIRCLE;
	public static final int DEFAULT_UI_ICON_ID = R.drawable.mx_special_star;

	protected String name = "";
	protected String description;
	protected String category = "";
	protected String address = "";
	protected int iconId;
	private String amenityOriginName = null;
	private String transportStopOriginName = null;
	private String comment = "";
	private double latitude;
	private double longitude;
	private int color;
	private boolean visible = true;
	private SpecialPointType specialPointType;
	private BackgroundType backgroundType;
	private double altitude = Double.NaN;
	private long timestamp;
	private long visitedDate;
	private long pickupDate;
	private boolean calendarEvent;
	private Map<String, String> extensions = new HashMap<String, String>();

	public FavouritePoint(double latitude, double longitude, String name, String category) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.category = category;
		if (name == null) {
			name = "";
		}
		this.name = name;
		timestamp = System.currentTimeMillis();
		initPersonalType();
	}

	public FavouritePoint(double latitude, double longitude, String name, String category, double altitude, long timestamp) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.category = category;
		if (name == null) {
			name = "";
		}
		this.name = name;
		this.altitude = altitude;
		this.timestamp = timestamp != 0 ? timestamp : System.currentTimeMillis();
		initPersonalType();
	}

	public FavouritePoint(@NonNull FavouritePoint point) {
		this.latitude = point.latitude;
		this.longitude = point.longitude;
		this.category = point.category;
		this.name = point.name;
		this.color = point.color;
		this.description = point.description;
		this.visible = point.visible;
		this.amenityOriginName = point.amenityOriginName;
		this.transportStopOriginName = point.transportStopOriginName;
		this.address = point.address;
		this.iconId = point.iconId;
		this.backgroundType = point.backgroundType;
		this.altitude = point.altitude;
		this.timestamp = point.timestamp;
		this.visitedDate = point.visitedDate;
		this.pickupDate = point.pickupDate;
		initPersonalType();
	}

	private void initPersonalType() {
		if (FavoriteGroup.PERSONAL_CATEGORY.equals(category)) {
			for (SpecialPointType pointType : SpecialPointType.values()) {
				if (Algorithms.stringsEqual(pointType.getName(), this.name)) {
					this.specialPointType = pointType;
				}
			}
		}
	}

	public void initAltitude(OsmandApplication app) {
		initAltitude(app, null);
	}

	public void initAltitude(OsmandApplication app, Runnable callback) {
		Location location = new Location("", latitude, longitude);
		app.getLocationProvider().getRouteSegment(location, null, false,
				new ResultMatcher<RouteDataObject>() {

					@Override
					public boolean publish(RouteDataObject routeDataObject) {
						if (routeDataObject != null) {
							LatLon latLon = new LatLon(latitude, longitude);
							routeDataObject.calculateHeightArray(latLon);
							altitude = routeDataObject.heightByCurrentLocation;
						}
						if (callback != null) {
							callback.run();
						}
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});
	}

	public SpecialPointType getSpecialPointType() {
		return specialPointType;
	}

	public boolean isHomeOrWork() {
		return specialPointType == SpecialPointType.HOME || specialPointType == SpecialPointType.WORK;
	}

	public int getColor() {
		return color;
	}

	@Nullable
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public boolean isAddressSpecified() {
		return !Algorithms.isEmpty(address);
	}

	public int getIconId() {
		return iconId;
	}

	public int getIconIdOrDefault() {
		return iconId == 0 ? DEFAULT_UI_ICON_ID : iconId;
	}

	public String getIconEntryName(Context ctx) {
		return ctx.getResources().getResourceEntryName(getOverlayIconId(ctx));
	}

	public void setIconId(int iconId) {
		this.iconId = iconId;
	}

	public void setIconIdFromName(@NonNull String iconName) {
		this.iconId = RenderingIcons.getBigIconResourceId(iconName);
	}

	public String getKey() {
		return name + DELIMITER + category;
	}

	@NonNull
	public String getIconName() {
		String name = RenderingIcons.getBigIconName(iconId);
		return name != null ? name : DEFAULT_ICON_NAME;
	}

	public boolean isSpecialPoint() {
		return specialPointType != null;
	}

	@Override
	public PointDescription getPointDescription(@NonNull Context ctx) {
		return new PointDescription(PointDescription.POINT_TYPE_FAVORITE, getDisplayName(ctx));
	}

	public void setColor(int color) {
		this.color = color;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public String getAmenityOriginName() {
		return amenityOriginName;
	}

	public void setAmenityOriginName(String amenityOriginName) {
		this.amenityOriginName = amenityOriginName;
	}

	public String getTransportStopOriginName() {
		return transportStopOriginName;
	}

	public void setTransportStopOriginName(String transportStopOriginName) {
		this.transportStopOriginName = transportStopOriginName;
	}

	public int getOverlayIconId(Context ctx) {
		if (isSpecialPoint()) {
			return specialPointType.getIconId(ctx);
		}
		return getIconIdOrDefault();
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public boolean getCalendarEvent() {
		return calendarEvent;
	}

	public void setCalendarEvent(boolean calendarEvent) {
		this.calendarEvent = calendarEvent;
	}

	public long getVisitedDate() {
		return visitedDate;
	}

	public void setVisitedDate(long visitedDate) {
		this.visitedDate = visitedDate;
	}

	public long getPickupDate() {
		return pickupDate;
	}

	public void setPickupDate(long pickupDate) {
		this.pickupDate = pickupDate;
	}

	public String getCategory() {
		return category;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Amenity getAmenity() {
		if (!this.extensions.isEmpty()) {
			Amenity amenity = Amenity.fromTagValue(this.extensions, GPXUtilities.PRIVATE_PREFIX, GPXUtilities.OSM_PREFIX);
			if (amenity != null) {
				amenity.setLocation(getLatitude(), getLongitude());
			}
			return amenity;
		}
		return null;
	}

	public void setAmenity(Amenity amenity) {
		if (amenity != null) {
			Map<String, String> extensions = amenity.toTagValue(GPXUtilities.PRIVATE_PREFIX, GPXUtilities.OSM_PREFIX);
			if (!extensions.isEmpty()) {
				this.extensions.putAll(extensions);
			}
		}
	}

	public String getCategoryDisplayName(@NonNull Context ctx) {
		return FavoriteGroup.getDisplayName(ctx, category);
	}

	public void setCategory(String category) {
		this.category = category;
		initPersonalType();
	}

	public String getDisplayName(@NonNull Context ctx) {
		if (isSpecialPoint()) {
			return specialPointType.getHumanString(ctx);
		}
		return name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		initPersonalType();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BackgroundType getBackgroundType() {
		return backgroundType == null ? DEFAULT_BACKGROUND_TYPE : backgroundType;
	}

	public void setBackgroundType(BackgroundType backgroundType) {
		this.backgroundType = backgroundType;
	}

	@NonNull
	@Override
	public String toString() {
		return "Favourite " + getName(); //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null) return false;

		if (getClass() != o.getClass()) return false;

		FavouritePoint fp = (FavouritePoint) o;

		if (name == null) {
			if (fp.name != null)
				return false;
		} else if (!name.equals(fp.name))
			return false;

		if (category == null) {
			if (fp.category != null)
				return false;
		} else if (!category.equals(fp.category))
			return false;

		if (description == null) {
			if (fp.description != null)
				return false;
		} else if (!description.equals(fp.description))
			return false;

		if (amenityOriginName == null) {
			if (fp.amenityOriginName != null)
				return false;
		} else if (!amenityOriginName.equals(fp.amenityOriginName))
			return false;

		if (transportStopOriginName == null) {
			if (fp.transportStopOriginName != null)
				return false;
		} else if (!transportStopOriginName.equals(fp.transportStopOriginName))
			return false;

		return (this.latitude == fp.latitude)
				&& (this.longitude == fp.longitude)
				&& (this.altitude == fp.altitude)
				&& (this.timestamp == fp.timestamp)
				&& (this.visitedDate == fp.visitedDate)
				&& (this.pickupDate == fp.pickupDate);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) Math.floor(latitude * 10000);
		result = prime * result + (int) Math.floor(longitude * 10000);
		result = prime * result + (int) Math.floor(altitude * 10000);
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		result = prime * result + (int) (visitedDate ^ (visitedDate >>> 32));
		result = prime * result + (int) (pickupDate ^ (pickupDate >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((amenityOriginName == null) ? 0 : amenityOriginName.hashCode());
		result = prime * result + ((transportStopOriginName == null) ? 0 : transportStopOriginName.hashCode());
		return result;
	}

	public static FavouritePoint fromWpt(@NonNull WptPt pt) {
		return fromWpt(pt, null);
	}

	public static FavouritePoint fromWpt(@NonNull WptPt wptPt, String category) {
		String name = wptPt.name;
		String categoryName = category != null ? category :
				(wptPt.category != null ? wptPt.category : "");
		if (name == null) {
			name = "";
		}
		FavouritePoint point = new FavouritePoint(wptPt.lat, wptPt.lon, name, categoryName, wptPt.ele, wptPt.time);
		point.extensions = wptPt.getExtensionsToRead();
		point.setDescription(wptPt.desc);
		point.setComment(wptPt.comment);
		if (wptPt.getAmenityOriginName() != null) {
			point.setAmenityOriginName(wptPt.getAmenityOriginName());
		}
		if (wptPt.getTransportStopOriginName() != null) {
			point.setTransportStopOriginName(wptPt.getTransportStopOriginName());
		}
		Map<String, String> extensions = wptPt.getExtensionsToWrite();
		if (extensions.containsKey(VISITED_DATE)) {
			String time = extensions.get(VISITED_DATE);
			point.setVisitedDate(GPXUtilities.parseTime(time));
		}
		String time = extensions.get(PICKUP_DATE);
		if (time == null) {
			time = extensions.get(CREATION_DATE);
		}
		if (!Algorithms.isEmpty(time)) {
			point.setPickupDate(GPXUtilities.parseTime(time));
		}
		if (extensions.containsKey(CALENDAR_EXTENSION)) {
			String calendarEvent = extensions.get(CALENDAR_EXTENSION);
			point.setCalendarEvent(Boolean.parseBoolean(calendarEvent));
		}
		point.setColor(wptPt.getColor(0));
		point.setVisible(!wptPt.getExtensionsToRead().containsKey(HIDDEN));
		point.setAddress(wptPt.getAddress());
		String iconName = wptPt.getIconName();
		if (iconName != null) {
			point.setIconIdFromName(iconName);
		}
		BackgroundType backgroundType = BackgroundType.getByTypeName(wptPt.getBackgroundType(), null);
		point.setBackgroundType(backgroundType);
		return point;
	}

	public WptPt toWpt(@NonNull Context ctx) {
		WptPt point = new WptPt();
		point.lat = getLatitude();
		point.lon = getLongitude();
		point.ele = getAltitude();
		point.time = getTimestamp();
		point.comment = getComment();

		Map<String, String> extensions = point.getExtensionsToWrite();
		extensions.putAll(this.extensions);
		if (!isVisible()) {
			extensions.put(HIDDEN, "true");
		}
		if (isAddressSpecified()) {
			point.setAddress(getAddress());
		}
		if (getVisitedDate() > 0) {
			extensions.put(VISITED_DATE, GPXUtilities.formatTime(getVisitedDate()));
		}
		if (getPickupDate() > 0) {
			extensions.put(PICKUP_DATE, GPXUtilities.formatTime(getPickupDate()));
		}
		if (getCalendarEvent()) {
			extensions.put(CALENDAR_EXTENSION, "true");
		}
		if (iconId != 0) {
			point.setIconName(getIconEntryName(ctx).substring(3));
		}
		if (backgroundType != null) {
			point.setBackgroundType(backgroundType.getTypeName());
		}
		if (getColor() != 0) {
			point.setColor(getColor());
		}
		point.name = getName();
		point.desc = getDescription();
		if (getCategory().length() > 0)
			point.category = getCategory();
		if (!Algorithms.isEmpty(getAmenityOriginName())) {
			point.setAmenityOriginName(getAmenityOriginName());
		}
		if (!Algorithms.isEmpty(getTransportStopOriginName())) {
			point.setTransportStopOriginName(getTransportStopOriginName());
		}
		return point;
	}
}
