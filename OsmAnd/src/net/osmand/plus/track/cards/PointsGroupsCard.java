package net.osmand.plus.track.cards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

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
	protected void updateContent() {
		HorizontalChipsView chipsView = view.findViewById(R.id.chips_view);

		ArrayList<ChipItem> items = new ArrayList<>();
		ChipItem categoryAll = new ChipItem(app.getString(R.string.shared_string_all));
		categoryAll.title = categoryAll.id;
		categoryAll.contentDescription = categoryAll.id;
		items.add(categoryAll);

		int iconSizePx = getDimen(R.dimen.poi_icon_size);
		int iconColorId = ColorUtilities.getSecondaryIconColorId(nightMode);
		int smallPadding = getDimen(R.dimen.content_padding_small);

		for (GpxDisplayGroup group : displayGroups) {
			String categoryDisplayName = Algorithms.isEmpty(group.getName())
					? app.getString(R.string.shared_string_gpx_points)
					: group.getName();
			ChipItem item = new ChipItem(categoryDisplayName);
			item.title = categoryDisplayName;
			item.contentDescription = categoryDisplayName;
			item.tag = group;
			item.onAfterViewBoundCallback = (chip, holder) -> {
				GpxDisplayGroup displayGroup = (GpxDisplayGroup) chip.tag;
				if (selectedGpxFile.isGroupHidden(displayGroup.getName())) {
					Drawable image = getColoredIcon(R.drawable.ic_action_hide_16, iconColorId);
					holder.image.setImageDrawable(image);
					holder.image.setVisibility(View.VISIBLE);

					LayoutParams imgLayoutParams = holder.image.getLayoutParams();
					imgLayoutParams.height = iconSizePx;
					imgLayoutParams.width = iconSizePx;

					int top = holder.container.getPaddingTop();
					int bottom = holder.container.getPaddingBottom();
					holder.container.setPadding(smallPadding, top, smallPadding, bottom);
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

		chipsView.setOnSelectChipListener(chip -> {
			selectedGroup = (GpxDisplayGroup) chip.tag;
			CardListener listener = getListener();
			if (listener != null) {
				listener.onCardButtonPressed(this, SELECT_GROUP_INDEX);
			}
			chipsView.smoothScrollTo(chip);
			return true;
		});
		chipsView.notifyDataSetChanged();
	}
}