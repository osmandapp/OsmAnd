package net.osmand.plus.avoidroads;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class AvoidRoadsDialog {

	public static void showDialog(@NonNull MapActivity activity, @Nullable ApplicationMode mode) {
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			OsmandApplication app = activity.getApp();
			AvoidRoadsHelper avoidRoadsHelper = app.getAvoidSpecificRoads();
			boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);

			AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
			builder.setTitle(R.string.impassable_road);

			List<AvoidRoadInfo> roadInfos = new ArrayList<>(avoidRoadsHelper.getImpassableRoads());
			if (roadInfos.isEmpty()) {
				builder.setMessage(R.string.avoid_roads_msg);
			} else {
				ArrayAdapter<AvoidRoadInfo> adapter = createAdapter(activity, roadInfos, nightMode);
				builder.setAdapter(adapter, (dialog, which) -> {
					AvoidRoadInfo point = adapter.getItem(which);
					if (point != null) {
						showOnMap(activity, point.getLatitude(), point.getLongitude(), point.getName(app));
					}
					dialog.dismiss();
				});
			}
			builder.setPositiveButton(R.string.shared_string_select_on_map, (dialogInterface, i) -> avoidRoadsHelper.selectFromMap(activity, mode));
			builder.setNegativeButton(R.string.shared_string_close, null);
			builder.show();
		}
	}

	@NonNull
	private static ArrayAdapter<AvoidRoadInfo> createAdapter(@NonNull MapActivity activity, @NonNull List<AvoidRoadInfo> roadInfos, boolean nightMode) {
		OsmandApplication app = activity.getApp();
		AvoidRoadsHelper avoidRoadsHelper = app.getAvoidSpecificRoads();

		LatLon mapLocation = activity.getMapLocation();
		LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
		Context themedContext = UiUtilities.getThemedContext(activity, nightMode);

		return new ArrayAdapter<AvoidRoadInfo>(themedContext, R.layout.waypoint_reached, R.id.title, roadInfos) {
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {
				View view = convertView;
				if (view == null || view.findViewById(R.id.info_close) == null) {
					view = inflater.inflate(R.layout.waypoint_reached, parent, false);
				}
				AvoidRoadInfo item = getItem(position);
				if (item != null) {
					ImageView icon = view.findViewById(R.id.waypoint_icon);
					icon.setImageDrawable(getIcon(R.drawable.ic_action_road_works_dark));

					TextView text = view.findViewById(R.id.waypoint_text);
					text.setText(item.getName(app));

					TextView distance = view.findViewById(R.id.waypoint_dist);
					distance.setText(getDist(mapLocation, item.getLatLon()));

					ImageButton removeButton = view.findViewById(R.id.info_close);
					removeButton.setImageDrawable(getIcon(R.drawable.ic_action_remove_dark));
					removeButton.setOnClickListener(v -> {
						remove(item);
						avoidRoadsHelper.removeImpassableRoad(item);
						notifyDataSetChanged();
						avoidRoadsHelper.recalculateRoute(item.getAppModeKey());
					});
					AndroidUiHelper.updateVisibility(removeButton, true);
					AndroidUiHelper.updateVisibility(view.findViewById(R.id.all_points), false);
				}
				return view;
			}

			private Drawable getIcon(@DrawableRes int iconId) {
				return app.getUIUtilities().getThemedIcon(iconId);
			}

			@NonNull
			private String getDist(@NonNull LatLon loc, @Nullable LatLon point) {
				double dist = point == null ? 0 : MapUtils.getDistance(loc, point);
				return OsmAndFormatter.getFormattedDistance((float) dist, app);
			}
		};
	}

	private static void showOnMap(@NonNull MapActivity activity, double lat, double lon, String name) {
		int zoom = Math.max(activity.getMapView().getZoom(), 15);
		PointDescription pd = new PointDescription("", name);
		activity.getSettings().setMapLocationToShow(lat, lon, zoom, pd, false, null);
		MapActivity.launchMapActivityMoveToTop(activity);
	}
}