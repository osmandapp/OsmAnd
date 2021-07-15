package net.osmand.plus.track;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.myplaces.GPXItemPagerAdapter;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.myplaces.SegmentGPXAdapter;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;
import net.osmand.util.Algorithms;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SegmentsCard extends MapBaseCard {

	private TrackDisplayHelper displayHelper;
	private GpxDisplayItemType[] filterTypes = new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_SEGMENT};
	private SegmentActionsListener listener;
	private SelectedGpxPoint gpxPoint;

	public SegmentsCard(@NonNull MapActivity mapActivity, @NonNull TrackDisplayHelper displayHelper,
						@Nullable SelectedGpxPoint gpxPoint, @NonNull SegmentActionsListener listener) {
		super(mapActivity);
		this.listener = listener;
		this.displayHelper = displayHelper;
		this.gpxPoint = gpxPoint;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_container;
	}

	@Override
	protected void updateContent() {
		ViewGroup container = (ViewGroup) view;
		container.removeAllViews();
		List<GpxDisplayItem> items = TrackDisplayHelper.flatten(displayHelper.getOriginalGroups(filterTypes));
		for (int i = 0; i < items.size(); i++) {
			GpxDisplayItem displayItem = items.get(i);
			updateLocationOnMap(displayItem);

			View segmentView = SegmentGPXAdapter.createGpxTabsView(displayHelper, container, listener, nightMode);

			AndroidUiHelper.updateVisibility(segmentView.findViewById(R.id.list_item_divider), i != 0);

			if (!Algorithms.isBlank(displayItem.trackSegmentName)) {
				TextView title = segmentView.findViewById(R.id.track_segment_title);
				title.setText(displayItem.trackSegmentName);
				AndroidUiHelper.updateVisibility(title, true);
			}

			WrapContentHeightViewPager pager = segmentView.findViewById(R.id.pager);
			PagerSlidingTabStrip tabLayout = segmentView.findViewById(R.id.sliding_tabs);

			pager.setAdapter(new GPXItemPagerAdapter(app, displayItem, displayHelper, nightMode, listener, false));
			tabLayout.setViewPager(pager);

			container.addView(segmentView);
		}
	}

	private void updateLocationOnMap(GpxDisplayItem item) {
		if (gpxPoint != null) {
			TrkSegment segment = GPXItemPagerAdapter.getSegmentForAnalysis(item, item.analysis);
			if (segment != null && (segment.points.contains(gpxPoint.getSelectedPoint())
					|| segment.points.contains(gpxPoint.getPrevPoint())
					&& segment.points.contains(gpxPoint.getNextPoint()))) {
				item.locationOnMap = gpxPoint.getSelectedPoint();
				listener.onPointSelected(segment, item.locationOnMap.lat, item.locationOnMap.lon);
			}
		}
	}
}