package net.osmand.plus.track;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.CompoundButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
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
	private final SelectedGpxFile selectedGpxFile;
	private final List<GpxDisplayGroup> displayGroups = new ArrayList<>();

	public PointsGroupsCard(@NonNull MapActivity mapActivity,
							@NonNull SelectedGpxFile selectedGpxFile,
							@NonNull List<GpxDisplayGroup> groups) {
		super(mapActivity);
		displayGroups.addAll(groups);
		this.selectedGpxFile = selectedGpxFile;
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
				updateShowOnMapItem();
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
		updateShowOnMapItem();

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setAdapter(selectionAdapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
		selectionAdapter.notifyDataSetChanged();
	}

	private void updateShowOnMapItem() {
		View container = view.findViewById(R.id.show_on_map);
		if (selectedGroup != null) {
			TextView title = view.findViewById(R.id.title);
			title.setText(R.string.shared_string_show_on_map);

			final String name = Algorithms.isEmpty(selectedGroup.getName()) ? null : selectedGroup.getName();
			boolean checked = !selectedGpxFile.getHiddenGroups().contains(name);
			CompoundButton compoundButton = view.findViewById(R.id.compound_button);
			compoundButton.setChecked(checked);
			UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);

			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean checked = !compoundButton.isChecked();
					if (checked) {
						selectedGpxFile.removeHiddenGroups(name);
					} else {
						selectedGpxFile.addHiddenGroups(name);
					}
					app.getSelectedGpxHelper().updateSelectedGpxFile(selectedGpxFile);

					compoundButton.setChecked(checked);
					mapActivity.refreshMap();
				}
			});
		}
		AndroidUiHelper.updateVisibility(container, selectedGroup != null);
	}
}