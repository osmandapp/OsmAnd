package net.osmand.plus.mapcontextmenu.controllers;

import static android.graphics.Typeface.DEFAULT;

import android.graphics.drawable.Drawable;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.FavouritePointMenuBuilder;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditor;
import net.osmand.plus.mapcontextmenu.editors.FavoritePointEditorFragment;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;
import net.osmand.view.GravityDrawable;

import java.util.List;

public class FavouritePointMenuController extends MenuController {

	private FavouritePoint fav;
	private MapMarker mapMarker;

	private TransportStopController transportStopController;

	public FavouritePointMenuController(@NonNull MapActivity activity,
			@NonNull PointDescription description,
			@NonNull FavouritePoint point, @Nullable Amenity amenity) {
		super(new FavouritePointMenuBuilder(activity, point, amenity), description, activity);
		this.fav = point;

		OsmandApplication app = activity.getApp();
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();

		mapMarker = markersHelper.getMapMarker(point);
		if (mapMarker == null) {
			mapMarker = markersHelper.getMapMarker(new LatLon(point.getLatitude(), point.getLongitude()));
		}
		if (mapMarker != null && mapMarker.history && !app.getSettings().KEEP_PASSED_MARKERS_ON_MAP.get()) {
			mapMarker = null;
		}
		if (mapMarker != null) {
			MapMarkerMenuController markerMenuController =
					new MapMarkerMenuController(activity, mapMarker.getPointDescription(activity), mapMarker);
			leftTitleButtonController = markerMenuController.getLeftTitleButtonController();
			rightTitleButtonController = markerMenuController.getRightTitleButtonController();
		}
		if (getObject() instanceof TransportStop) {
			TransportStop stop = (TransportStop) getObject();
			transportStopController = new TransportStopController(activity, description, stop);
			transportStopController.processRoutes();
		}
		amenity = getBuilder().getAmenity();
		if (amenity != null) {
			openingHoursInfo = OpeningHoursParser.getInfo(amenity.getOpeningHours());
		}
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof FavouritePoint) {
			this.fav = (FavouritePoint) object;
		}
	}

	@Override
	protected Object getObject() {
		return fav;
	}

	@Override
	public List<TransportStopRoute> getTransportStopRoutes() {
		if (transportStopController != null) {
			return transportStopController.getTransportStopRoutes();
		}
		return null;
	}

	@Override
	protected List<TransportStopRoute> getSubTransportStopRoutes(boolean nearby) {
		if (transportStopController != null) {
			return transportStopController.getSubTransportStopRoutes(nearby);
		}
		return null;
	}

	@Override
	public boolean handleSingleTapOnMap() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(FavoritePointEditor.TAG);
			if (fragment instanceof FavoritePointEditorFragment) {
				((FavoritePointEditorFragment) fragment).dismiss();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public boolean needTypeStr() {
		return true;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getRightIcon() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return PointImageUtils.getFromPoint(mapActivity.getApp(),
					mapActivity.getApp().getFavoritesHelper().getColorWithCategory(fav,
							ContextCompat.getColor(mapActivity, R.color.color_favorite)), false, fav);
		} else {
			return null;
		}
	}

	@Override
	public boolean isWaypointButtonEnabled() {
		return mapMarker == null;
	}

	@NonNull
	@Override
	public String getNameStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return fav.getDisplayName(mapActivity);
		} else {
			return super.getNameStr();
		}
	}

	@NonNull
	@Override
	public CharSequence getSubtypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !Algorithms.isEmpty(fav.getAddress())) {
			SpannableString addressSpannable = new SpannableString(fav.getAddress());
			addressSpannable.setSpan(new CustomTypefaceSpan(DEFAULT), 0, addressSpannable.length(), 0);

			return addressSpannable;
		} else {
			return "";
		}
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getApp();
			FavouritesHelper helper = app.getFavoritesHelper();
			String group = fav.getCategory();
			Drawable line2icon = helper.getGroup(group) != null ? helper.getColoredIconForGroup(group) : null;
			if (line2icon != null) {
				GravityDrawable gravityIcon = new GravityDrawable(line2icon);
				gravityIcon.setBoundsFrom(line2icon);
				return gravityIcon;
			} else {
				int colorId = isLight() ? R.color.icon_color_default_light : R.color.icon_color_secondary_dark;
				return getIcon(R.drawable.ic_action_group_name_16, colorId);
			}
		}
		return null;
	}

	@Override
	public int getFavActionIconId() {
		return R.drawable.ic_action_edit_dark;
	}

	@Override
	public int getFavActionStringId() {
		return R.string.shared_string_edit;
	}

	@Override
	public boolean isFavButtonEnabled() {
		return !fav.isSpecialPoint();
	}

	@NonNull
	@Override
	public String getTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return fav.getCategory().length() == 0 ?
					mapActivity.getString(R.string.shared_string_favorites) : fav.getCategoryDisplayName(mapActivity);
		} else {
			return "";
		}
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		Amenity amenity = getBuilder().getAmenity();
		if (amenity != null) {
			AmenityMenuController.addTypeMenuItem(amenity, builder);
		}
	}
}
