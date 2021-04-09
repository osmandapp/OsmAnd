package net.osmand.plus.base;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.plus.download.MultipleIndexesUiHelper.SelectedItemsListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.MultiStateToggleButton;
import net.osmand.plus.widgets.MultiStateToggleButton.OnRadioItemClickListener;
import net.osmand.plus.widgets.MultiStateToggleButton.RadioItem;
import net.osmand.util.Algorithms;
import net.osmand.view.ThreeStateCheckbox;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.view.ThreeStateCheckbox.State.CHECKED;
import static net.osmand.view.ThreeStateCheckbox.State.MISC;
import static net.osmand.view.ThreeStateCheckbox.State.UNCHECKED;

public class SelectMultipleItemsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectMultipleItemsBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private UiUtilities uiUtilities;

	private TextView title;
	private TextView description;
	private TextView applyButtonTitle;
	private TextView checkBoxTitle;
	private TextView selectedSize;
	private ThreeStateCheckbox checkBox;

	private int sizeAboveList = 0;
	private int activeColorRes;
	private int secondaryColorRes;
	private String addDescriptionText;
	private String leftRadioButtonText;
	private String rightRadioButtonText;
	private boolean customOptionsVisible;
	private boolean leftButtonSelected;

	private final List<SelectableItem> allItems = new ArrayList<>();
	private final List<SelectableItem> selectedItems = new ArrayList<>();
	private SelectionUpdateListener selectionUpdateListener;
	private OnApplySelectionListener onApplySelectionListener;
	private OnRadioButtonSelectListener onRadioButtonSelectListener;
	private SelectedItemsListener selectedItemsListener;

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
		sizeAboveList = items.size();
		createListItems();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (requireActivity().isChangingConfigurations()) {
			dismiss();
		}
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
		TextView addDescription = view.findViewById(R.id.additional_description);
		LinearLayout customRadioButtons = view.findViewById(R.id.custom_radio_buttons);

		if (!isMultipleItem()) {
			AndroidUiHelper.setVisibility(View.GONE, description, selectedSize, selectAllButton);
		} else {
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

		if (!Algorithms.isEmpty(addDescriptionText)) {
			addDescription.setText(addDescriptionText);
			AndroidUiHelper.setVisibility(View.VISIBLE, addDescription);
		}

		if (customOptionsVisible) {
			AndroidUiHelper.setVisibility(View.VISIBLE, customRadioButtons);
			RadioItem leftRadioButton = new RadioItem(leftRadioButtonText);
			RadioItem rightRadioButton = new RadioItem(rightRadioButtonText);
			MultiStateToggleButton toggleButtons =
					new MultiStateToggleButton(app, customRadioButtons, nightMode);
			toggleButtons.setItems(leftRadioButton, rightRadioButton);
			toggleButtons.updateView(true);
			leftRadioButton.setOnClickListener(new OnRadioItemClickListener() {
				@Override
				public boolean onRadioItemClick(RadioItem radioItem, View view) {
					onRadioButtonSelectListener.onSelect(leftButtonSelected = true);
					updateSelectedSizeView();
					updateSelectAllButton();
					updateApplyButtonEnable();
					return true;
				}
			});
			rightRadioButton.setOnClickListener(new OnRadioItemClickListener() {
				@Override
				public boolean onRadioItemClick(RadioItem radioItem, View view) {
					onRadioButtonSelectListener.onSelect(leftButtonSelected = false);
					updateSelectedSizeView();
					updateSelectAllButton();
					updateApplyButtonEnable();
					return true;
				}
			});
			toggleButtons.setSelectedItem(leftButtonSelected ? leftRadioButton : rightRadioButton);
		}

		return new SimpleBottomSheetItem.Builder().setCustomView(view).create();
	}

	private void createListItems() {
		if (isMultipleItem()) {
			for (int i = 0; i < allItems.size(); i++) {
				final SelectableItem item = allItems.get(i);
				boolean checked = selectedItems.contains(item);
				final int finalI = i;
				items.add(new Builder()
						.setChecked(checked)
						.setButtonTintList(AndroidUtils.createCheckedColorStateList(app, secondaryColorRes, activeColorRes))
						.setDescription(item.description)
						.setIcon(uiUtilities.getIcon(item.iconId, activeColorRes))
						.setTitle(item.title)
						.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp)
						.setTag(item)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								BottomSheetItemWithCompoundButton item = (BottomSheetItemWithCompoundButton) items.get(finalI + sizeAboveList);
								boolean checked = item.isChecked();
								item.setChecked(!checked);
								SelectableItem tag = (SelectableItem) item.getTag();
								if (!checked) {
									selectedItems.add(tag);
								} else {
									selectedItems.remove(tag);
								}
								onSelectedItemsChanged();
							}
						})
						.create());
			}
		} else if (allItems.size() == 1) {
			final SelectableItem item = allItems.get(0);
			items.add(new Builder()
					.setDescription(item.description)
					.setDescriptionColorId(AndroidUtils.getSecondaryTextColorId(nightMode))
					.setIcon(uiUtilities.getIcon(item.iconId, activeColorRes))
					.setTitle(item.title)
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
					.setTag(item)
					.create());
		}
	}

	private void onSelectedItemsChanged() {
		updateSelectAllButton();
		updateSelectedSizeView();
		updateApplyButtonEnable();
		if (selectionUpdateListener != null) {
			selectionUpdateListener.onSelectionUpdate();
		}
	}

	private void updateSelectAllButton() {
		if (isMultipleItem()) {
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
	}

	private void updateSelectedSizeView() {
		if (isMultipleItem()) {
			String selected = String.valueOf(selectedItems.size());
			String all = String.valueOf(allItems.size());
			selectedSize.setText(getString(R.string.ltr_or_rtl_combine_via_slash, selected, all));
		}
	}

	private void updateApplyButtonEnable() {
		rightButton.setEnabled(!Algorithms.isEmpty(selectedItems));
	}

	private void updateItems(boolean checked) {
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				((BottomSheetItemWithCompoundButton) item).setChecked(checked);
			}
		}
	}

	public void setItems(List<SelectableItem> allItems) {
		this.allItems.clear();
		if (!Algorithms.isEmpty(allItems)) {
			this.allItems.addAll(allItems);
		}
	}

	private void setSelectedItems(List<SelectableItem> selected) {
		this.selectedItems.clear();
		if (!Algorithms.isEmpty(selected)) {
			/*List<SelectableItem> prevDownloadItems = new ArrayList<>(this.selectedItems);
			for (SelectableItem prevDownloadItem : selected) {
				Object object = prevDownloadItem.getObject();
				if (object instanceof IndexItem && ((IndexItem) object).isDownloaded()) {
					prevDownloadItems.add(prevDownloadItem);
				}
			}
			selected.removeAll(prevDownloadItems);*/
			this.selectedItems.addAll(selected);
		}
	}

	public void recreateList(List<SelectableItem> allItems) {
		setItems(allItems);
		if (selectedItemsListener != null) {
			setSelectedItems(selectedItemsListener.createSelectedItems(this.allItems, leftButtonSelected));
		}
		if (items.size() > sizeAboveList) {
			for (int i = 0; i < this.allItems.size(); i++) {
				SelectableItem item = this.allItems.get(i);
				BottomSheetItemWithDescription button = (BottomSheetItemWithDescription) items.get(i + sizeAboveList);
				button.setDescription(item.description);
				button.setTitle(item.title);
				button.setTag(item);
				if (isMultipleItem()) {
					((BottomSheetItemWithCompoundButton) button).setChecked(selectedItems.contains(item));
				}
			}
		}
	}

	public boolean isMultipleItem() {
		return allItems.size() > 1;
	}

	public List<SelectableItem> getSelectedItems() {
		return selectedItems;
	}

	public void setConfirmButtonTitle(@NonNull String confirmButtonTitle) {
		applyButtonTitle.setText(confirmButtonTitle);
	}

	public void setTitle(@NonNull String title) {
		this.title.setText(title);
	}

	public void setDescription(@NonNull String description) {
		this.description.setText(description);
	}

	public void setAddDescriptionText(String addDescriptionText) {
		this.addDescriptionText = addDescriptionText;
	}

	public void setLeftRadioButtonText(String leftRadioButtonText) {
		this.leftRadioButtonText = leftRadioButtonText;
	}

	public void setRightRadioButtonText(String rightRadioButtonText) {
		this.rightRadioButtonText = rightRadioButtonText;
	}

	public void setCustomOptionsVisible(boolean customOptionsVisible) {
		this.customOptionsVisible = customOptionsVisible;
	}

	public void setLeftButtonSelected(boolean leftButtonSelected) {
		this.leftButtonSelected = leftButtonSelected;
	}

	public void setSelectionUpdateListener(SelectionUpdateListener selectionUpdateListener) {
		this.selectionUpdateListener = selectionUpdateListener;
	}

	public void setOnApplySelectionListener(OnApplySelectionListener onApplySelectionListener) {
		this.onApplySelectionListener = onApplySelectionListener;
	}

	public void setOnRadioButtonSelectListener(OnRadioButtonSelectListener onRadioButtonSelectListener) {
		this.onRadioButtonSelectListener = onRadioButtonSelectListener;
	}

	public void setSelectedItemsListener(SelectedItemsListener selectedItemsListener) {
		this.selectedItemsListener = selectedItemsListener;
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

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
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

	public static SelectMultipleItemsBottomSheet showInstance(@NonNull AppCompatActivity activity,
	                                                          @NonNull List<SelectableItem> items,
	                                                          @Nullable List<SelectableItem> selected,
	                                                          boolean usedOnMap,
	                                                          String addDescription,
	                                                          boolean customOptionsVisible,
	                                                          boolean leftButtonSelected,
	                                                          String leftRadioButtonText,
	                                                          String rightRadioButtonText) {
		SelectMultipleItemsBottomSheet fragment = new SelectMultipleItemsBottomSheet();
		fragment.setUsedOnMap(usedOnMap);
		fragment.setItems(items);
		fragment.setSelectedItems(selected);
		fragment.setAddDescriptionText(addDescription);
		fragment.setCustomOptionsVisible(customOptionsVisible);
		fragment.setLeftButtonSelected(leftButtonSelected);
		fragment.setLeftRadioButtonText(leftRadioButtonText);
		fragment.setRightRadioButtonText(rightRadioButtonText);
		FragmentManager fm = activity.getSupportFragmentManager();
		fragment.show(fm, TAG);
		return fragment;
	}

	public interface SelectionUpdateListener {
		void onSelectionUpdate();
	}

	public interface OnApplySelectionListener {
		void onSelectionApplied(List<SelectableItem> selectedItems);
	}

	public interface OnRadioButtonSelectListener {
		void onSelect(boolean leftButton);
	}

	public static class SelectableItem {
		private String title;
		private String description;
		private int iconId;
		private Object object;

		public void setTitle(String title) {
			this.title = title;
		}

		public String getDescription() {
			return description;
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
