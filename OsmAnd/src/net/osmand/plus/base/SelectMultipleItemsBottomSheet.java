package net.osmand.plus.base;

import android.content.res.ColorStateList;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton.Builder;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.view.ThreeStateCheckbox.State.CHECKED;
import static net.osmand.view.ThreeStateCheckbox.State.MISC;
import static net.osmand.view.ThreeStateCheckbox.State.UNCHECKED;

public class SelectMultipleItemsBottomSheet extends SelectionBottomSheet {

	public static final String TAG = SelectMultipleItemsBottomSheet.class.getSimpleName();

	private final List<SelectableItem> allItems = new ArrayList<>();
	private final List<SelectableItem> selectedItems = new ArrayList<>();
	private SelectionUpdateListener selectionUpdateListener;

	@Override
	protected void initHeaderUi() {
		selectAllButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				checkBox.performClick();
				boolean checked = checkBox.getState() == CHECKED;
				if (checked) {
					selectedItems.addAll(allItems);
				} else {
					selectedItems.clear();
				}
				onSelectedItemsChanged();
				updateItems(checked);
			}
		});
	}

	@Override
	protected void createSelectionUi() {
		for (final SelectableItem item : allItems) {
			boolean checked = selectedItems.contains(item);
			final BottomSheetItemWithCompoundButton[] uiItem = new BottomSheetItemWithCompoundButton[1];
			final Builder builder = (BottomSheetItemWithCompoundButton.Builder) new Builder();
			builder.setChecked(checked)
					.setButtonTintList(AndroidUtils.createCheckedColorStateList(app, secondaryColorRes, activeColorRes))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							boolean checked = !uiItem[0].isChecked();
							uiItem[0].setChecked(checked);
							SelectableItem tag = (SelectableItem) uiItem[0].getTag();
							if (checked) {
								selectedItems.add(tag);
							} else {
								selectedItems.remove(tag);
							}
							onSelectedItemsChanged();
						}
					})
					.setTag(item);
			setupListItem(builder, item);
			uiItem[0] = builder.create();
			items.add(uiItem[0]);
		}
	}

	@Override
	protected void notifyUiInitialized() {
		onSelectedItemsChanged();
		super.notifyUiInitialized();
	}

	private void onSelectedItemsChanged() {
		updateSelectAllButton();
		updateSelectedSizeView();
		updateApplyButtonEnable();
		if (selectionUpdateListener != null) {
			selectionUpdateListener.onSelectionUpdate();
		}
	}

	private void setupListItem(Builder builder, SelectableItem item) {
		builder.setTitle(item.getTitle());
		builder.setDescription(item.getDescription());
		builder.setIcon(uiUtilities.getIcon(item.getIconId(), activeColorRes));
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
		if (Algorithms.isEmpty(selectedItems)) {
			rightButton.setEnabled(false);
		} else {
			rightButton.setEnabled(true);
		}
	}

	private void updateItems(boolean checked) {
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				((BottomSheetItemWithCompoundButton) item).setChecked(checked);
			}
		}
	}

	protected void setItems(List<SelectableItem> allItems) {
		if (!Algorithms.isEmpty(allItems)) {
			this.allItems.clear();
			this.allItems.addAll(allItems);
		}
	}

	protected void setSelectedItems(List<SelectableItem> selected) {
		if (!Algorithms.isEmpty(selected)) {
			selectedItems.clear();
			selectedItems.addAll(selected);
		}
	}

	@NonNull
	@Override
	public List<SelectableItem> getSelection() {
		return selectedItems;
	}

	public void setSelectionUpdateListener(SelectionUpdateListener selectionUpdateListener) {
		this.selectionUpdateListener = selectionUpdateListener;
	}

	public static SelectMultipleItemsBottomSheet showInstance(@NonNull AppCompatActivity activity,
	                                                          @NonNull List<SelectableItem> items,
	                                                          @Nullable List<SelectableItem> selected,
	                                                          boolean usedOnMap) {
		SelectMultipleItemsBottomSheet fragment = new SelectMultipleItemsBottomSheet();
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