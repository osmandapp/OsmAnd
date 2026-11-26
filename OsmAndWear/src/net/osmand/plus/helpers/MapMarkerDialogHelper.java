package net.osmand.plus.helpers;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.views.DirectionDrawable;


public class MapMarkerDialogHelper {

	public static void updateMapMarkerInfo(Context ctx,
	                                       View localView,
	                                       LatLon loc,
	                                       Float heading,
	                                       boolean useCenter,
	                                       boolean nightMode,
	                                       int screenOrientation,
	                                       MapMarker marker) {
		TextView text = localView.findViewById(R.id.waypoint_text);
		TextView textShadow = localView.findViewById(R.id.waypoint_text_shadow);
		TextView textDist = localView.findViewById(R.id.waypoint_dist);
		ImageView arrow = localView.findViewById(R.id.direction);
		ImageView waypointIcon = localView.findViewById(R.id.waypoint_icon);
		TextView waypointDeviation = localView.findViewById(R.id.waypoint_deviation);
		TextView descText = localView.findViewById(R.id.waypoint_desc_text);
		CheckBox checkBox = localView.findViewById(R.id.checkbox);
		TextView dateGroupText = localView.findViewById(R.id.date_group_text);

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
			dd.setImage(arrowResId, ColorUtilities.getSecondaryTextColorId(nightMode));
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

		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();

		if (!marker.history) {
			waypointIcon.setImageDrawable(getMapMarkerIcon(app, marker.colorIndex));
			AndroidUtils.setTextPrimaryColor(ctx, text, nightMode);
			textDist.setTextColor(ctx.getColor(useCenter ? R.color.color_distance : R.color.color_myloc_distance));
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
