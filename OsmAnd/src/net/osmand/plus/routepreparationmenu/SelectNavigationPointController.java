package net.osmand.plus.routepreparationmenu;

import android.content.Context;
import android.graphics.PointF;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.selectlocation.ILocationSelectionHandler;
import net.osmand.plus.dialogs.selectlocation.SelectLocationController;
import net.osmand.plus.dialogs.selectlocation.extractor.CenterMapPixelPointExtractor;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.routepreparationmenu.data.PointType;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapSelectionResult;
import net.osmand.plus.views.layers.SelectedMapObject;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import java.util.Objects;

public class SelectNavigationPointController {

	private final OsmandApplication app;

	public SelectNavigationPointController(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void selectOnMap(@NonNull MapActivity mapActivity,
	                        @NonNull PointType pointType, @Nullable String dialogId) {
		CenterMapPixelPointExtractor extractor = new CenterMapPixelPointExtractor();
		SelectLocationController.showDialog(mapActivity, extractor, new ILocationSelectionHandler<>() {
			@NonNull
			@Override
			public String getDialogTitle(@NonNull MapActivity activity) {
				return getTitle(mapActivity, pointType);
			}

			@Nullable
			@Override
			public Object getCenterPointIcon(@NonNull MapActivity activity) {
				return getPointIcon(activity, pointType);
			}

			@Nullable
			@Override
			public String getCenterPointLabel(@NonNull MapActivity mapActivity) {
				if (pointType == PointType.INTERMEDIATE) {
					TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
					return String.valueOf(targetPointsHelper.getIntermediatePoints().size() + 1);
				}
				return null;
			}

			@Override
			public void onLocationSelected(@NonNull MapActivity activity, @NonNull PointF location) {
				handleSelectedMapLocation(activity, pointType, location);
			}

			@Override
			public void onScreenClosed(@NonNull MapActivity activity, boolean selected) {
				if (Objects.equals(WaypointsFragment.TAG, dialogId)) {
					WaypointsFragment.showInstance(activity, true);
				} else {
					activity.getMapRouteInfoMenu().restore();
				}
			}
		});
	}

	public void selectAddress(@Nullable String name, @NonNull LatLon latLon, PointType pointType) {
		PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, name);
		choosePointTypeAction(latLon, pointType, pd, name);
	}

	public void selectMapMarker(@NonNull MapActivity mapActivity,
	                            @NonNull MapMarker marker, @NonNull PointType pointType) {
		LatLon latLon = new LatLon(marker.getLatitude(), marker.getLongitude());
		PointDescription pd = marker.getPointDescription(mapActivity);
		choosePointTypeAction(latLon, pointType, pd, null);
	}

	private void handleSelectedMapLocation(@NonNull MapActivity activity,
	                                       @NonNull PointType pointType,
	                                       @NonNull PointF point) {
		OsmandMapTileView mapView = activity.getMapView();
		RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox();

		Pair<LatLon, PointDescription> pair = fetchMapObject(mapView, tileBox, point);
		LatLon selectedPoint;
		PointDescription name = null;
		if (pair != null) {
			selectedPoint = pair.first;
			name = pair.second;
		} else {
			MapRendererView mapRenderer = mapView.getMapRenderer();
			selectedPoint = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tileBox, point);
		}
		choosePointTypeAction(selectedPoint, pointType, name, null);
	}

	@Nullable
	private Pair<LatLon, PointDescription> fetchMapObject(@NonNull OsmandMapTileView mapView,
	                                                      @NonNull RotatedTileBox tileBox,
	                                                      @NonNull PointF point) {
		MapSelectionResult result = new MapSelectionResult(mapView.getApplication(), tileBox, point);
		for (OsmandMapLayer layer : mapView.getLayers()) {
			if (layer instanceof IContextMenuProvider provider) {
				provider.collectObjectsFromPoint(result, true, true);
				for (SelectedMapObject selectedMapObject : result.getAllObjects()) {
					Object object = selectedMapObject.object();
					LatLon latLon = provider.getObjectLocation(object);
					PointDescription name = null;
					if (object instanceof FavouritePoint) {
						name = ((FavouritePoint) object).getPointDescription(mapView.getApplication());
					}
					return new Pair<>(latLon, name);
				}
			}
		}
		return null;
	}

	@NonNull
	private String getTitle(@NonNull Context context, @NonNull PointType pointType) {
		switch (pointType) {
			case START -> {
				return context.getString(R.string.set_start_point);
			}
			case INTERMEDIATE -> {
				return context.getString(R.string.set_intermediate_point);
			}
			case TARGET -> {
				return context.getString(R.string.add_destination_point);
			}
			case HOME -> {
				return context.getString(R.string.set_home);
			}
			case WORK -> {
				return context.getString(R.string.set_work);
			}
			case PARKING -> {
				return context.getString(R.string.set_parking_position);
			}
		}
		return "";
	}

	@Nullable
	private Object getPointIcon(@NonNull MapActivity mapActivity, @NonNull PointType pointType) {
		MapLayers layers = mapActivity.getMapLayers();
		switch (pointType) {
			case START -> {
				return layers.getNavigationLayer().getStartPointIcon();
			}
			case INTERMEDIATE -> {
				return layers.getNavigationLayer().getIntermediatePointIcon();
			}
			case TARGET -> {
				return layers.getNavigationLayer().getPointToNavigateIcon();
			}
			case HOME -> {
				return layers.getFavouritesLayer().createHomeIcon();
			}
			case WORK -> {
				return layers.getFavouritesLayer().createWorkIcon();
			}
			case PARKING -> {
				return layers.getFavouritesLayer().createParkingIcon();
			}
		}
		return null;
	}

	private void choosePointTypeAction(@NonNull LatLon latLon, @NonNull PointType pointType,
	                                   @Nullable PointDescription pd, @Nullable String address) {
		FavouritesHelper favorites = app.getFavoritesHelper();
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		switch (pointType) {
			case START:
				targetPointsHelper.setStartPoint(latLon, true, pd);
				break;
			case TARGET:
				targetPointsHelper.navigateToPoint(latLon, true, -1, pd);
				break;
			case INTERMEDIATE:
				int index = targetPointsHelper.getIntermediatePoints().size();
				targetPointsHelper.navigateToPoint(latLon, true, index, pd);
				break;
			case HOME:
				favorites.setSpecialPoint(latLon, SpecialPointType.HOME, address);
				break;
			case WORK:
				favorites.setSpecialPoint(latLon, SpecialPointType.WORK, address);
				break;
		}
	}

	public boolean isSelectFromMap() {
		return SelectLocationController.getExistedInstance(app) != null;
	}
}
