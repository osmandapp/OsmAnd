package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.helpers.TrackSelectSegmentAdapter;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;

public class TrackEditCard extends MapBaseCard {

	private final GPXFile gpxFile;

	public TrackEditCard(MapActivity mapActivity, GPXFile gpxFile) {
		super(mapActivity);
		this.gpxFile = gpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_track_item;
	}

	private GpxDataItem getDataItem(final GPXInfo info) {
		GpxDataItemCallback itemCallback = new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public void onGpxDataItemReady(GpxDataItem item) {
				if (item != null) {
					updateContent();
				}
			}
		};
		return app.getGpxDbHelper().getItem(new File(info.getFileName()), itemCallback);
	}

	@Override
	protected void updateContent() {
		String fileName = null;
		File file = null;
		if (!Algorithms.isEmpty(gpxFile.path)) {
			file = new File(gpxFile.path);
			fileName = gpxFile.path;
		} else if (!Algorithms.isEmpty(gpxFile.tracks)) {
			fileName = gpxFile.tracks.get(0).name;
		}
		if (Algorithms.isEmpty(fileName)) {
			fileName = app.getString(R.string.shared_string_gpx_track);
		}

		GPXInfo gpxInfo = new GPXInfo(gpxFile.path, file != null ? file.lastModified() : 0, file != null ? file.length() : 0);
		GpxDataItem dataItem = getDataItem(gpxInfo);
		String title = GpxUiHelper.getGpxTitle(Algorithms.getFileWithoutDirs(fileName));
		GPXRouteParamsBuilder routeParams = app.getRoutingHelper().getCurrentGPXRoute();
		if (gpxFile.getNonEmptySegmentsCount() > 1 && routeParams != null && routeParams.getSelectedSegment() != -1) {
			int selectedSegmentCount = routeParams.getSelectedSegment() + 1;
			int totalSegmentCount = routeParams.getFile().getNonEmptyTrkSegments(false).size();
			title = app.getResources().getString(R.string.of, selectedSegmentCount, totalSegmentCount) + ", " + title;
		}
		GpxUiHelper.updateGpxInfoView(view, title, gpxInfo, dataItem, false, app);

		if (gpxFile.getNonEmptySegmentsCount() > 1 && routeParams != null)
			if (routeParams.getSelectedSegment() != -1 && gpxFile.getNonEmptySegmentsCount() > routeParams.getSelectedSegment()) {
					TextView distanceView = view.findViewById(R.id.distance);
					TextView timeView = view.findViewById(R.id.time);
					ImageView timeIcon = view.findViewById(R.id.time_icon);
					AndroidUiHelper.updateVisibility(view.findViewById(R.id.points_icon), false);
					AndroidUiHelper.updateVisibility(view.findViewById(R.id.points_count), false);
					List<GPXUtilities.TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
					GPXUtilities.TrkSegment segment = segments.get(routeParams.getSelectedSegment());
					double distance = TrackSelectSegmentAdapter.getDistance(segment);
					long time = TrackSelectSegmentAdapter.getSegmentTime(segment);
					boolean timeAvailable = time != 1;
					if (timeAvailable) {
						timeView.setText(Algorithms.formatDuration((int) (time / 1000),
								app.accessibilityEnabled()));
					}
					AndroidUiHelper.updateVisibility(timeView, timeAvailable);
					AndroidUiHelper.updateVisibility(timeIcon, timeAvailable);
					distanceView.setText(OsmAndFormatter.getFormattedDistance((float) distance, app));
				}

		ImageButton editButton = view.findViewById(R.id.show_on_map);
		editButton.setVisibility(View.VISIBLE);
		editButton.setImageDrawable(getContentIcon(R.drawable.ic_action_edit_dark));
		editButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardPressed(TrackEditCard.this);
				}
			}
		});

		int minCardHeight = app.getResources().getDimensionPixelSize(R.dimen.setting_list_item_large_height);
		int listContentPadding = app.getResources().getDimensionPixelSize(R.dimen.list_content_padding);

		LinearLayout container = view.findViewById(R.id.container);
		container.setMinimumHeight(minCardHeight);
		AndroidUtils.setPadding(container, listContentPadding, 0, 0, 0);

		int activeColor = getActiveColor();
		int bgColor = UiUtilities.getColorWithAlpha(activeColor, 0.1f);
		view.setBackgroundDrawable(new ColorDrawable(bgColor));
	}
}