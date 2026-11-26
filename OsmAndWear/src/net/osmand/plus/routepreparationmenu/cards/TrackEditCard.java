package net.osmand.plus.routepreparationmenu.cards;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.TrackSelectSegmentAdapter;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper.GpxDataItemCallback;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.Route;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;

public class TrackEditCard extends MapBaseCard {

	private final GpxFile gpxFile;

	public TrackEditCard(MapActivity mapActivity, GpxFile gpxFile) {
		super(mapActivity);
		this.gpxFile = gpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_track_item;
	}

	private GpxDataItem getDataItem(GPXInfo info) {
		GpxDataItemCallback itemCallback = item -> updateContent();
		return app.getGpxDbHelper().getItem(new KFile(info.getFileName()), itemCallback);
	}

	@Override
	protected void updateContent() {
		String fileName = null;
		File file = null;
		if (!Algorithms.isEmpty(gpxFile.getPath())) {
			file = new File(gpxFile.getPath());
			fileName = gpxFile.getPath();
		} else if (!Algorithms.isEmpty(gpxFile.getTracks())) {
			fileName = gpxFile.getTracks().get(0).getName();
		}
		if (Algorithms.isEmpty(fileName)) {
			fileName = app.getString(R.string.shared_string_gpx_track);
		}

		GPXInfo gpxInfo = new GPXInfo(fileName, file);
		GpxTrackAnalysis analysis = null;
		if (file != null) {
			GpxDataItem dataItem = getDataItem(gpxInfo);
			if (dataItem != null) {
				analysis = dataItem.getAnalysis();
			}
		} else {
			analysis = gpxFile.getAnalysis(0);
		}
		String title = GpxHelper.INSTANCE.getGpxTitle(Algorithms.getFileWithoutDirs(fileName));
		GPXRouteParamsBuilder routeParams = app.getRoutingHelper().getCurrentGPXRoute();

		if (routeParams != null) {
			title = getGpxTitleWithSelectedItem(app, routeParams, title);
		}
		GpxUiHelper.updateGpxInfoView(view, title, gpxInfo, analysis, app);

		if (routeParams != null) {
			int selectedRoute = routeParams.getSelectedRoute();
			int selectedSegment = routeParams.getSelectedSegment();
			if (selectedSegment != -1 && gpxFile.getNonEmptySegmentsCount() > selectedSegment) {
				List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
				TrkSegment segment = segments.get(selectedSegment);
				setupSecondRow(segment.getPoints());
			} else if (selectedRoute != -1 && gpxFile.getRoutes().size() > selectedRoute) {
				Route route = gpxFile.getRoutes().get(selectedRoute);
				setupSecondRow(route.getPoints());
			}
		}
		ImageButton editButton = view.findViewById(R.id.show_on_map);
		editButton.setVisibility(View.VISIBLE);
		editButton.setImageDrawable(getContentIcon(R.drawable.ic_action_edit_dark));
		editButton.setOnClickListener(v -> notifyCardPressed());

		int minCardHeight = getDimen(R.dimen.setting_list_item_large_height);
		int listContentPadding = getDimen(R.dimen.list_content_padding);

		LinearLayout container = view.findViewById(R.id.container);
		container.setMinimumHeight(minCardHeight);
		AndroidUtils.setPadding(container, listContentPadding, 0, 0, 0);

		int activeColor = getActiveColor();
		view.setBackgroundColor(ColorUtilities.getColorWithAlpha(activeColor, 0.1f));
	}

	private void setupSecondRow(@NonNull List<WptPt> points) {
		TextView timeView = view.findViewById(R.id.time);
		ImageView timeIcon = view.findViewById(R.id.time_icon);
		TextView distanceView = view.findViewById(R.id.distance);

		long time = TrackSelectSegmentAdapter.getSegmentTime(points);
		double distance = TrackSelectSegmentAdapter.getDistance(points);

		boolean timeAvailable = time != 1;
		if (timeAvailable) {
			timeView.setText(Algorithms.formatDuration((int) (time / 1000), app.accessibilityEnabled()));
		}
		AndroidUiHelper.updateVisibility(timeView, timeAvailable);
		AndroidUiHelper.updateVisibility(timeIcon, timeAvailable);
		distanceView.setText(OsmAndFormatter.getFormattedDistance((float) distance, app));

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.points_icon), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.points_count), false);
	}

	public static String getGpxTitleWithSelectedItem(@NonNull OsmandApplication app, @NonNull GPXRouteParamsBuilder paramsBuilder, String fileName) {
		GpxFile gpxFile = paramsBuilder.getFile();
		int selectedRoute = paramsBuilder.getSelectedRoute();
		int selectedSegment = paramsBuilder.getSelectedSegment();
		if (gpxFile.getNonEmptySegmentsCount() > 1 && selectedSegment != -1) {
			int totalCount = gpxFile.getNonEmptyTrkSegments(false).size();
			return app.getString(R.string.of, selectedSegment + 1, totalCount) + ", " + fileName;
		} else if (gpxFile.getRoutes().size() > 1 && selectedRoute != -1) {
			int totalCount = gpxFile.getRoutes().size();
			return app.getString(R.string.of, selectedRoute + 1, totalCount) + ", " + fileName;
		}
		return fileName;
	}
}