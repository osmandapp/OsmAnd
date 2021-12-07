package net.osmand.plus.track.cards;

import android.view.View;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.data.LatLon;
import net.osmand.plus.track.helpers.FilteredSelectedGpxFile;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.TrackChartPoints;
import net.osmand.plus.myplaces.GPXItemPagerAdapter;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.myplaces.SegmentGPXAdapter;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class GpsFilterGraphCard extends GpsFilterBaseCard {

	private final TrackDisplayHelper displayHelper;
	private final TrackChartPoints trackChartPoints;

	private View view;
	PagerSlidingTabStrip slidingTabs;

	public GpsFilterGraphCard(@NonNull MapActivity mapActivity,
	                          @NonNull Fragment target,
	                          @NonNull FilteredSelectedGpxFile filteredSelectedGpxFile) {
		super(mapActivity, target, filteredSelectedGpxFile);
		displayHelper = createTrackDisplayHelper();
		trackChartPoints = new TrackChartPoints();
	}

	private TrackDisplayHelper createTrackDisplayHelper() {
		TrackDisplayHelper displayHelper = new TrackDisplayHelper(app);
		GPXFile gpxFile = filteredSelectedGpxFile.getGpxFile();
		displayHelper.setFile(new File(gpxFile.path));
		displayHelper.setGpx(gpxFile);
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
		GpxDisplayItemType[] filterTypes = new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_SEGMENT};
		List<GpxDisplayItem> displayItems = TrackDisplayHelper.flatten(displayHelper.getOriginalGroups(filterTypes));
		return Algorithms.isEmpty(displayItems) ? null : displayItems.get(0);
	}

	private void setupGraph(@NonNull GpxDisplayItem displayItem) {
		SegmentGPXAdapter.setupGpxTabsView(view, nightMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.list_item_divider), false);

		slidingTabs = view.findViewById(R.id.sliding_tabs);
		WrapContentHeightViewPager pager = view.findViewById(R.id.pager);

		GPXItemPagerAdapter pagerAdapter = new GPXItemPagerAdapter(app, displayItem, displayHelper, nightMode,
				getSegmentActionsListener(), false, true);
		pagerAdapter.setChartHMargin(app.getResources().getDimensionPixelSize(R.dimen.content_padding));
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
		};
	}

	@Override
	public void onFinishFiltering() {
		updateMainContent();
	}
}