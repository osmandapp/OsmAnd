package net.osmand.plus.base;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.util.ArrayList;
import java.util.List;

public abstract class BottomSheetBehaviourDialogFragment extends BottomSheetDialogFragment {

	private static final String USED_ON_MAP_KEY = "used_on_map";
	private static final int DEFAULT_VALUE = -1;

	protected List<BaseBottomSheetItem> items = new ArrayList<>();

	protected boolean usedOnMap = true;
	protected boolean portrait;

	protected DialogButton dismissButton;
	protected DialogButton rightButton;

	private LinearLayout itemsContainer;

	public void setUsedOnMap(boolean usedOnMap) {
		this.usedOnMap = usedOnMap;
	}

	@Override
	public boolean isUsedOnMap() {
		return usedOnMap;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			usedOnMap = savedInstanceState.getBoolean(USED_ON_MAP_KEY);
		}
		super.onCreate(savedInstanceState);
		portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		updateNightMode();
		View mainView = inflate(R.layout.bottom_sheet_behaviour_base, parent, false);
		itemsContainer = mainView.findViewById(R.id.items_container);

		View scrollView = mainView.findViewById(R.id.bottom_sheet_scroll_view);
		BottomSheetBehavior behavior = BottomSheetBehavior.from(scrollView);
		behavior.setPeekHeight(getPeekHeight());

		LinearLayout buttonsContainer = mainView.findViewById(R.id.bottom_buttons_container);
		buttonsContainer.setBackgroundResource(getButtonsContainerBg());

		if (!portrait) {
			Dialog dialog = getDialog();
			if (dialog != null) {
				dialog.setOnShowListener(d -> behavior.setState(BottomSheetBehavior.STATE_EXPANDED));
			}
		}

		createMenuItems(savedInstanceState);
		inflateMenuItems();

		dismissButton = mainView.findViewById(R.id.dismiss_button);
		dismissButton.setButtonType(getDismissButtonType());
		dismissButton.setTitleId(getDismissButtonTextId());
		dismissButton.setOnClickListener(v -> {
			onDismissButtonClickAction();
			dismiss();
		});
		if (hideButtonsContainer()) {
			mainView.findViewById(R.id.bottom_buttons_container).setVisibility(View.GONE);
		} else {
			int rightBottomButtonTextId = getRightBottomButtonTextId();
			if (rightBottomButtonTextId != DEFAULT_VALUE) {
				mainView.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);
				rightButton = mainView.findViewById(R.id.right_bottom_button);
				rightButton.setButtonType(getRightBottomButtonType());
				rightButton.setTitleId(rightBottomButtonTextId);
				rightButton.setVisibility(View.VISIBLE);
				rightButton.setOnClickListener(v -> onRightBottomButtonClick());
			}
		}
		updateBackground();
		updateBottomButtons();
		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		FragmentActivity activity = requireActivity();
		if (!AndroidUiHelper.isOrientationPortrait(activity)) {
			Dialog dialog = getDialog();
			Window window = dialog != null ? dialog.getWindow() : null;
			if (window != null) {
				WindowManager.LayoutParams params = window.getAttributes();
				params.width = getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
				window.setAttributes(params);
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(USED_ON_MAP_KEY, usedOnMap);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		items.clear();
		if (itemsContainer != null) {
			itemsContainer.removeAllViews();
		}
	}

	public abstract void createMenuItems(Bundle savedInstanceState);

	protected void inflateMenuItems() {
		Activity activity = requireActivity();
		for (BaseBottomSheetItem item : items) {
			item.inflate(activity, itemsContainer, nightMode);
		}
	}

	private void updateBackground() {
		if (portrait) {
			itemsContainer.setBackgroundResource(getPortraitBgResId());
		} else {
			itemsContainer.setBackgroundResource(getLandscapeTopsidesBgResId());
		}
	}

	protected int getPeekHeight() {
		return DEFAULT_VALUE;
	}

	protected boolean hideButtonsContainer() {
		return false;
	}

	@StringRes
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	protected DialogButtonType getDismissButtonType() {
		return DialogButtonType.SECONDARY;
	}

	protected void onDismissButtonClickAction() {

	}

	@StringRes
	protected int getRightBottomButtonTextId() {
		return DEFAULT_VALUE;
	}

	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.PRIMARY;
	}

	protected void onRightBottomButtonClick() {

	}

	protected boolean isDismissButtonEnabled() {
		return true;
	}

	protected boolean isRightBottomButtonEnabled() {
		return true;
	}

	protected void updateBottomButtons() {
		if (dismissButton != null) {
			boolean enabled = isDismissButtonEnabled();
			dismissButton.setEnabled(enabled);
			dismissButton.findViewById(R.id.button_text).setEnabled(enabled);
		}
		if (rightButton != null) {
			boolean enabled = isRightBottomButtonEnabled();
			rightButton.setEnabled(enabled);
			rightButton.findViewById(R.id.button_text).setEnabled(enabled);
		}
	}

	@DrawableRes
	protected int getPortraitBgResId() {
		return nightMode ? R.drawable.bg_bottom_menu_dark : R.drawable.bg_bottom_menu_light;
	}

	@DrawableRes
	protected int getLandscapeTopsidesBgResId() {
		return nightMode ? R.drawable.bg_bottom_sheet_topsides_landscape_dark : R.drawable.bg_bottom_sheet_topsides_landscape_light;
	}

	private int getButtonsContainerBg() {
		if (portrait) {
			return ColorUtilities.getListBgColorId(nightMode);
		}
		return nightMode ? R.drawable.bottom_sheet_buttons_bg_dark : R.drawable.bottom_sheet_buttons_bg_light;
	}
}