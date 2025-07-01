package net.osmand.plus.base;

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

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MultipleSelectionBottomSheet<T> extends SelectionBottomSheet<T> {

	public static final String TAG = MultipleSelectionBottomSheet.class.getSimpleName();

	protected final List<SelectableItem<T>> selectedItems = new ArrayList<>();
	protected SelectionUpdateListener selectionUpdateListener;

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
	protected void updateItemView(SelectableItem<T> item, View view) {
		boolean checked = selectedItems.contains(item);
		ImageView imageView = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);
		CheckBox checkBox = view.findViewById(R.id.compound_button);
		AndroidUiHelper.setVisibility(View.VISIBLE, imageView, title, description, checkBox);

		checkBox.setChecked(checked);
		CompoundButtonCompat.setButtonTintList(checkBox, AndroidUtils.createCheckedColorStateList(app, secondaryColorRes, activeColorRes));

		view.setOnClickListener(v -> {
			boolean selected = !checkBox.isChecked();
			checkBox.setChecked(selected);
			if (selected) {
				selectedItems.add(item);
			} else {
				selectedItems.remove(item);
			}
			onSelectedItemsChanged();
		});
		title.setText(item.getTitle());
		if (description != null) {
			description.setText(item.getDescription());
		}
		imageView.setImageDrawable(getIcon(item.getIconId(), activeColorRes));
	}

	@Override
	protected int getItemLayoutId() {
		return R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp;
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
		for (SelectableItem<T> item : allItems) {
			View v = listViews.get(item);
			CheckBox checkBox = v != null ? (CheckBox) v.findViewById(R.id.compound_button) : null;
			if (checkBox != null) {
				checkBox.setChecked(checked);
			}
		}
	}

	public void setSelectedItems(List<SelectableItem<T>> selected) {
		selectedItems.clear();
		if (!Algorithms.isEmpty(selected)) {
			selectedItems.addAll(selected);
		}
	}

	@NonNull
	@Override
	public List<SelectableItem<T>> getSelectedItems() {
		return selectedItems;
	}

	public void setSelectionUpdateListener(SelectionUpdateListener selectionUpdateListener) {
		this.selectionUpdateListener = selectionUpdateListener;
	}

	public static <T> MultipleSelectionBottomSheet<T> showInstance(@NonNull AppCompatActivity activity,
	                                                               @NonNull List<SelectableItem<T>> items,
	                                                               @Nullable List<SelectableItem<T>> selected,
	                                                               boolean usedOnMap) {
		MultipleSelectionBottomSheet<T> fragment = new MultipleSelectionBottomSheet<>();
		fragment.setUsedOnMap(usedOnMap);
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