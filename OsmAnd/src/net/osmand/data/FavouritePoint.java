package net.osmand.data;

import static net.osmand.data.SpecialPointType.HOME;
import static net.osmand.data.SpecialPointType.WORK;
import static net.osmand.data.SpecialPointType.HOME;
import static net.osmand.data.SpecialPointType.WORK;
import static net.osmand.shared.gpx.GpxUtilities.DEFAULT_ICON_NAME;
import static net.osmand.plus.mapmarkers.ItineraryDataHelper.CREATION_DATE;
import static net.osmand.plus.mapmarkers.ItineraryDataHelper.VISITED_DATE;
import static net.osmand.plus.myplaces.favorites.FavoriteGroup.PERSONAL_CATEGORY;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
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
	private static final String PICKUP_DATE = "pickup_date";

	public static final BackgroundType DEFAULT_BACKGROUND_TYPE = BackgroundType.CIRCLE;
	public static final int DEFAULT_UI_ICON_ID = R.drawable.mx_special_star;

	private String name;
	private String category;
	private String description;
	private String address;
	private String comment;

	private double latitude;
	private double longitude;
	private double altitude = Double.NaN;

	private int color;
	private int iconId;
	private BackgroundType backgroundType;
	private SpecialPointType specialPointType;

	private long timestamp;
	private long visitedDate;
	private long pickupDate;
	private boolean calendarEvent;

	private String amenityOriginName;
	private Map<String, String> amenityExtensions = new HashMap<>();

	private boolean visible = true;

	public FavouritePoint(double latitude, double longitude, String name, String category) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.category = category;
		this.name = name != null ? name : "";
		timestamp = System.currentTimeMillis();
		initPersonalType();
	}

	public FavouritePoint(double latitude, double longitude, String name, String category, double altitude, long timestamp) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.category = category;
		this.name = name != null ? name : "";
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
		this.address = point.address;
		this.comment = point.comment;
		this.iconId = point.iconId;
		this.backgroundType = point.backgroundType;
		this.altitude = point.altitude;
		this.timestamp = point.timestamp;
		this.visitedDate = point.visitedDate;
		this.pickupDate = point.pickupDate;
		this.calendarEvent = point.calendarEvent;
		this.amenityExtensions = new HashMap<>(point.amenityExtensions);
		initPersonalType();
	}

	private void initPersonalType() {
		if (PERSONAL_CATEGORY.equals(category)) {
			for (SpecialPointType pointType : SpecialPointType.values()) {
				if (Algorithms.stringsEqual(pointType.getName(), this.name)) {
					this.specialPointType = pointType;
				}
			}
		}
	}

	@Nullable
	public SpecialPointType getSpecialPointType() {
		return specialPointType;
	}

	public void setSpecialPointType(@Nullable SpecialPointType pointType) {
		this.specialPointType = pointType;
	}

	public boolean isHomeOrWork() {
		return specialPointType == HOME || specialPointType == WORK;
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

	public String getIconEntryName(@NonNull Context ctx) {
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

	public int getOverlayIconId(@NonNull Context ctx) {
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

	@Nullable
	public String getAmenityOriginName() {
		return amenityOriginName;
	}

	public void setAmenityOriginName(@Nullable String amenityOriginName) {
		this.amenityOriginName = amenityOriginName;
	}

	@NonNull
	public Map<String, String> getAmenityExtensions() {
		return amenityExtensions;
	}

	public void setAmenityExtensions(@NonNull Map<String, String> extensions) {
		amenityExtensions = extensions;
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

	public boolean isBackgroundSet(){
		return backgroundType != null;
	}

	@NonNull
	@Override
	public String toString() {
		return "Favourite " + getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (getClass() != o.getClass()) {
			return false;
		}

		FavouritePoint point = (FavouritePoint) o;
		if (!Algorithms.stringsEqual(name, point.name)) {
			return false;
		}
		if (!Algorithms.stringsEqual(category, point.category)) {
			return false;
		}
		if (!Algorithms.stringsEqual(description, point.description)) {
			return false;
		}
		if (!Algorithms.stringsEqual(amenityOriginName, point.amenityOriginName)) {
			return false;
		}

		return Double.compare(this.latitude, point.latitude) == 0
				&& Double.compare(this.longitude, point.longitude) == 0
				&& Double.compare(this.altitude, point.altitude) == 0
				&& (this.timestamp == point.timestamp)
				&& (this.visitedDate == point.visitedDate)
				&& (this.pickupDate == point.pickupDate)
				&& (this.specialPointType == point.specialPointType)
				&& appearanceEquals(point);
	}

	public boolean appearanceEquals(@NonNull FavouritePoint point) {
		return (this.color == point.color)
				&& (this.iconId == point.iconId)
				&& (this.backgroundType == point.backgroundType);
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
		return result;
	}

	public void copyAppearance(@NonNull FavouritePoint point) {
		setColor(point.getColor());
		setIconId(point.getIconId());
		setBackgroundType(point.getBackgroundType());
	}

	public static FavouritePoint fromWpt(@NonNull WptPt pt) {
		return fromWpt(pt, null);
	}

	public static FavouritePoint fromWpt(@NonNull WptPt wptPt, @Nullable String category) {
		String name = wptPt.getName() != null ? wptPt.getName() : "";
		if (category == null) {
			category = wptPt.getCategory() != null ? wptPt.getCategory() : "";
		}
		FavouritePoint point = new FavouritePoint(wptPt.getLat(), wptPt.getLon(), name, category, wptPt.getEle(), wptPt.getTime());
		point.setDescription(wptPt.getDesc());
		point.setComment(wptPt.getComment());
		point.setAmenityOriginName(wptPt.getAmenityOriginName());
		point.setAmenityExtensions(wptPt.getExtensionsToRead());

		Map<String, String> extensions = wptPt.getExtensionsToWrite();
		if (extensions.containsKey(VISITED_DATE)) {
			String time = extensions.get(VISITED_DATE);
			if (!Algorithms.isEmpty(time)) {
				point.setVisitedDate(GpxUtilities.INSTANCE.parseTime(time));
			}
		}
		String time = extensions.get(PICKUP_DATE);
		if (time == null) {
			time = extensions.get(CREATION_DATE);
		}
		if (!Algorithms.isEmpty(time)) {
			point.setPickupDate(GpxUtilities.INSTANCE.parseTime(time));
		}
		if (extensions.containsKey(CALENDAR_EXTENSION)) {
			String calendarEvent = extensions.get(CALENDAR_EXTENSION);
			point.setCalendarEvent(Boolean.parseBoolean(calendarEvent));
		}
		point.setColor(wptPt.getColor(0));

		point.setVisible(!wptPt.isHidden());

		point.setAddress(wptPt.getAddress());

		String iconName = wptPt.getIconName();
		if (iconName != null) {
			point.setIconIdFromName(iconName);
		}
		point.setSpecialPointType(SpecialPointType.getByName(wptPt.getSpecialPointType()));
		point.setBackgroundType(BackgroundType.getByTypeName(wptPt.getBackgroundType(), null));

		return point;
	}

	public WptPt toWpt(@NonNull Context ctx) {
		WptPt point = new WptPt();
		point.setLat(getLatitude());
		point.setLon(getLongitude());
		point.setEle(getAltitude());
		point.setTime(getTimestamp());
		point.setName(getName());
		point.setDesc(getDescription());
		point.setComment(getComment());

		if (!Algorithms.isEmpty(getCategory())) {
			point.setCategory(getCategory());
		}
		Map<String, String> extensions = point.getExtensionsToWrite();
		extensions.putAll(getAmenityExtensions());
		if (isVisible()) {
			extensions.remove(HIDDEN);
		} else {
			extensions.put(HIDDEN, "true");
		}
		if (isAddressSpecified()) {
			point.setAddress(getAddress());
		}
		if (getVisitedDate() > 0) {
			extensions.put(VISITED_DATE, GpxUtilities.INSTANCE.formatTime(getVisitedDate()));
		}
		if (getPickupDate() > 0) {
			extensions.put(PICKUP_DATE, GpxUtilities.INSTANCE.formatTime(getPickupDate()));
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
		if (specialPointType != null) {
			point.setSpecialPointType(specialPointType.getName());
		}
		if (getColor() != 0) {
			point.setColor(getColor());
		}
		if (!Algorithms.isEmpty(amenityOriginName)) {
			point.setAmenityOriginName(amenityOriginName);
		}
		return point;
	}
}
