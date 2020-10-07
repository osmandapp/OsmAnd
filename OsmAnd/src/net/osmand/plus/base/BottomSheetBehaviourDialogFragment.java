package net.osmand.plus.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.util.ArrayList;
import java.util.List;

public abstract class BottomSheetBehaviourDialogFragment extends BottomSheetDialogFragment {

	private static final String USED_ON_MAP_KEY = "used_on_map";
	private static final int DEFAULT_VALUE = -1;

	protected List<BaseBottomSheetItem> items = new ArrayList<>();

	protected boolean usedOnMap = true;
	protected boolean nightMode;
	protected boolean portrait;

	protected View dismissButton;
	protected View rightButton;

	private LinearLayout itemsContainer;

	public void setUsedOnMap(boolean usedOnMap) {
		this.usedOnMap = usedOnMap;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			usedOnMap = savedInstanceState.getBoolean(USED_ON_MAP_KEY);
		}
		nightMode = isNightMode(requiredMyApplication());
		portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View mainView = themedInflater.inflate(R.layout.bottom_sheet_behaviour_base, parent, false);
		itemsContainer = (LinearLayout) mainView.findViewById(R.id.items_container);

		View scrollView = mainView.findViewById(R.id.bottom_sheet_scroll_view);
		final BottomSheetBehavior behavior = BottomSheetBehavior.from(scrollView);
		behavior.setPeekHeight(getPeekHeight());

		LinearLayout buttonsContainer = (LinearLayout) mainView.findViewById(R.id.buttons_container);
		buttonsContainer.setBackgroundResource(getButtonsContainerBg());

		if (!portrait) {
			Dialog dialog = getDialog();
			if (dialog != null) {
				dialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
					}
				});
			}
		}

		createMenuItems(savedInstanceState);
		inflateMenuItems();

		dismissButton = mainView.findViewById(R.id.dismiss_button);
		UiUtilities.setupDialogButton(nightMode, dismissButton, getDismissButtonType(), getDismissButtonTextId());
		dismissButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onDismissButtonClickAction();
				dismiss();
			}
		});
		if (hideButtonsContainer()) {
			mainView.findViewById(R.id.buttons_container).setVisibility(View.GONE);
		} else {
			int rightBottomButtonTextId = getRightBottomButtonTextId();
			if (rightBottomButtonTextId != DEFAULT_VALUE) {
				mainView.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);
				rightButton = mainView.findViewById(R.id.right_bottom_button);
				UiUtilities.setupDialogButton(nightMode, rightButton, getRightBottomButtonType(), rightBottomButtonTextId);
				rightButton.setVisibility(View.VISIBLE);
				rightButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onRightBottomButtonClick();
					}
				});
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
				params.width = activity.getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
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

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light);
	}

	protected Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, getActiveColorId());
	}

	@ColorRes
	protected int getActiveColorId() {
		return nightMode ? R.color.osmand_orange : R.color.color_myloc_distance;
	}

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		Context ctx = getContext();
		return ctx != null ? ContextCompat.getColor(ctx, colorId) : 0;
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

	@ColorRes
	protected int getBgColorId() {
		return nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light;
	}

	@DrawableRes
	protected int getPortraitBgResId() {
		return nightMode ? R.drawable.bg_bottom_menu_dark : R.drawable.bg_bottom_menu_light;
	}

	@DrawableRes
	protected int getLandscapeTopsidesBgResId() {
		return nightMode ? R.drawable.bg_bottom_sheet_topsides_landscape_dark : R.drawable.bg_bottom_sheet_topsides_landscape_light;
	}

	@DrawableRes
	protected int getLandscapeSidesBgResId() {
		return nightMode ? R.drawable.bg_bottom_sheet_sides_landscape_dark : R.drawable.bg_bottom_sheet_sides_landscape_light;
	}

	private int getButtonsContainerBg() {
		if (portrait) {
			return getBgColorId();
		}
		return nightMode ? R.drawable.bottom_sheet_buttons_bg_dark : R.drawable.bottom_sheet_buttons_bg_light;
	}

	protected boolean isNightMode(@NonNull OsmandApplication app) {
		if (usedOnMap) {
			return app.getDaynightHelper().isNightModeForMapControls();
		}
		return !app.getSettings().isLightContent();
	}
}