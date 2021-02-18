package net.osmand.plus.base;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton.Builder;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.util.Algorithms;
import net.osmand.view.ThreeStateCheckbox;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.view.ThreeStateCheckbox.State.CHECKED;
import static net.osmand.view.ThreeStateCheckbox.State.MISC;
import static net.osmand.view.ThreeStateCheckbox.State.UNCHECKED;

public class SelectMultipleItemsBottomSheet extends MenuBottomSheetDialogFragment {

	private OsmandApplication app;
	private UiUtilities uiUtilities;

	private TextView title;
	private TextView description;
	private TextView applyButtonTitle;
	private TextView checkBoxTitle;
	private TextView selectedSize;
	private ThreeStateCheckbox checkBox;

	private int activeColorRes;
	private int secondaryColorRes;

	private final List<SelectableItem> allItems = new ArrayList<>();
	private final List<SelectableItem> selectedItems = new ArrayList<>();
	private SelectionUpdateListener selectionUpdateListener;
	private OnApplySelectionListener onApplySelectionListener;

	public static final String TAG = SelectMultipleItemsBottomSheet.class.getSimpleName();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View mainView = super.onCreateView(inflater, parent, savedInstanceState);
		onSelectedItemsChanged();
		return mainView;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		uiUtilities = app.getUIUtilities();
		activeColorRes = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
		secondaryColorRes = nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;

		items.add(createTitleItem());
		items.add(new SimpleDividerItem(app));
		createListItems();
	}

	private BaseBottomSheetItem createTitleItem() {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.settings_group_title, null);

		checkBox = view.findViewById(R.id.check_box);
		checkBoxTitle = view.findViewById(R.id.check_box_title);
		description = view.findViewById(R.id.description);
		selectedSize = view.findViewById(R.id.selected_size);
		title = view.findViewById(R.id.title);
		View selectAllButton = view.findViewById(R.id.select_all_button);
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
		return new SimpleBottomSheetItem.Builder().setCustomView(view).create();
	}

	private void createListItems() {
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
	protected void setupRightButton() {
		super.setupRightButton();
		applyButtonTitle = rightButton.findViewById(R.id.button_text);
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (onApplySelectionListener != null) {
			onApplySelectionListener.onSelectionApplied(selectedItems);
		}
		dismiss();
	}

	private void onSelectedItemsChanged() {
		updateSelectAllButton();
		updateSelectedSizeView();
		updateApplyButtonEnable();
		if (selectionUpdateListener != null) {
			selectionUpdateListener.onSelectionUpdate();
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	private void setupListItem(Builder builder, SelectableItem item) {
		builder.setTitle(item.title);
		builder.setDescription(item.description);
		builder.setIcon(uiUtilities.getIcon(item.iconId, activeColorRes));
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

	public void setTitle(@NonNull String title) {
		this.title.setText(title);
	}

	public void setDescription(@NonNull String description) {
		this.description.setText(description);
	}

	public void setConfirmButtonTitle(@NonNull String confirmButtonTitle) {
		applyButtonTitle.setText(confirmButtonTitle);
	}

	private void setItems(List<SelectableItem> allItems) {
		if (!Algorithms.isEmpty(allItems)) {
			this.allItems.addAll(allItems);
		}
	}

	private void setSelectedItems(List<SelectableItem> selected) {
		if (!Algorithms.isEmpty(selected)) {
			this.selectedItems.addAll(selected);
		}
	}

	public List<SelectableItem> getSelectedItems() {
		return selectedItems;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (requireActivity().isChangingConfigurations()) {
			dismiss();
		}
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

	public void setSelectionUpdateListener(SelectionUpdateListener selectionUpdateListener) {
		this.selectionUpdateListener = selectionUpdateListener;
	}

	public void setOnApplySelectionListener(OnApplySelectionListener onApplySelectionListener) {
		this.onApplySelectionListener = onApplySelectionListener;
	}

	public interface SelectionUpdateListener {
		void onSelectionUpdate();
	}

	public interface OnApplySelectionListener {
		void onSelectionApplied(List<SelectableItem> selectedItems);
	}

	public static class SelectableItem {
		private String title;
		private String description;
		private int iconId;
		private Object object;

		public void setTitle(String title) {
			this.title = title;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public void setIconId(int iconId) {
			this.iconId = iconId;
		}

		public void setObject(Object object) {
			this.object = object;
		}

		public Object getObject() {
			return object;
		}
	}

}
