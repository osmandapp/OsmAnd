package net.osmand.plus.track;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionAdapterListener;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class PointsGroupsCard extends MapBaseCard {

	public static final int SELECT_GROUP_INDEX = 0;

	private GpxDisplayGroup selectedGroup;
	private final List<GpxDisplayGroup> displayGroups = new ArrayList<>();

	public PointsGroupsCard(@NonNull MapActivity mapActivity, @NonNull List<GpxDisplayGroup> groups) {
		super(mapActivity);
		displayGroups.addAll(groups);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_groups_card;
	}

	public GpxDisplayGroup getSelectedGroup() {
		return selectedGroup;
	}

	@Override
	protected void updateContent() {
		ArrayList<HorizontalSelectionItem> items = new ArrayList<>();
		items.add(new HorizontalSelectionItem(app.getString(R.string.shared_string_all), null));
		for (GpxDisplayGroup group : displayGroups) {
			String categoryName = group.getName();
			if (Algorithms.isEmpty(categoryName)) {
				categoryName = app.getString(R.string.shared_string_gpx_points);
			}
			items.add(new HorizontalSelectionItem(categoryName, group));
		}
		final HorizontalSelectionAdapter selectionAdapter = new HorizontalSelectionAdapter(app, nightMode);
		selectionAdapter.setItems(items);
		selectionAdapter.setListener(new HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(HorizontalSelectionItem item) {
				selectedGroup = (GpxDisplayGroup) item.getObject();
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(PointsGroupsCard.this, SELECT_GROUP_INDEX);
				}
				selectionAdapter.notifyDataSetChanged();
			}
		});
		if (selectedGroup != null) {
			String categoryName = selectedGroup.getName();
			if (Algorithms.isEmpty(categoryName)) {
				categoryName = app.getString(R.string.shared_string_gpx_points);
			}
			selectionAdapter.setSelectedItemByTitle(categoryName);
		} else {
			selectionAdapter.setSelectedItemByTitle(app.getString(R.string.shared_string_all));
		}

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setAdapter(selectionAdapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
		selectionAdapter.notifyDataSetChanged();
	}
}