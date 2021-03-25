package net.osmand.plus.track;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.GPXItemPagerAdapter;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.myplaces.SegmentGPXAdapter;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;

import java.util.List;

public class SegmentsCard extends BaseCard {

	private TrackDisplayHelper displayHelper;
	private GpxDisplayItemType[] filterTypes = new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_SEGMENT};
	private SegmentActionsListener listener;

	public SegmentsCard(@NonNull MapActivity mapActivity, @NonNull TrackDisplayHelper displayHelper,
						@NonNull SegmentActionsListener listener) {
		super(mapActivity);
		this.displayHelper = displayHelper;
		this.listener = listener;
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
		for (GpxDisplayItem displayItem : items) {
			View segmentView = SegmentGPXAdapter.createGpxTabsView(displayHelper, container, listener, nightMode);

			WrapContentHeightViewPager pager = segmentView.findViewById(R.id.pager);
			PagerSlidingTabStrip tabLayout = segmentView.findViewById(R.id.sliding_tabs);

			pager.setAdapter(new GPXItemPagerAdapter(app, displayItem, displayHelper, nightMode, listener, false));
			tabLayout.setViewPager(pager);

			container.addView(segmentView);
		}
	}
}