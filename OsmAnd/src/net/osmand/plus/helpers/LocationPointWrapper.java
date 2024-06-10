package net.osmand.plus.helpers;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LocationPoint;
import net.osmand.data.WptLocationPoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.AlarmInfo;
import net.osmand.plus.settings.enums.DrivingRegion;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.PointImageUtils;

public class LocationPointWrapper {

	public LocationPoint point;
	public float deviationDistance;
	public boolean deviationDirectionRight;
	public int type;

	int routeIndex;
	boolean announce = true;

	public LocationPointWrapper() {
	}

	public LocationPointWrapper(int type, LocationPoint point, float deviationDistance, int routeIndex) {
		this.type = type;
		this.point = point;
		this.deviationDistance = deviationDistance;
		this.routeIndex = routeIndex;
	}

	public void setAnnounce(boolean announce) {
		this.announce = announce;
	}

	public float getDeviationDistance() {
		return deviationDistance;
	}

	public boolean isDeviationDirectionRight() {
		return deviationDirectionRight;
	}

	public LocationPoint getPoint() {
		return point;
	}

	@Nullable
	public Drawable getDrawable(@NonNull Context context, @NonNull OsmandApplication app, boolean nightMode) {
		if (type == WaypointHelper.POI) {
			Amenity amenity = ((AmenityLocationPoint) point).getAmenity();
			String iconName = RenderingIcons.getBigIconNameForAmenity(amenity);
			return iconName == null ? null : AppCompatResources.getDrawable(context,
					RenderingIcons.getBigIconResourceId(iconName));
		} else if (type == WaypointHelper.TARGETS) {
			UiUtilities iconsCache = app.getUIUtilities();
			if (((TargetPoint) point).start) {
				if (app.getTargetPointsHelper().getPointToStart() == null) {
					return iconsCache.getIcon(R.drawable.ic_action_location_color, 0);
				} else {
					return iconsCache.getIcon(R.drawable.list_startpoint, 0);
				}
			} else if (((TargetPoint) point).intermediate) {
				return iconsCache.getIcon(R.drawable.list_intermediate, 0);
			} else {
				return iconsCache.getIcon(R.drawable.list_destination, 0);
			}

		} else if (type == WaypointHelper.FAVORITES) {
			int color = ColorUtilities.getColor(app, R.color.color_favorite);
			return PointImageUtils.getFromPoint(context,
					app.getFavoritesHelper().getColorWithCategory((FavouritePoint) point, color), false, (FavouritePoint) point);
		} else if (type == WaypointHelper.WAYPOINTS) {
			if (point instanceof WptLocationPoint) {
				return PointImageUtils.getFromPoint(context, point.getColor(), false, ((WptLocationPoint) point).getPt());
			} else {
				return null;
			}
		} else if (type == WaypointHelper.ALARMS) {
			//assign alarm list icons manually for now
			String typeString = ((AlarmInfo) point).getType().toString();
			DrivingRegion region = app.getSettings().DRIVING_REGION.get();
			if (typeString.equals("SPEED_CAMERA")) {
				return AppCompatResources.getDrawable(context, R.drawable.mx_highway_speed_camera);
			} else if (typeString.equals("BORDER_CONTROL")) {
				return AppCompatResources.getDrawable(context, R.drawable.mx_barrier_border_control);
			} else if (typeString.equals("RAILWAY")) {
				if (region.isAmericanTypeSigns()) {
					return AppCompatResources.getDrawable(context, R.drawable.list_warnings_railways_us);
				} else {
					return AppCompatResources.getDrawable(context, R.drawable.list_warnings_railways);
				}
			} else if (typeString.equals("TRAFFIC_CALMING")) {
				if (region.isAmericanTypeSigns()) {
					return AppCompatResources.getDrawable(context, R.drawable.list_warnings_traffic_calming_us);
				} else {
					return AppCompatResources.getDrawable(context, R.drawable.list_warnings_traffic_calming);
				}
			} else if (typeString.equals("TOLL_BOOTH")) {
				return AppCompatResources.getDrawable(context, R.drawable.mx_toll_booth);
			} else if (typeString.equals("STOP")) {
				return AppCompatResources.getDrawable(context, R.drawable.list_stop);
			} else if (typeString.equals("PEDESTRIAN")) {
				if (region.isAmericanTypeSigns()) {
					return AppCompatResources.getDrawable(context, R.drawable.list_warnings_pedestrian_us);
				} else {
					return AppCompatResources.getDrawable(context, R.drawable.list_warnings_pedestrian);
				}
			} else if (typeString.equals("TUNNEL")) {
				if (region.isAmericanTypeSigns()) {
					return AppCompatResources.getDrawable(context, R.drawable.list_warnings_tunnel_us);
				} else {
					return AppCompatResources.getDrawable(context, R.drawable.list_warnings_tunnel);
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public int hashCode() {
		return ((point == null) ? 0 : point.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LocationPointWrapper other = (LocationPointWrapper) obj;
		if (point == null) {
			return other.point == null;
		} else return point.equals(other.point);
	}
}