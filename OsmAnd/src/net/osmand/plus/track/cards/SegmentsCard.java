package net.osmand.plus.track.cards;

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

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.myplaces.GPXItemPagerAdapter;
import net.osmand.plus.myplaces.SegmentActionsListener;
import net.osmand.plus.myplaces.SegmentGPXAdapter;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.util.List;

public class SegmentsCard extends MapBaseCard {

	private final TrackDisplayHelper displayHelper;
	private final GpxDisplayItemType[] filterTypes = new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_SEGMENT};
	private final SegmentActionsListener listener;
	private final SelectedGpxPoint gpxPoint;

	public SegmentsCard(@NonNull MapActivity mapActivity, @NonNull TrackDisplayHelper displayHelper,
	                    @Nullable SelectedGpxPoint gpxPoint, @NonNull SegmentActionsListener listener) {
		super(mapActivity);
		this.listener = listener;
		this.displayHelper = displayHelper;
		this.gpxPoint = gpxPoint;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.segments_card;
	}

	public void setScrollAvailabilityListener(ScrollAvailabilityListener listener) {
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.clearOnScrollListeners();
		recyclerView.addOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
				listener.onScrollToTopAvailable(isScrollToTopAvailable());
			}
		});
	}

	public boolean isScrollToTopAvailable() {
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		return recyclerView.canScrollVertically(-1);
	}

	public void removeScrollAvailabilityListener() {
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.clearOnScrollListeners();
	}

	public void disallowScrollOnChartTouch() {
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.requestDisallowInterceptTouchEvent(true);
	}

	@Override
	public void updateContent() {
		List<GpxDisplayItem> items = TrackDisplayHelper.flatten(displayHelper.getOriginalGroups(filterTypes));

		updateLocationOnMap(items);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(activity));
		recyclerView.setAdapter(new SegmentsAdapter(items));
		recyclerView.setHasFixedSize(true);

		LinearLayout noRoutesContainer = view.findViewById(R.id.no_routes_container);
		TextViewEx createRoutesButton = view.findViewById(R.id.create_routes_btn);

		GPXFile gpxFile = displayHelper.getGpx();
		if (gpxFile != null){
			boolean notRecording = gpxFile.path == null && gpxFile.showCurrentTrack;
			AndroidUiHelper.updateVisibility(noRoutesContainer, items.isEmpty() || notRecording);
		}
		createRoutesButton.setOnClickListener(v -> SegmentsCard.this.notifyButtonPressed(EDIT_BUTTON_INDEX));
	}

	private void updateLocationOnMap(@NonNull List<GpxDisplayItem> displayItems) {
		if (gpxPoint != null) {
			for (GpxDisplayItem item : displayItems) {
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

	private class SegmentsAdapter extends RecyclerView.Adapter<SegmentViewHolder> {

		private final List<GpxDisplayItem> displayItems;

		public SegmentsAdapter(List<GpxDisplayItem> displayItems) {
			this.displayItems = displayItems;
		}

		@NonNull
		@Override
		public SegmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View segmentView = SegmentGPXAdapter.createGpxTabsView(parent, nightMode);
			return new SegmentViewHolder(segmentView);
		}

		@Override
		public void onBindViewHolder(@NonNull SegmentViewHolder holder, int position) {
			GpxDisplayItem displayItem = displayItems.get(position);

			AndroidUiHelper.updateVisibility(holder.divider, position != 0);

			if (!Algorithms.isBlank(displayItem.trackSegmentName)) {
				holder.title.setText(displayItem.trackSegmentName);
			}
			AndroidUiHelper.updateVisibility(holder.title, !Algorithms.isBlank(displayItem.trackSegmentName));

			holder.pager.setAdapter(new GPXItemPagerAdapter(app, displayItem, displayHelper, nightMode,
					listener, false, false));
			holder.tabLayout.setViewPager(holder.pager);

			AndroidUiHelper.updateVisibility(holder.bottomDivider, position + 1 == displayItems.size());
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