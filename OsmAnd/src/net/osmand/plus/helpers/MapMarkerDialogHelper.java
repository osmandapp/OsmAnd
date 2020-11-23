package net.osmand.plus.helpers;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.DirectionDrawable;


public class MapMarkerDialogHelper {

	public static void updateMapMarkerInfo(final Context ctx,
										   View localView,
										   LatLon loc,
										   Float heading,
										   boolean useCenter,
										   boolean nightMode,
										   int screenOrientation,
										   final MapMarker marker) {
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		TextView textShadow = (TextView) localView.findViewById(R.id.waypoint_text_shadow);
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		ImageView arrow = (ImageView) localView.findViewById(R.id.direction);
		ImageView waypointIcon = (ImageView) localView.findViewById(R.id.waypoint_icon);
		TextView waypointDeviation = (TextView) localView.findViewById(R.id.waypoint_deviation);
		TextView descText = (TextView) localView.findViewById(R.id.waypoint_desc_text);
		final CheckBox checkBox = (CheckBox) localView.findViewById(R.id.checkbox);
		TextView dateGroupText = (TextView) localView.findViewById(R.id.date_group_text);

		if (text == null || textDist == null || arrow == null || waypointIcon == null
				|| waypointDeviation == null || descText == null) {
			return;
		}

		float[] mes = new float[2];
		if (loc != null && marker.point != null) {
			Location.distanceBetween(marker.getLatitude(), marker.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
		}
		boolean newImage = false;
		int arrowResId = R.drawable.ic_direction_arrow;
		DirectionDrawable dd;
		if (!(arrow.getDrawable() instanceof DirectionDrawable)) {
			newImage = true;
			dd = new DirectionDrawable((OsmandApplication) ctx.getApplicationContext(), arrow.getWidth(), arrow.getHeight());
		} else {
			dd = (DirectionDrawable) arrow.getDrawable();
		}
		if (!marker.history) {
			dd.setImage(arrowResId, useCenter ? R.color.color_distance : R.color.color_myloc_distance);
		} else {
			dd.setImage(arrowResId, nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light);
		}
		if (loc == null || heading == null || marker.point == null) {
			dd.setAngle(0);
		} else {
			dd.setAngle(mes[1] - heading + 180 + screenOrientation);
		}
		if (newImage) {
			arrow.setImageDrawable(dd);
		}
		arrow.setVisibility(View.VISIBLE);
		arrow.invalidate();

		final OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();

		if (!marker.history) {
			waypointIcon.setImageDrawable(getMapMarkerIcon(app, marker.colorIndex));
			AndroidUtils.setTextPrimaryColor(ctx, text, nightMode);
			textDist.setTextColor(ctx.getResources()
					.getColor(useCenter ? R.color.color_distance : R.color.color_myloc_distance));
		} else {
			waypointIcon.setImageDrawable(app.getUIUtilities()
					.getIcon(R.drawable.ic_action_flag, !nightMode));
			AndroidUtils.setTextSecondaryColor(ctx, text, nightMode);
			AndroidUtils.setTextSecondaryColor(ctx, textDist, nightMode);
		}

		int dist = (int) mes[0];
		textDist.setText(OsmAndFormatter.getFormattedDistance(dist, app));

		waypointDeviation.setVisibility(View.GONE);

		String descr = marker.getName(app);
		if (textShadow != null) {
			textShadow.setText(descr);
		}
		text.setText(descr);

		descText.setVisibility(View.GONE);

		String desc = OsmAndFormatter.getFormattedDate(app, marker.creationDate);
		String markerGroupName = marker.groupName;
		if (markerGroupName != null) {
			if (markerGroupName.isEmpty()) {
				markerGroupName = app.getString(R.string.shared_string_favorites);
			}
			desc += " • " + markerGroupName;
		}
		dateGroupText.setVisibility(View.VISIBLE);
		dateGroupText.setText(desc);

		checkBox.setVisibility(View.GONE);
		checkBox.setOnClickListener(null);
	}

	public static Drawable getMapMarkerIcon(OsmandApplication app, int colorIndex) {
		return app.getUIUtilities().getIcon(R.drawable.ic_action_flag, MapMarker.getColorId(colorIndex));
	}
}
