package net.osmand.plus.myplaces;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.track.TrackDisplayHelper;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;

import java.util.List;

public class SegmentGPXAdapter extends ArrayAdapter<GpxDisplayItem> {

	private OsmandApplication app;
	private TrackDisplayHelper displayHelper;
	private SegmentActionsListener listener;
	private boolean nightMode;

	public SegmentGPXAdapter(@NonNull Context context, @NonNull List<GpxDisplayItem> items,
							 @NonNull TrackDisplayHelper displayHelper,
							 @NonNull SegmentActionsListener listener,
							 boolean nightMode) {
		super(context, R.layout.gpx_list_item_tab_content, items);
		this.app = (OsmandApplication) context.getApplicationContext();
		this.displayHelper = displayHelper;
		this.listener = listener;
		this.nightMode = nightMode;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		View row = convertView;
		boolean create = false;
		if (row == null) {
			create = true;
			row = createGpxTabsView(displayHelper, parent, listener, nightMode);
		}
		GpxDisplayItem item = getItem(position);
		if (item != null) {
			WrapContentHeightViewPager pager = row.findViewById(R.id.pager);
			PagerSlidingTabStrip tabLayout = row.findViewById(R.id.sliding_tabs);

			pager.setAdapter(new GPXItemPagerAdapter(tabLayout, item, displayHelper, listener));
			if (create) {
				tabLayout.setViewPager(pager);
			} else {
				tabLayout.notifyDataSetChanged(true);
			}
		}
		return row;
	}

	public static View createGpxTabsView(TrackDisplayHelper displayHelper, ViewGroup root,
										 SegmentActionsListener listener, boolean nightMode) {
		Context context = root.getContext();
		View row = UiUtilities.getInflater(context, nightMode).inflate(R.layout.gpx_list_item_tab_content, root, false);

		PagerSlidingTabStrip tabLayout = row.findViewById(R.id.sliding_tabs);
		tabLayout.setTabBackground(R.color.color_transparent);
		tabLayout.setIndicatorColorResource(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
		tabLayout.setIndicatorBgColorResource(nightMode ? R.color.divider_color_dark : R.color.divider_color_light);
		tabLayout.setIndicatorHeight(AndroidUtils.dpToPx(context, 1f));
		if (!nightMode) {
			tabLayout.setTextColor(tabLayout.getIndicatorColor());
			tabLayout.setTabInactiveTextColor(ContextCompat.getColor(row.getContext(), R.color.text_color_secondary_light));
		}
		tabLayout.setTextSize(AndroidUtils.spToPx(context, 12f));
		tabLayout.setShouldExpand(true);
		WrapContentHeightViewPager pager = row.findViewById(R.id.pager);
		pager.setSwipeable(false);
		pager.setOffscreenPageLimit(2);

		return row;
	}
}
