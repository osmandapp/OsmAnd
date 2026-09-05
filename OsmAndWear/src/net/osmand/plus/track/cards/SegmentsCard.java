package net.osmand.plus.track.cards;

import static net.osmand.plus.myplaces.tracks.dialogs.GPXItemPagerAdapter.createGpxTabsView;
import static net.osmand.plus.track.cards.OptionsCard.EDIT_BUTTON_INDEX;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.myplaces.tracks.dialogs.GPXItemPagerAdapter;
import net.osmand.plus.myplaces.tracks.GPXTabItemType;
import net.osmand.plus.myplaces.tracks.dialogs.SegmentActionsListener;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.util.List;

public class SegmentsCard extends MapBaseCard {

	private final TrackDisplayHelper displayHelper;
	private final GpxDisplayItemType[] filterTypes = {GpxDisplayItemType.TRACK_SEGMENT};
	private final SegmentActionsListener listener;
	private final SelectedGpxFile selectedGpxFile;
	private final SelectedGpxPoint gpxPoint;

	private GPXTabItemType tabToOpen;

	private RecyclerView recyclerView;

	public SegmentsCard(@NonNull MapActivity mapActivity,
	                    @NonNull TrackDisplayHelper displayHelper,
	                    @Nullable SelectedGpxPoint gpxPoint,
	                    @Nullable SelectedGpxFile selectedGpxFile,
	                    @NonNull SegmentActionsListener listener,
	                    @NonNull GPXTabItemType tabToOpen) {
		super(mapActivity);
		this.listener = listener;
		this.selectedGpxFile = selectedGpxFile;
		this.displayHelper = displayHelper;
		this.gpxPoint = gpxPoint;
		this.tabToOpen = tabToOpen;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.segments_card;
	}

	public void setScrollAvailabilityListener(ScrollAvailabilityListener listener) {
		recyclerView.clearOnScrollListeners();
		recyclerView.addOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
				listener.onScrollToTopAvailable(isScrollToTopAvailable());
			}
		});
	}

	public boolean isScrollToTopAvailable() {
		return recyclerView.canScrollVertically(-1);
	}

	public void removeScrollAvailabilityListener() {
		recyclerView.clearOnScrollListeners();
	}

	public void disallowScrollOnChartTouch() {
		recyclerView.requestDisallowInterceptTouchEvent(true);
	}

	@Override
	public void updateContent() {
		List<GpxDisplayItem> items = TrackDisplayHelper.flatten(displayHelper.getOriginalGroups(filterTypes));

		updateLocationOnMap(items);
		recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(activity));
		recyclerView.setAdapter(new SegmentsAdapter(items, tabToOpen));
		tabToOpen = null;
		recyclerView.setHasFixedSize(true);

		LinearLayout noRoutesContainer = view.findViewById(R.id.no_routes_container);
		TextViewEx createRoutesButton = view.findViewById(R.id.create_routes_btn);
		TextViewEx noRoutesDescr = view.findViewById(R.id.gpx_no_routes_descr);

		String args = mapActivity.getString(R.string.plan_a_route);
		String noRoutesDescrText = mapActivity.getString(R.string.gpx_no_routes_descr, args);
		noRoutesDescr.setText(noRoutesDescrText);

		boolean showEmptyRoutes = items.isEmpty() && !selectedGpxFile.isShowCurrentTrack();
		AndroidUiHelper.updateVisibility(noRoutesContainer, showEmptyRoutes);
		AndroidUiHelper.updateVisibility(recyclerView, !showEmptyRoutes);
		createRoutesButton.setOnClickListener(v -> notifyButtonPressed(EDIT_BUTTON_INDEX));
	}

	private void updateLocationOnMap(@NonNull List<GpxDisplayItem> displayItems) {
		if (gpxPoint != null) {
			for (GpxDisplayItem item : displayItems) {
				TrkSegment segment = GPXItemPagerAdapter.getSegmentForAnalysis(item, item.analysis);
				if (segment != null && (segment.getPoints().contains(gpxPoint.getSelectedPoint())
						|| segment.getPoints().contains(gpxPoint.getPrevPoint())
						&& segment.getPoints().contains(gpxPoint.getNextPoint()))) {
					item.locationOnMap = gpxPoint.getSelectedPoint();
					listener.onPointSelected(segment, item.locationOnMap.getLat(), item.locationOnMap.getLon());
				}
			}
		}
	}

	private class SegmentsAdapter extends RecyclerView.Adapter<SegmentViewHolder> {

		private final List<GpxDisplayItem> displayItems;
		private GPXTabItemType tabToOpen;

		public SegmentsAdapter(List<GpxDisplayItem> displayItems, @Nullable GPXTabItemType tabToOpen) {
			this.displayItems = displayItems;
			this.tabToOpen = tabToOpen;
		}

		@NonNull
		@Override
		public SegmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new SegmentViewHolder(createGpxTabsView(parent, nightMode));
		}

		@Override
		public void onBindViewHolder(@NonNull SegmentViewHolder holder, int position) {
			GpxDisplayItem displayItem = displayItems.get(position);

			AndroidUiHelper.updateVisibility(holder.divider, position != 0);

			if (!Algorithms.isBlank(displayItem.trackSegmentName)) {
				holder.title.setText(displayItem.trackSegmentName);
			}
			AndroidUiHelper.updateVisibility(holder.title, !Algorithms.isBlank(displayItem.trackSegmentName));

			GPXItemPagerAdapter adapter = new GPXItemPagerAdapter(app, displayItem, displayHelper, listener, nightMode, true, mapActivity);
			holder.pager.setAdapter(adapter);
			holder.tabLayout.setViewPager(holder.pager);

			GPXTabItemType tabToOpen = getTabToOpen(position);
			if (tabToOpen != null) {
				int tabIndex = adapter.getTabIndex(tabToOpen);
				if (tabIndex != -1) {
					holder.pager.setCurrentItem(tabIndex);
				}
			}

			AndroidUiHelper.updateVisibility(holder.bottomDivider, position + 1 == displayItems.size());
		}

		@Nullable
		private GPXTabItemType getTabToOpen(int position) {
			if (tabToOpen == null || position > 0) {
				return null;
			}
			GPXTabItemType tabToOpen = this.tabToOpen;
			this.tabToOpen = null;
			return tabToOpen;
		}

		@Override
		public int getItemCount() {
			return displayItems.size();
		}
	}

	private static class SegmentViewHolder extends RecyclerView.ViewHolder {

		final View divider;
		final TextView title;
		final WrapContentHeightViewPager pager;
		final PagerSlidingTabStrip tabLayout;
		final View bottomDivider;


		public SegmentViewHolder(@NonNull View itemView) {
			super(itemView);
			divider = itemView.findViewById(R.id.list_item_divider);
			title = itemView.findViewById(R.id.track_segment_title);
			pager = itemView.findViewById(R.id.pager);
			tabLayout = itemView.findViewById(R.id.sliding_tabs);
			bottomDivider = itemView.findViewById(R.id.bottom_divider);
		}
	}

	public interface ScrollAvailabilityListener {

		void onScrollToTopAvailable(boolean available);

	}
}