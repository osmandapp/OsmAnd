package net.osmand.plus.track;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;

import net.osmand.plus.ColorUtilities;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.ChipsAdapter.OnSelectChipListener;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
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

	@Override
	protected void updateContent() {
		HorizontalChipsView chipsView = view.findViewById(R.id.chips_view);

		ArrayList<ChipItem> items = new ArrayList<>();
		ChipItem categoryAll = new ChipItem(app.getString(R.string.shared_string_all));
		categoryAll.title = categoryAll.id;
		items.add(categoryAll);

		int iconSizePx = app.getResources().getDimensionPixelSize(R.dimen.poi_icon_size);
		int iconColorId = ColorUtilities.getSecondaryIconColorId(nightMode);
		int smallPadding = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);

		Set<String> hidden = selectedGpxFile.getHiddenGroups();
		for (GpxDisplayGroup group : displayGroups) {
			String categoryName = group.getName();
			if (Algorithms.isEmpty(categoryName)) {
				categoryName = app.getString(R.string.shared_string_gpx_points);
			}
			ChipItem item = new ChipItem(categoryName);
			item.title = categoryName;
			item.tag = group;
			item.onAfterBindCallback = (chip, holder) -> {
				if (hidden.contains(chip.id)) {
					Drawable image = getColoredIcon(R.drawable.ic_action_hide_16, iconColorId);
					holder.image.setImageDrawable(image);
					holder.image.setVisibility(View.VISIBLE);

					LayoutParams imgLayoutParams = holder.image.getLayoutParams();
					imgLayoutParams.height = iconSizePx;
					imgLayoutParams.width = iconSizePx;

					int top = holder.container.getPaddingTop();
					int bottom = holder.container.getPaddingBottom();
					holder.container.setPadding(smallPadding, top, smallPadding, bottom);

					holder.image.requestLayout();
				}
			};
			items.add(item);
		}
		chipsView.setItems(items);

		String selectedId = categoryAll.id;
		if (selectedGroup != null) {
			selectedId = selectedGroup.getName();
			if (Algorithms.isEmpty(selectedId)) {
				selectedId = app.getString(R.string.shared_string_gpx_points);
			}
		}
		ChipItem selected = chipsView.getChipById(selectedId);
		chipsView.setSelected(selected);

		chipsView.setOnSelectChipListener(new OnSelectChipListener() {
			@Override
			public boolean onSelectChip(ChipItem chip) {
				selectedGroup = (GpxDisplayGroup) chip.tag;
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(PointsGroupsCard.this, SELECT_GROUP_INDEX);
				}
				return true;
			}
		});
		chipsView.notifyDataSetChanged();
	}
}