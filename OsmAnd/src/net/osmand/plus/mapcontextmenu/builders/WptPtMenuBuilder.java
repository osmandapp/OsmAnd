package net.osmand.plus.mapcontextmenu.builders;

import android.view.View;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.Date;

public class WptPtMenuBuilder extends MenuBuilder {

	private final WptPt wpt;

	public WptPtMenuBuilder(OsmandApplication app, final WptPt wpt) {
		super(app);
		this.wpt = wpt;
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	public void buildInternal(View view) {
		if (wpt.time > 0) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(view.getContext());
			DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(view.getContext());
			Date date = new Date(wpt.time);
			buildRow(view, R.drawable.ic_action_data,
					dateFormat.format(date) + " — " + timeFormat.format(date), 0, false, 0, false);
		}
		if (wpt.speed > 0) {
			buildRow(view, R.drawable.ic_action_speed,
					OsmAndFormatter.getFormattedSpeed((float)wpt.speed, app), 0, false, 0, false);
		}
		if (!Double.isNaN(wpt.ele)) {
			buildRow(view, R.drawable.ic_action_altitude,
					OsmAndFormatter.getFormattedDistance((float) wpt.ele, app), 0, false, 0, false);
		}
		if (!Double.isNaN(wpt.hdop)) {
			buildRow(view, R.drawable.ic_action_gps_info,
					Algorithms.capitalizeFirstLetterAndLowercase(app.getString(R.string.plugin_distance_point_hdop)) + ": "
							+ OsmAndFormatter.getFormattedDistance((float)wpt.hdop, app), 0, false, 0, false);
		}
		if (!Algorithms.isEmpty(wpt.desc)) {
			final View row = buildRow(view, R.drawable.ic_action_note_dark, wpt.desc, 0, true, 10, false);
			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					POIMapLayer.showDescriptionDialog(row.getContext(), app, wpt.desc,
							row.getResources().getString(R.string.description));
				}
			});
		}

		buildPlainMenuItems(view);
	}
}

