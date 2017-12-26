package net.osmand.plus.mapcontextmenu.builders;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;

import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class WptPtMenuBuilder extends MenuBuilder {

	private final WptPt wpt;

	public WptPtMenuBuilder(MapActivity mapActivity, final WptPt wpt) {
		super(mapActivity);
		this.wpt = wpt;
		setShowNearestWiki(true);
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
					dateFormat.format(date) + " — " + timeFormat.format(date), 0, false, null, false, 0, false, null, false);
		}
		if (wpt.speed > 0) {
			buildRow(view, R.drawable.ic_action_speed,
					OsmAndFormatter.getFormattedSpeed((float)wpt.speed, app), 0, false, null, false, 0, false, null, false);
		}
		if (!Double.isNaN(wpt.ele)) {
			buildRow(view, R.drawable.ic_action_altitude,
					OsmAndFormatter.getFormattedDistance((float) wpt.ele, app), 0, false, null, false, 0, false, null, false);
		}
		if (!Double.isNaN(wpt.hdop)) {
			buildRow(view, R.drawable.ic_action_gps_info,
					Algorithms.capitalizeFirstLetterAndLowercase(app.getString(R.string.plugin_distance_point_hdop)) + ": " + (int)wpt.hdop, 0,
					false, null, false, 0, false, null, false);
		}
		if (!Algorithms.isEmpty(wpt.desc)) {
			final View row = buildRow(view, R.drawable.ic_action_note_dark, wpt.desc, 0, false, null, true, 10, false, null, false);
			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					POIMapLayer.showDescriptionDialog(row.getContext(), app, wpt.desc,
							row.getResources().getString(R.string.description));
				}
			});
		}
		if (!Algorithms.isEmpty(wpt.comment)) {
			final View rowc = buildRow(view, R.drawable.ic_action_note_dark, wpt.comment, 0,
					false, null, true, 10, false, null, false);
			rowc.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					POIMapLayer.showDescriptionDialog(rowc.getContext(), app, wpt.comment,
							rowc.getResources().getString(R.string.poi_dialog_comment));
				}
			});
		}

		buildWaypointsView(view);
		buildPlainMenuItems(view);
	}

	private void buildWaypointsView(View view) {
		GpxSelectionHelper gpxSelectionHelper = app.getSelectedGpxHelper();
		SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedGPXFile(wpt);
		if (selectedGpxFile != null) {
			List<WptPt> points = selectedGpxFile.getGpxFile().getPoints();
			GPXUtilities.GPXFile gpx = selectedGpxFile.getGpxFile();
			if (points.size() > 0) {
				String title = view.getContext().getString(R.string.context_menu_points_of_group);
				File file = new File(gpx.path);
				String gpxName = file.getName().replace(".gpx", "").replace("/", " ").replace("_", " ");
				int color = getPointColor(wpt, getFileColor(selectedGpxFile));
				buildRow(view, app.getIconsCache().getPaintedIcon(R.drawable.ic_type_waypoints_group, color), title, 0, gpxName,
						true, getCollapsableWaypointsView(view.getContext(), true, gpx, wpt),
						false, 0, false, null, false);
			}
		}
	}

	private int getFileColor(@NonNull SelectedGpxFile g) {
		return g.getColor() == 0 ? ContextCompat.getColor(app, R.color.gpx_color_point) : g.getColor();
	}

	@ColorInt
	private int getPointColor(WptPt o, @ColorInt int fileColor) {
		boolean visit = isPointVisited(o);
		return visit ? ContextCompat.getColor(app, R.color.color_ok) : o.getColor(fileColor);
	}

	private boolean isPointVisited(WptPt o) {
		boolean visit = false;
		String visited = o.getExtensionsToRead().get("VISITED_KEY");
		if (visited != null && !visited.equals("0")) {
			visit = true;
		}
		return visit;
	}
}
