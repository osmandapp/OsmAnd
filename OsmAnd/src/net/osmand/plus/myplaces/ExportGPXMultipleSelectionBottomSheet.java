package net.osmand.plus.myplaces;

import static net.osmand.plus.myplaces.AvailableGPXFragment.getGpxTrackAnalysis;
import static net.osmand.util.Algorithms.formatDuration;
import static net.osmand.view.ThreeStateCheckbox.State.CHECKED;
import static net.osmand.view.ThreeStateCheckbox.State.MISC;
import static net.osmand.view.ThreeStateCheckbox.State.UNCHECKED;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.base.SelectionBottomSheet;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class ExportGPXMultipleSelectionBottomSheet extends SelectionBottomSheet {

	public static final String TAG = ExportGPXMultipleSelectionBottomSheet.class.getSimpleName();
	private final List<SelectableItem> selectedItems = new ArrayList<>();
	private SelectionUpdateListener selectionUpdateListener;

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		selectAllButton.setOnClickListener(v -> {
			checkBox.performClick();
			boolean checked = checkBox.getState() == CHECKED;
			if (checked) {
				selectedItems.addAll(allItems);
			} else {
				selectedItems.clear();
			}
			onSelectedItemsChanged();
			updateItemsSelection(checked);
		});
	}

	@Override
	protected boolean shouldShowDivider() {
		return true;
	}
	@Override
	protected int getItemLayoutId() {
		return R.layout.gpx_track_select_item;
	}

	@Override
	protected void updateItemView(SelectableItem item, View view) {
		boolean checked = selectedItems.contains(item);
		ImageView imageView = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.name);
		TextView distance = view.findViewById(R.id.distance);
		TextView pointsCount = view.findViewById(R.id.points_count);
		TextView time = view.findViewById(R.id.time);
		final CheckBox checkBox = view.findViewById(R.id.compound_button);
		AndroidUiHelper.setVisibility(View.VISIBLE, imageView, title, distance, pointsCount, time, checkBox);
		checkBox.setChecked(checked);
		CompoundButtonCompat.setButtonTintList(checkBox, AndroidUtils.createCheckedColorStateList(app, secondaryColorRes, activeColorRes));

		view.setOnClickListener(v -> {
			boolean isSelected = !checkBox.isChecked();
			checkBox.setChecked(isSelected);
			if (isSelected) {
				selectedItems.add(item);
			} else {
				selectedItems.remove(item);
			}
			onSelectedItemsChanged();
		});
		title.setText(item.getTitle());

		GpxInfo info = (GpxInfo) item.getObject();
		GPXTrackAnalysis analysis = getGpxTrackAnalysis(info, app, null);
		distance.setText(OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app));
		pointsCount.setText(String.valueOf(analysis.wptPoints));
		time.setText(formatDuration((int) (analysis.timeSpan / 1000), app.accessibilityEnabled()));
		imageView.setImageDrawable(uiUtilities.getIcon(item.getIconId(), activeColorRes));
	}

	@Override
	protected void notifyUiCreated() {
		onSelectedItemsChanged();
		super.notifyUiCreated();
	}

	public void onSelectedItemsChanged() {
		updateSelectAllButton();
		updateSelectedSizeView();
		updateApplyButtonEnable();
		if (selectionUpdateListener != null) {
			selectionUpdateListener.onSelectionUpdate();
		}
	}

	private void updateSelectAllButton() {
		String checkBoxTitle;
		if (Algorithms.isEmpty(selectedItems)) {
			checkBox.setState(UNCHECKED);
			checkBoxTitle = getString(R.string.shared_string_select_all);
		} else {
			checkBox.setState(selectedItems.containsAll(allItems) ? CHECKED : MISC);
			checkBoxTitle = getString(R.string.shared_string_deselect_all);
		}
		int checkBoxColor = checkBox.getState() == UNCHECKED ? secondaryColorRes : activeColorRes;
		CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(ContextCompat.getColor(app, checkBoxColor)));
		this.checkBoxTitle.setText(checkBoxTitle);
	}

	private void updateSelectedSizeView() {
		String selected = String.valueOf(selectedItems.size());
		String all = String.valueOf(allItems.size());
		selectedSize.setText(getString(R.string.ltr_or_rtl_combine_via_slash, selected, all));
	}

	private void updateApplyButtonEnable() {
		boolean noEmptySelection = !Algorithms.isEmpty(selectedItems);
		rightButton.setEnabled(noEmptySelection);
	}

	private void updateItemsSelection(boolean checked) {
		for (SelectableItem item : allItems) {
			View v = listViews.get(item);
			CheckBox checkBox = v != null ? (CheckBox) v.findViewById(R.id.compound_button) : null;
			if (checkBox != null) {
				checkBox.setChecked(checked);
			}
		}
	}

	public void setSelectedItems(List<SelectableItem> selected) {
		selectedItems.clear();
		if (!Algorithms.isEmpty(selected)) {
			selectedItems.addAll(selected);
		}
	}

	@NonNull
	@Override
	public List<SelectableItem> getSelectedItems() {
		return selectedItems;
	}

	public void setSelectionUpdateListener(SelectionUpdateListener selectionUpdateListener) {
		this.selectionUpdateListener = selectionUpdateListener;
	}

	public static ExportGPXMultipleSelectionBottomSheet showInstance(@NonNull AppCompatActivity activity,
	                                                                 @NonNull List<SelectableItem> items,
	                                                                 @Nullable List<SelectableItem> selected) {
		ExportGPXMultipleSelectionBottomSheet fragment = new ExportGPXMultipleSelectionBottomSheet();
		fragment.setItems(items);
		fragment.setSelectedItems(selected);
		FragmentManager fm = activity.getSupportFragmentManager();
		fragment.show(fm, TAG);
		return fragment;
	}

	public interface SelectionUpdateListener {
		void onSelectionUpdate();
	}
}