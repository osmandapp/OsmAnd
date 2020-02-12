package net.osmand.plus.mapcontextmenu.builders;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class WptPtMenuBuilder extends MenuBuilder {

	private final WptPt wpt;

	public WptPtMenuBuilder(@NonNull MapActivity mapActivity, final @NonNull WptPt wpt) {
		super(mapActivity);
		this.wpt = wpt;
		setShowNearestWiki(true);
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	protected void buildTopInternal(View view) {
		super.buildTopInternal(view);
		buildWaypointsView(view);
	}

	@Override
	protected void buildDescription(View view) {
		if (!Algorithms.isEmpty(wpt.desc)) {
			buildDescriptionRow(view, app.getString(R.string.shared_string_description), wpt.desc, 0, 10, true);
		}
	}

	@Override
	public void buildInternal(View view) {
		if (wpt.time > 0) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(view.getContext());
			DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(view.getContext());
			Date date = new Date(wpt.time);
			buildRow(view, R.drawable.ic_action_data,
					null, dateFormat.format(date) + " — " + timeFormat.format(date), 0, false, null, false, 0, false, null, false);
		}
		if (wpt.speed > 0) {
			buildRow(view, R.drawable.ic_action_speed,
					null, OsmAndFormatter.getFormattedSpeed((float)wpt.speed, app), 0, false, null, false, 0, false, null, false);
		}
		if (!Double.isNaN(wpt.ele)) {
			buildRow(view, R.drawable.ic_action_altitude,
					null, OsmAndFormatter.getFormattedDistance((float) wpt.ele, app), 0, false, null, false, 0, false, null, false);
		}
		if (!Double.isNaN(wpt.hdop)) {
			buildRow(view, R.drawable.ic_action_gps_info,
					null, Algorithms.capitalizeFirstLetterAndLowercase(app.getString(R.string.plugin_distance_point_hdop)) + ": " + (int)wpt.hdop, 0,
					false, null, false, 0, false, null, false);
		}
		
		if (!Algorithms.isEmpty(wpt.desc)) {
			prepareDescription(wpt, view);
		}
		
		if (!Algorithms.isEmpty(wpt.comment)) {
			final View rowc = buildRow(view, R.drawable.ic_action_note_dark, null, wpt.comment, 0,
					false, null, true, 10, false, null, false);
			rowc.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					POIMapLayer.showDescriptionDialog(rowc.getContext(), app, wpt.comment,
							rowc.getResources().getString(R.string.poi_dialog_comment));
				}
			});
		}

		buildPlainMenuItems(view);
	}
	
	protected void prepareDescription(final WptPt wpt, View view) {

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
				buildRow(view, app.getUIUtilities().getPaintedIcon(R.drawable.ic_type_waypoints_group, color), null, title, 0, gpxName,
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

	private CollapsableView getCollapsableWaypointsView(final Context context, boolean collapsed, @NonNull final GPXUtilities.GPXFile gpxFile, WptPt selectedPoint) {
		LinearLayout view = (LinearLayout) buildCollapsableContentView(context, collapsed, true);

		List<WptPt> points = gpxFile.getPoints();
		String selectedCategory = selectedPoint != null && selectedPoint.category != null ? selectedPoint.category : "";
		int showCount = 0;
		for (final WptPt point : points) {
			String currentCategory = point != null ? point.category : null;
			if (selectedCategory.equals(currentCategory)) {
				showCount++;
				boolean selected = selectedPoint != null && selectedPoint.equals(point);
				TextViewEx button = buildButtonInCollapsableView(context, selected, false);
				button.setText(point.name);

				if (!selected) {
					button.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
							PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_WPT, point.name);
							mapActivity.getContextMenu().setCenterMarker(true);
							mapActivity.getContextMenu().show(latLon, pointDescription, point);
						}
					});
				}
				view.addView(button);
			}
			if (showCount >= 10) {
				break;
			}
		}

		if (points.size() > 10) {
			TextViewEx button = buildButtonInCollapsableView(context, false, true);
			button.setText(context.getString(R.string.shared_string_show_all));
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					OsmAndAppCustomization appCustomization = app.getAppCustomization();
					final Intent intent = new Intent(context, appCustomization.getTrackActivity());
					intent.putExtra(TrackActivity.TRACK_FILE_NAME, gpxFile.path);
					intent.putExtra(TrackActivity.OPEN_POINTS_TAB, true);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					context.startActivity(intent);
				}
			});
			view.addView(button);
		}

		return new CollapsableView(view, this, collapsed);
	}
}
