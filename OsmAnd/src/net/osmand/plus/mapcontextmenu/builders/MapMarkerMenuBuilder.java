package net.osmand.plus.mapcontextmenu.builders;

import android.view.View;

import net.osmand.data.Amenity;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.Date;

public class MapMarkerMenuBuilder extends SyncedItemMenuBuilder {

	private GPXUtilities.WptPt wptPt;

	public MapMarkerMenuBuilder(MapActivity mapActivity, final MapMarkersHelper.MapMarker marker) {
		super(mapActivity);
		this.favouritePoint = marker.favouritePoint;
		this.wptPt = marker.wptPt;
		if (favouritePoint != null) {
			acquireOriginObject();
		}
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	protected void buildNearestWikiRow(View view) {
		if (originObject == null || !(originObject instanceof Amenity)) {
			super.buildNearestWikiRow(view);
		}
	}

	@Override
	protected void buildInternal(View view) {
		if (originObject != null && originObject instanceof Amenity) {
			AmenityMenuBuilder builder = new AmenityMenuBuilder(mapActivity, (Amenity) originObject);
			builder.setLatLon(getLatLon());
			builder.setLight(light);
			builder.buildInternal(view);
		}
		if (wptPt != null) {
			if (wptPt.time > 0) {
				DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(view.getContext());
				DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(view.getContext());
				Date date = new Date(wptPt.time);
				buildRow(view, R.drawable.ic_action_data,
						dateFormat.format(date) + " — " + timeFormat.format(date), 0, false, null, false, 0, false, null);
			}
			if (wptPt.speed > 0) {
				buildRow(view, R.drawable.ic_action_speed,
						OsmAndFormatter.getFormattedSpeed((float) wptPt.speed, app), 0, false, null, false, 0, false, null);
			}
			if (!Double.isNaN(wptPt.ele)) {
				buildRow(view, R.drawable.ic_action_altitude,
						OsmAndFormatter.getFormattedDistance((float) wptPt.ele, app), 0, false, null, false, 0, false, null);
			}
			if (!Double.isNaN(wptPt.hdop)) {
				buildRow(view, R.drawable.ic_action_gps_info,
						Algorithms.capitalizeFirstLetterAndLowercase(app.getString(R.string.plugin_distance_point_hdop)) + ": " + (int) wptPt.hdop, 0,
						false, null, false, 0, false, null);
			}
			if (!Algorithms.isEmpty(wptPt.desc)) {
				final View row = buildRow(view, R.drawable.ic_action_note_dark, wptPt.desc, 0, false, null, true, 10, false, null);
				row.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						POIMapLayer.showDescriptionDialog(row.getContext(), app, wptPt.desc,
								row.getResources().getString(R.string.description));
					}
				});
			}
			if (!Algorithms.isEmpty(wptPt.comment)) {
				final View rowc = buildRow(view, R.drawable.ic_action_note_dark, wptPt.comment, 0,
						false, null, true, 10, false, null);
				rowc.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						POIMapLayer.showDescriptionDialog(rowc.getContext(), app, wptPt.comment,
								rowc.getResources().getString(R.string.poi_dialog_comment));
					}
				});
			}
		}
		buildPlainMenuItems(view);
	}
}
