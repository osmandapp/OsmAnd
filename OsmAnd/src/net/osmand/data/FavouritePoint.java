package net.osmand.data;

import static net.osmand.GPXUtilities.DEFAULT_ICON_NAME;
import static net.osmand.plus.mapmarkers.ItineraryDataHelper.CREATION_DATE;
import static net.osmand.plus.mapmarkers.ItineraryDataHelper.VISITED_DATE;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.FavoriteGroup;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.preferences.BooleanPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.util.Algorithms;

import java.io.Serializable;


public class FavouritePoint implements Serializable, LocationPoint {
	private static final long serialVersionUID = 729654300829771466L;

	private static final String DELIMITER = "__";
	private static final String HIDDEN = "hidden";
	private static final String CALENDAR_EXTENSION = "calendar_event";

	public static final BackgroundType DEFAULT_BACKGROUND_TYPE = BackgroundType.CIRCLE;
	public static final int DEFAULT_UI_ICON_ID = R.drawable.mx_special_star;

	protected String name = "";
	protected String description;
	protected String category = "";
	protected String address = "";
	protected int iconId;
	private String originObjectName = "";
	private double latitude;
	private double longitude;
	private int color;
	private boolean visible = true;
	private SpecialPointType specialPointType = null;
	private BackgroundType backgroundType = null;
	private double altitude = Double.NaN;
	private long timestamp;
	private long visitedDate;
	private long creationDate;
	private boolean calendarEvent;

	public FavouritePoint() {
	}

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

	public FavouritePoint(FavouritePoint favouritePoint) {
		this.latitude = favouritePoint.latitude;
		this.longitude = favouritePoint.longitude;
		this.category = favouritePoint.category;
		this.name = favouritePoint.name;
		this.color = favouritePoint.color;
		this.description = favouritePoint.description;
		this.visible = favouritePoint.visible;
		this.originObjectName = favouritePoint.originObjectName;
		this.address = favouritePoint.address;
		this.iconId = favouritePoint.iconId;
		this.backgroundType = favouritePoint.backgroundType;
		this.altitude = favouritePoint.altitude;
		this.timestamp = favouritePoint.timestamp;
		this.visitedDate = favouritePoint.visitedDate;
		this.creationDate = favouritePoint.creationDate;
		initPersonalType();
	}

	private void initPersonalType() {
		if (FavoriteGroup.PERSONAL_CATEGORY.equals(category)) {
			for (SpecialPointType p : SpecialPointType.values()) {
				if (p.typeName.equals(this.name)) {
					this.specialPointType = p;
				}
			}
		}
	}

	public void initAltitude(OsmandApplication app) {
		initAltitude(app, null);
	}

	public void initAltitude(OsmandApplication app, final Runnable callback) {
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

	public String getOriginObjectName() {
		return originObjectName;
	}

	public void setOriginObjectName(String originObjectName) {
		this.originObjectName = originObjectName;
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

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public String getCategory() {
		return category;
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

		if (originObjectName == null) {
			if (fp.originObjectName != null)
				return false;
		} else if (!originObjectName.equals(fp.originObjectName))
			return false;

		return (this.latitude == fp.latitude)
				&& (this.longitude == fp.longitude)
				&& (this.altitude == fp.altitude)
				&& (this.timestamp == fp.timestamp)
				&& (this.visitedDate == fp.visitedDate)
				&& (this.creationDate == fp.creationDate);
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
		result = prime * result + (int) (creationDate ^ (creationDate >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((originObjectName == null) ? 0 : originObjectName.hashCode());
		return result;
	}

	public enum SpecialPointType {
		HOME("home", R.string.home_button, R.drawable.mx_special_house),
		WORK("work", R.string.work_button, R.drawable.mx_special_building),
		PARKING("parking", R.string.osmand_parking_position_name, R.drawable.mx_parking);

		private String typeName;
		@StringRes
		private int resId;
		@DrawableRes
		private int iconId;

		SpecialPointType(@NonNull String typeName, @StringRes int resId, @DrawableRes int iconId) {
			this.typeName = typeName;
			this.resId = resId;
			this.iconId = iconId;
		}

		public String getCategory() {
			return FavoriteGroup.PERSONAL_CATEGORY;
		}

		public String getName() {
			return typeName;
		}

		public int getIconId(@NonNull Context ctx) {
			if (this == PARKING) {
				OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
				OsmandPreference parkingType = app.getSettings().getPreference(ParkingPositionPlugin.PARKING_TYPE);
				if (parkingType instanceof BooleanPreference && ((BooleanPreference) parkingType).get()) {
					return R.drawable.mx_special_parking_time_limited;
				}
				return iconId;
			}
			return iconId;
		}

		public String getHumanString(@NonNull Context ctx) {
			return ctx.getString(resId);
		}
	}

	public static FavouritePoint fromWpt(@NonNull WptPt pt, @NonNull Context ctx) {
		return fromWpt(pt, ctx, null);
	}

	public static FavouritePoint fromWpt(@NonNull WptPt pt, @NonNull Context ctx, String category) {
		String name = pt.name;
		String categoryName = category != null ? category :
				(pt.category != null ? pt.category : "");
		if (name == null) {
			name = "";
		}
		FavouritePoint fp = new FavouritePoint(pt.lat, pt.lon, name, categoryName, pt.ele, pt.time);
		fp.setDescription(pt.desc);
		if (pt.comment != null) {
			fp.setOriginObjectName(pt.comment);
		}
		if (pt.getExtensionsToWrite().containsKey(VISITED_DATE)) {
			String time = pt.getExtensionsToWrite().get(VISITED_DATE);
			fp.setVisitedDate(GPXUtilities.parseTime(time));
		}
		if (pt.getExtensionsToWrite().containsKey(CREATION_DATE)) {
			String time = pt.getExtensionsToWrite().get(CREATION_DATE);
			fp.setCreationDate(GPXUtilities.parseTime(time));
		}
		if (pt.getExtensionsToWrite().containsKey(CALENDAR_EXTENSION)) {
			String calendarEvent = pt.getExtensionsToWrite().get(CALENDAR_EXTENSION);
			fp.setCalendarEvent(calendarEvent.equals("true"));
		}
		fp.setColor(pt.getColor(0));
		fp.setVisible(!pt.getExtensionsToRead().containsKey(HIDDEN));
		fp.setAddress(pt.getAddress());
		String iconName = pt.getIconName();
		if (iconName != null) {
			fp.setIconIdFromName(iconName);
		}
		BackgroundType backgroundType = BackgroundType.getByTypeName(pt.getBackgroundType(), null);
		fp.setBackgroundType(backgroundType);
		return fp;
	}

	public WptPt toWpt(@NonNull Context ctx) {
		WptPt pt = new WptPt();
		pt.lat = getLatitude();
		pt.lon = getLongitude();
		pt.ele = getAltitude();
		pt.time = getTimestamp();
		if (!isVisible()) {
			pt.getExtensionsToWrite().put(HIDDEN, "true");
		}
		if (isAddressSpecified()) {
			pt.setAddress(getAddress());
		}
		if (getVisitedDate() != 0) {
			pt.getExtensionsToWrite().put(VISITED_DATE, GPXUtilities.formatTime(getVisitedDate()));
		}
		if (getCreationDate() != 0) {
			pt.getExtensionsToWrite().put(CREATION_DATE, GPXUtilities.formatTime(getCreationDate()));
		}
		if (getCalendarEvent()) {
			pt.getExtensionsToWrite().put(CALENDAR_EXTENSION, "true");
		}
		if (iconId != 0) {
			pt.setIconName(getIconEntryName(ctx).substring(3));
		}
		if (backgroundType != null) {
			pt.setBackgroundType(backgroundType.getTypeName());
		}
		if (getColor() != 0) {
			pt.setColor(getColor());
		}
		pt.name = getName();
		pt.desc = getDescription();
		if (getCategory().length() > 0)
			pt.category = getCategory();
		if (getOriginObjectName().length() > 0) {
			pt.comment = getOriginObjectName();
		}
		return pt;
	}
}
