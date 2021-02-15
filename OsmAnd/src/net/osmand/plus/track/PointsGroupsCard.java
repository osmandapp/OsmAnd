package net.osmand.plus.track;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionAdapterListener;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

import java.util.ArrayList;
import java.util.List;

public class PointsGroupsCard extends BaseCard {

	private final TrackDisplayHelper displayHelper;
	private final GpxDisplayItemType[] filterTypes = new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_POINTS, GpxDisplayItemType.TRACK_ROUTE_POINTS};
	private final List<GpxDisplayGroup> displayGroups = new ArrayList<>();

	public PointsGroupsCard(@NonNull MapActivity mapActivity, @NonNull TrackDisplayHelper displayHelper,
							@NonNull List<GpxDisplayGroup> groups) {
		super(mapActivity);
		this.displayHelper = displayHelper;
		displayGroups.addAll(groups);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_groups_card;
	}

	@Override
	protected void updateContent() {
		List<String> groupNames = new ArrayList<>();
		for (GpxDisplayGroup group : displayGroups) {
			String categoryName = group.getName();
			if (TextUtils.isEmpty(categoryName)) {
				categoryName = app.getString(R.string.shared_string_gpx_points);
			}
			groupNames.add(categoryName);
		}
		HorizontalSelectionAdapter selectionAdapter = new HorizontalSelectionAdapter(app, nightMode);
		selectionAdapter.setTitledItems(groupNames);
		selectionAdapter.setSelectedItemByTitle(app.getString(R.string.shared_string_gpx_points));
		selectionAdapter.setListener(new HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(HorizontalSelectionItem item) {

			}
		});

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setAdapter(selectionAdapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
		selectionAdapter.notifyDataSetChanged();
	}
}