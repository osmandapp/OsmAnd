package net.osmand.plus.track.cards;

import static net.osmand.plus.myplaces.tracks.dialogs.GPXItemPagerAdapter.setupGpxTabsView;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.charts.TrackChartPoints;
import net.osmand.plus.myplaces.tracks.dialogs.GPXItemPagerAdapter;
import net.osmand.plus.myplaces.tracks.dialogs.SegmentActionsListener;
import net.osmand.plus.track.helpers.FilteredSelectedGpxFile;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;

public class GpsFilterGraphCard extends GpsFilterBaseCard {

	private final TrackDisplayHelper displayHelper;
	private final TrackChartPoints trackChartPoints;

	private View view;
	private PagerSlidingTabStrip slidingTabs;

	public GpsFilterGraphCard(@NonNull MapActivity mapActivity,
	                          @NonNull Fragment target,
	                          @NonNull FilteredSelectedGpxFile filteredSelectedGpxFile) {
		super(mapActivity, target, filteredSelectedGpxFile);
		displayHelper = createTrackDisplayHelper();
		trackChartPoints = new TrackChartPoints();
	}

	private TrackDisplayHelper createTrackDisplayHelper() {
		TrackDisplayHelper displayHelper = new TrackDisplayHelper(app);
		GpxFile gpxFile = filteredSelectedGpxFile.getGpxFile();
		displayHelper.setFile(new File(gpxFile.getPath()));
		displayHelper.setGpx(gpxFile);
		displayHelper.setSelectedGpxFile(filteredSelectedGpxFile);
		return displayHelper;
	}

	@Override
	protected int getMainContentLayoutId() {
		return R.layout.gpx_list_item_tab_content;
	}

	@Override
	protected void updateMainContent() {
		if (view == null) {
			view = inflateMainContent();
		}

		GpxDisplayItem displayItem = getGpxDisplayItem();
		if (displayItem != null) {
			setupGraph(displayItem);
			AndroidUiHelper.updateVisibility(view, true);
		} else {
			AndroidUiHelper.updateVisibility(view, false);
		}
	}

	@Nullable
	private GpxDisplayItem getGpxDisplayItem() {
		GpxDisplayItemType[] filterTypes = {GpxDisplayItemType.TRACK_SEGMENT};
		List<GpxDisplayItem> displayItems = TrackDisplayHelper.flatten(displayHelper.getOriginalGroups(filterTypes));
		return Algorithms.isEmpty(displayItems) ? null : displayItems.get(0);
	}

	private void setupGraph(@NonNull GpxDisplayItem displayItem) {
		setupGpxTabsView(view, nightMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.list_item_divider), false);

		slidingTabs = view.findViewById(R.id.sliding_tabs);
		WrapContentHeightViewPager pager = view.findViewById(R.id.pager);

		GPXItemPagerAdapter pagerAdapter = new GPXItemPagerAdapter(app, displayItem, displayHelper, getSegmentActionsListener(), nightMode, false, mapActivity);
		pagerAdapter.setHideJoinGapsBottomButtons(true);
		pagerAdapter.setChartHMargin(getDimen(R.dimen.content_padding));
		pager.setAdapter(pagerAdapter);
		slidingTabs.setViewPager(pager);
	}

	private SegmentActionsListener getSegmentActionsListener() {
		return new SegmentActionsListener() {

			@Override
			public void updateContent() {
			}

			@Override
			public void onChartTouch() {
				disallowScroll();
			}

			@Override
			public void scrollBy(int px) {
			}

			@Override
			public void onPointSelected(TrkSegment segment, double lat, double lon) {
				int segmentColor = segment != null ? segment.getColor(0) : 0;
				trackChartPoints.setSegmentColor(segmentColor);
				trackChartPoints.setHighlightedPoint(new LatLon(lat, lon));
				mapActivity.getMapLayers().getGpxLayer().setTrackChartPoints(trackChartPoints);
				mapActivity.refreshMap();
			}

			@Override
			public void openSplitInterval(GpxDisplayItem gpxItem, TrkSegment trkSegment) {
			}

			@Override
			public void showOptionsPopupMenu(View view, TrkSegment trkSegment, boolean confirmDeletion, GpxDisplayItem gpxItem) {
			}

			@Override
			public void openAnalyzeOnMap(@NonNull GpxDisplayItem gpxItem) {
			}

			@Override
			public void openGetAltitudeBottomSheet(@NonNull GpxDisplayItem gpxItem) {

			}
		};
	}

	@Override
	public void onFinishFiltering() {
		updateMainContent();
	}
}