package net.osmand.plus.mapcontextmenu.builders;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.track.fragments.ReadPointDescriptionFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.POIMapLayer;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WptPtMenuBuilder extends MenuBuilder {

	private final WptPt wpt;
	private final Map<String, String> amenityExtensions = new HashMap<>();
	private Amenity amenity;

	public WptPtMenuBuilder(@NonNull MapActivity mapActivity, @NonNull WptPt wpt) {
		super(mapActivity);
		this.wpt = wpt;
		setShowNearestWiki(true);
		acquireAmenityExtensions();
	}

	private void acquireAmenityExtensions() {
		AmenityExtensionsHelper helper = new AmenityExtensionsHelper(app);

		String amenityOriginName = wpt.getAmenityOriginName();
		if (amenityOriginName != null) {
			amenity = helper.findAmenity(amenityOriginName, wpt.getLatitude(), wpt.getLongitude());
		}

		amenityExtensions.putAll(helper.getUpdatedAmenityExtensions(wpt.getExtensionsToRead(),
				wpt.getAmenityOriginName(), wpt.getLatitude(), wpt.getLongitude()));
	}

	@Override
	protected void buildNearestRow(View view, List<Amenity> nearestAmenities, int iconId, String text, String amenityKey) {
		if (amenity == null || !(amenity instanceof Amenity)) {
			super.buildNearestRow(view, nearestAmenities, iconId, text, amenityKey);
		}
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
		if (Algorithms.isEmpty(wpt.desc)) {
			return;
		}

		String textPrefix = app.getString(R.string.shared_string_description);
		View.OnClickListener clickListener = v -> POIMapLayer.showPlainDescriptionDialog(view.getContext(), app, wpt.desc, textPrefix);

		buildRow(view, null, null, textPrefix, wpt.desc, 0,
				null, false, null, true, 10,
				false, false, false, clickListener, matchWidthDivider);
	}

	@Override
	protected void showDescriptionDialog(@NonNull Context ctx, @NonNull String description, @NonNull String title) {
		ReadPointDescriptionFragment.showInstance(mapActivity, description);
	}

	@Override
	public void buildInternal(View view) {
		buildDateRow(view, wpt.time);
		if (wpt.speed > 0) {
			buildRow(view, R.drawable.ic_action_speed,
					null, OsmAndFormatter.getFormattedSpeed((float) wpt.speed, app), 0, false, null, false, 0, false, null, false);
		}
		if (!Double.isNaN(wpt.ele)) {
			buildRow(view, R.drawable.ic_action_altitude,
					null, OsmAndFormatter.getFormattedDistance((float) wpt.ele, app), 0, false, null, false, 0, false, null, false);
		}
		if (!Double.isNaN(wpt.hdop)) {
			buildRow(view, R.drawable.ic_action_gps_info,
					null, Algorithms.capitalizeFirstLetterAndLowercase(app.getString(R.string.plugin_distance_point_hdop)) + ": " + (int) wpt.hdop, 0,
					false, null, false, 0, false, null, false);
		}
		prepareDescription(wpt, view);
		buildCommentRow(view, wpt.comment);

		if (!Algorithms.isEmpty(amenityExtensions)) {
			AmenityUIHelper helper = new AmenityUIHelper(mapActivity, getPreferredMapAppLang(), amenityExtensions);
			helper.setLight(light);
			helper.setLatLon(getLatLon());
			helper.setCollapseExpandListener(getCollapseExpandListener());
			helper.buildInternal(view);
		}

		buildPlainMenuItems(view);
	}

	protected void prepareDescription(WptPt wpt, View view) {

	}

	private void buildWaypointsView(View view) {
		GpxSelectionHelper gpxSelectionHelper = app.getSelectedGpxHelper();
		SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedGPXFile(wpt);
		if (selectedGpxFile != null) {
			List<WptPt> points = selectedGpxFile.getGpxFile().getPoints();
			GPXFile gpx = selectedGpxFile.getGpxFile();
			if (points.size() > 0) {
				String title = view.getContext().getString(R.string.context_menu_points_of_group);
				File file = new File(gpx.path);
				String gpxName = file.getName().replace(IndexConstants.GPX_FILE_EXT, "").replace("/", " ").replace("_", " ");
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

	private CollapsableView getCollapsableWaypointsView(Context context, boolean collapsed, @NonNull GPXFile gpxFile, WptPt selectedPoint) {
		LinearLayout view = buildCollapsableContentView(context, collapsed, true);

		List<WptPt> points = gpxFile.getPoints();
		String selectedCategory = selectedPoint != null && selectedPoint.category != null ? selectedPoint.category : "";
		int showCount = 0;
		for (WptPt point : points) {
			String currentCategory = point != null ? point.category : null;
			if (selectedCategory.equals(currentCategory)) {
				showCount++;
				boolean selected = point.equals(selectedPoint);
				TextViewEx button = buildButtonInCollapsableView(context, selected, false);
				button.setText(point.name);

				if (!selected) {
					button.setOnClickListener(v -> {
						LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
						PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_WPT, point.name);
						mapActivity.getContextMenu().setCenterMarker(true);
						mapActivity.getContextMenu().show(latLon, pointDescription, point);
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
			button.setOnClickListener(v -> TrackMenuFragment.openTrack(mapActivity, new File(gpxFile.path), null));
			view.addView(button);
		}

		return new CollapsableView(view, this, collapsed);
	}
}
