package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.MARKER_ACTION_ID;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.views.layers.MapMarkersLayer;

public class MarkerAction extends SelectMapLocationAction {

	public static final QuickActionType TYPE = new QuickActionType(MARKER_ACTION_ID,
			"marker.add", MarkerAction.class)
			.nameRes(R.string.map_marker).iconRes(R.drawable.ic_action_flag).nonEditable().
			category(QuickActionType.MY_PLACES).nameActionRes(R.string.shared_string_add);

	public MarkerAction() {
		super(TYPE);
	}

	public MarkerAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon, @Nullable Bundle params) {
		double lat = latLon.getLatitude();
		double lon = latLon.getLongitude();

		PointDescription pd = new PointDescription(lat, lon);

		if (pd.isLocation() && pd.getName().equals(PointDescription.getAddressNotFoundStr(mapActivity))) {
			pd = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
		}

		mapActivity.getMapActions().addMapMarker(lat, lon, pd, null);
	}

	@Override
	@Nullable
	protected Object getLocationIcon(@NonNull MapActivity mapActivity) {
		MapMarkersHelper markersHelper = mapActivity.getApp().getMapMarkersHelper();
		MapMarkersLayer layer = mapActivity.getMapLayers().getMapMarkersLayer();
		int colorIndex = markersHelper.getNextMarkerColorIndex(-1);
		return layer.getMapMarkerShiftedBitmap(colorIndex);
	}

	@NonNull
	@Override
	protected CharSequence getQuickActionDescription(@NonNull Context context) {
		return context.getString(R.string.quick_action_add_marker_descr);
	}
}
