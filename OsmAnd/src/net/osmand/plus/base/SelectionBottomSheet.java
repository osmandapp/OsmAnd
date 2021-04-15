package net.osmand.plus.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.plus.widgets.MultiStateToggleButton;
import net.osmand.view.ThreeStateCheckbox;

import java.util.List;

public abstract class SelectionBottomSheet extends MenuBottomSheetDialogFragment {

	protected OsmandApplication app;
	protected UiUtilities uiUtilities;

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

	private OnUiInitializedListener uiInitializedListener;
	private OnApplySelectionListener applySelectionListener;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View mainView = super.onCreateView(inflater, parent, savedInstanceState);
		notifyUiInitialized();
		return mainView;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		uiUtilities = app.getUIUtilities();
		activeColorRes = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
		secondaryColorRes = nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;

		items.add(createHeaderUi());
		if (shouldShowDivider()) {
			items.add(new SimpleDividerItem(app));
		}
		createSelectionUi();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (requireActivity().isChangingConfigurations()) {
			dismiss();
		}
	}

	private BaseBottomSheetItem createHeaderUi() {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.settings_group_title, null);

		title = view.findViewById(R.id.title);
		titleDescription = view.findViewById(R.id.title_description);
		primaryDescription = view.findViewById(R.id.primary_description);
		secondaryDescription = view.findViewById(R.id.secondary_description);
		selectedSize = view.findViewById(R.id.selected_size);
		toggleContainer = view.findViewById(R.id.custom_radio_buttons);
		radioGroup = new MultiStateToggleButton(app, toggleContainer, nightMode);
		selectAllButton = view.findViewById(R.id.select_all_button);
		checkBoxTitle = view.findViewById(R.id.check_box_title);
		checkBox = view.findViewById(R.id.check_box);

		initHeaderUi();
		return new SimpleBottomSheetItem.Builder().setCustomView(view).create();
	}

	protected abstract void createSelectionUi();

	protected abstract void initHeaderUi();

	protected boolean shouldShowDivider() {
		return true;
	}

	@Override
	protected void setupRightButton() {
		super.setupRightButton();
		applyButtonTitle = rightButton.findViewById(R.id.button_text);
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

	@Override
	protected void onRightBottomButtonClick() {
		if (applySelectionListener != null) {
			applySelectionListener.onSelectionApplied(getSelection());
		}
		dismiss();
	}

	@NonNull
	public abstract List<SelectableItem> getSelection();

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	public void setUiInitializedListener(OnUiInitializedListener uiInitializedListener) {
		this.uiInitializedListener = uiInitializedListener;
	}

	public void setOnApplySelectionListener(OnApplySelectionListener onApplySelectionListener) {
		this.applySelectionListener = onApplySelectionListener;
	}

	protected void notifyUiInitialized() {
		if (uiInitializedListener != null) {
			uiInitializedListener.onUiInitialized();
		}
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	public interface OnUiInitializedListener {
		void onUiInitialized();
	}

	public interface OnApplySelectionListener {
		void onSelectionApplied(List<SelectableItem> selectedItems);
	}

	public static class SelectableItem {
		private String title;
		private String description;
		private int iconId;
		private Object object;

		public String getTitle() {
			return title;
		}

		public String getDescription() {
			return description;
		}

		public int getIconId() {
			return iconId;
		}

		public Object getObject() {
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

		public void setObject(Object object) {
			this.object = object;
		}
	}

}
