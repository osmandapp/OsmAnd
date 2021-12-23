package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionAdapterListener;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PointsGroupsCard extends MapBaseCard {

	public static final int SELECT_GROUP_INDEX = 0;

	private GpxDisplayGroup selectedGroup;
	private final SelectedGpxFile selectedGpxFile;
	private final List<GpxDisplayGroup> displayGroups = new ArrayList<>();

	public PointsGroupsCard(@NonNull MapActivity mapActivity,
	                        @NonNull List<GpxDisplayGroup> groups,
	                        @NonNull SelectedGpxFile selectedGpxFile) {
		super(mapActivity);
		this.selectedGpxFile = selectedGpxFile;
		displayGroups.addAll(groups);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_groups_card;
	}

	public GpxDisplayGroup getSelectedGroup() {
		return selectedGroup;
	}

	public void updateContent(@NonNull List<GpxDisplayGroup> updatedGroups) {
		displayGroups.clear();
		displayGroups.addAll(updatedGroups);
		updateContent();
	}

	@Override
	public void updateContent() {
		ArrayList<HorizontalSelectionItem> items = new ArrayList<>();
		items.add(new HorizontalSelectionItem(app.getString(R.string.shared_string_all), null));

		int iconSizePx = app.getResources().getDimensionPixelSize(R.dimen.poi_icon_size);
		int iconColorId = ColorUtilities.getSecondaryIconColorId(nightMode);
		int smallPadding = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);

		for (GpxDisplayGroup group : displayGroups) {
			String categoryDisplayName = Algorithms.isEmpty(group.getName())
					? app.getString(R.string.shared_string_gpx_points)
					: group.getName();
			HorizontalSelectionItem item = new HorizontalSelectionItem(categoryDisplayName, group);
			if (selectedGpxFile.isGroupHidden(group.getName())) {
				item.setIconId(R.drawable.ic_action_hide_16);
				item.setIconColorId(iconColorId);
				item.setIconSizePx(iconSizePx);
				item.setHorizontalPaddingPx(smallPadding);
			}
			items.add(item);
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