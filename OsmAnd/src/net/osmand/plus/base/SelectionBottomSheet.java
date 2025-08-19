package net.osmand.plus.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.multistatetoggle.MultiStateToggleButton;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.util.Algorithms;
import net.osmand.view.ThreeStateCheckbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SelectionBottomSheet<T> extends MenuBottomSheetDialogFragment {

	protected TextView title;
	protected TextView titleDescription;
	protected TextView primaryDescription;
	protected TextView secondaryDescription;
	protected TextView selectedSize;
	protected LinearLayout toggleContainer;
	protected MultiStateToggleButton radioGroup;
	protected View selectAllButton;
	protected TextView checkBoxTitle;
	protected ThreeStateCheckbox checkBox;
	protected LinearLayout listContainer;
	protected TextView applyButtonTitle;

	protected int activeColorRes;
	protected int secondaryColorRes;

	private DialogStateListener dialogStateListener;
	private OnApplySelectionListener<T> onApplySelectionListener;

	protected List<SelectableItem<T>> allItems = new ArrayList<>();
	protected Map<SelectableItem<T>, View> listViews = new HashMap<>();
	private List<RadioItem> modes;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View mainView = super.onCreateView(inflater, parent, savedInstanceState);
		createSelectionListIfPossible();
		notifyUiCreated();
		return mainView;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		activeColorRes = ColorUtilities.getActiveIconColorId(nightMode);
		secondaryColorRes = ColorUtilities.getSecondaryIconColorId(nightMode);

		items.add(createHeaderView());
		if (shouldShowDivider()) {
			items.add(new SimpleDividerItem(app));
		}
		items.add(createSelectionView());
	}

	@NonNull
	private BaseBottomSheetItem createHeaderView() {
		View view = inflate(R.layout.settings_group_title);

		title = view.findViewById(R.id.title);
		titleDescription = view.findViewById(R.id.title_description);
		primaryDescription = view.findViewById(R.id.primary_description);
		secondaryDescription = view.findViewById(R.id.secondary_description);
		selectedSize = view.findViewById(R.id.selected_size);
		toggleContainer = view.findViewById(R.id.custom_radio_buttons);
		radioGroup = new TextToggleButton(app, toggleContainer, nightMode);
		selectAllButton = view.findViewById(R.id.select_all_button);
		checkBoxTitle = view.findViewById(R.id.check_box_title);
		checkBox = view.findViewById(R.id.check_box);

		if (modes != null) {
			radioGroup.setItems(modes);
		}

		return new SimpleBottomSheetItem.Builder().setCustomView(view).create();
	}

	@NonNull
	private BaseBottomSheetItem createSelectionView() {
		listContainer = new LinearLayout(getThemedContext());
		listContainer.setLayoutParams(new LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		listContainer.setOrientation(LinearLayout.VERTICAL);
		return new SimpleBottomSheetItem.Builder().setCustomView(listContainer).create();
	}

	public void setTitle(@NonNull String title) {
		this.title.setText(title);
	}

	public void setTitleDescription(@NonNull String description) {
		titleDescription.setText(description);
	}

	public void setPrimaryDescription(@NonNull String description) {
		primaryDescription.setText(description);
	}

	public void setSecondaryDescription(@NonNull String description) {
		secondaryDescription.setText(description);
	}

	public void setApplyButtonTitle(@NonNull String title) {
		applyButtonTitle.setText(title);
	}

	public void setModes(@NonNull List<RadioItem> modes) {
		this.modes = modes;
		if (radioGroup != null) {
			radioGroup.setItems(modes);
		}
	}

	public void setSelectedMode(@NonNull RadioItem mode) {
		radioGroup.setSelectedItem(mode);
	}

	public void setItems(List<SelectableItem<T>> allItems) {
		this.allItems.clear();
		if (!Algorithms.isEmpty(allItems)) {
			this.allItems.addAll(allItems);
			createSelectionListIfPossible();
		}
	}

	public void setCustomView(@Nullable View view) {
		if (listContainer != null) {
			allItems.clear();
			listViews.clear();
			listContainer.removeAllViews();
			if (view != null) {
				listContainer.addView(view);
			}
		}
	}

	public void setDialogStateListener(DialogStateListener dialogStateListener) {
		this.dialogStateListener = dialogStateListener;
	}

	public void setOnApplySelectionListener(OnApplySelectionListener<T> onApplySelectionListener) {
		this.onApplySelectionListener = onApplySelectionListener;
	}

	private void createSelectionListIfPossible() {
		if (listContainer != null && allItems != null) {
			recreateList();
		}
	}

	private void recreateList() {
		listViews.clear();
		listContainer.removeAllViews();
		for (SelectableItem<T> item : allItems) {
			setupItemView(item, inflate(getItemLayoutId()));
		}
	}

	private void setupItemView(SelectableItem<T> item, View view) {
		updateItemView(item, view);
		listViews.put(item, view);
		listContainer.addView(view);
	}

	public List<SelectableItem<T>> getAllItems() {
		return allItems;
	}

	@NonNull
	public abstract List<SelectableItem<T>> getSelectedItems();

	protected abstract void updateItemView(SelectableItem<T> item, View view);

	protected abstract int getItemLayoutId();

	protected abstract boolean shouldShowDivider();

	protected void notifyUiCreated() {
		if (dialogStateListener != null) {
			dialogStateListener.onDialogCreated();
		}
	}

	protected void showElements(View... views) {
		AndroidUiHelper.setVisibility(View.VISIBLE, views);
	}

	protected void hideElements(View... views) {
		AndroidUiHelper.setVisibility(View.GONE, views);
	}

	@Override
	protected void setupRightButton() {
		super.setupRightButton();
		applyButtonTitle = rightButton.findViewById(R.id.button_text);
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (onApplySelectionListener != null) {
			onApplySelectionListener.onSelectionApplied(getSelectedItems());
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

	@Override
	public void onPause() {
		super.onPause();
		if (requireActivity().isChangingConfigurations()) {
			dismiss();
		}
	}

	@Override
	public void onDestroy() {
		if (dialogStateListener != null) {
			dialogStateListener.onCloseDialog();
		}
		super.onDestroy();
	}

	public interface DialogStateListener {
		default void onDialogCreated() {

		}

		default void onCloseDialog() {

		}
	}

	public interface OnApplySelectionListener<T> {
		void onSelectionApplied(List<SelectableItem<T>> selectedItems);
	}

	public static class SelectableItem<T> {
		private String title;
		private String description;
		private int iconId;
		private T object;
		@ColorInt
		private int color;

		public String getTitle() {
			return title;
		}

		public String getDescription() {
			return description;
		}

		public int getIconId() {
			return iconId;
		}

		public T getObject() {
			return object;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public void setIconId(int iconId) {
			this.iconId = iconId;
		}

		public void setObject(T object) {
			this.object = object;
		}

		@ColorInt
		public int getColor() {
			return color;
		}

		public void setColor(@ColorInt int color) {
			this.color = color;
		}
	}
}