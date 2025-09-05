package net.osmand.plus.mapcontextmenu.builders;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.IndexConstants;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
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
import net.osmand.plus.views.layers.PlaceDetailsObject;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WptPtMenuBuilder extends MenuBuilder {

	private final WptPt wpt;
	private final Map<String, String> amenityExtensions = new HashMap<>();

	public WptPtMenuBuilder(@NonNull MapActivity mapActivity, @NonNull WptPt wpt,
			@Nullable PlaceDetailsObject detailsObject) {
		super(mapActivity);
		this.wpt = wpt;
		if (detailsObject != null) {
			setAmenity(detailsObject.getSyntheticAmenity());
		}
		setShowNearestWiki(true);
		acquireAmenityExtensions();
	}

	private void acquireAmenityExtensions() {
		AmenityExtensionsHelper helper = new AmenityExtensionsHelper(app);
		if (amenity == null) {
			String originName = wpt.getAmenityOriginName();
			if (!Algorithms.isEmpty(originName)) {
				amenity = helper.findAmenity(originName, wpt.getLatitude(), wpt.getLongitude());
			}
		}
		amenityExtensions.putAll(helper.getUpdatedAmenityExtensions(wpt.getExtensionsToRead(), amenity));
	}

	@Override
	protected void buildNearestRow(@NonNull View view, @NonNull List<Amenity> nearestAmenities,
			int iconId, String text, String amenityKey) {
		if (amenity == null) {
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
		if (!Algorithms.isEmpty(wpt.getDesc())) {
			String textPrefix = app.getString(R.string.shared_string_description);
			View.OnClickListener clickListener = v -> POIMapLayer.showPlainDescriptionDialog(view.getContext(), app, wpt.getDesc(), textPrefix);

			buildRow(view, null, null, textPrefix, wpt.getDesc(), 0,
					null, false, null, true, 10,
					false, false, false, clickListener, matchWidthDivider);
		}
	}

	@Override
	protected void showDescriptionDialog(@NonNull Context ctx, @NonNull String description, @NonNull String title) {
		ReadPointDescriptionFragment.showInstance(mapActivity, description);
	}

	protected Map<String, String> getAdditionalCardParams() {
		return AmenityExtensionsHelper.getImagesParams(amenityExtensions);
	}

	@Override
	public void buildInternal(View view) {
		buildDateRow(view, wpt.getTime());
		if (wpt.getSpeed() > 0) {
			buildRow(view, R.drawable.ic_action_speed,
					null, OsmAndFormatter.getFormattedSpeed((float) wpt.getSpeed(), app), 0, false, null, false, 0, false, null, false);
		}
		if (!Double.isNaN(wpt.getEle())) {
			buildRow(view, R.drawable.ic_action_altitude,
					null, OsmAndFormatter.getFormattedDistance((float) wpt.getEle(), app), 0, false, null, false, 0, false, null, false);
		}
		if (!Double.isNaN(wpt.getHdop())) {
			buildRow(view, R.drawable.ic_action_gps_info,
					null, Algorithms.capitalizeFirstLetterAndLowercase(app.getString(R.string.plugin_distance_point_hdop)) + ": " + (int) wpt.getHdop(), 0,
					false, null, false, 0, false, null, false);
		}
		prepareDescription(wpt, view);
		buildCommentRow(view, wpt.getComment());

		if (!Algorithms.isEmpty(amenityExtensions)) {
			boolean light = isLightContent();
			AdditionalInfoBundle bundle = new AdditionalInfoBundle(app, amenityExtensions);
			AmenityUIHelper helper = new AmenityUIHelper(mapActivity, getPreferredMapAppLang(), bundle);
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
			List<WptPt> points = selectedGpxFile.getGpxFile().getPointsList();
			if (!points.isEmpty()) {
				GpxFile gpx = selectedGpxFile.getGpxFile();
				String title = view.getContext().getString(R.string.context_menu_points_of_group);
				File file = new File(gpx.getPath());
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

	private CollapsableView getCollapsableWaypointsView(Context context, boolean collapsed,
			@NonNull GpxFile gpxFile, WptPt selectedPoint) {
		LinearLayout view = buildCollapsableContentView(context, collapsed, true);

		List<WptPt> points = gpxFile.getPointsList();
		String selectedCategory = selectedPoint != null && selectedPoint.getCategory() != null ? selectedPoint.getCategory() : "";
		int showCount = 0;
		for (WptPt point : points) {
			String currentCategory = point != null ? point.getCategory() : null;
			if (selectedCategory.equals(currentCategory)) {
				showCount++;
				boolean selected = point.equals(selectedPoint);
				TextViewEx button = buildButtonInCollapsableView(context, selected, false);
				button.setText(point.getName());

				if (!selected) {
					button.setOnClickListener(v -> {
						LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
						PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_WPT, point.getName());
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
			button.setOnClickListener(v -> TrackMenuFragment.openTrack(mapActivity, new File(gpxFile.getPath()), null));
			view.addView(button);
		}

		return new CollapsableView(view, this, collapsed);
	}
}
