package net.osmand.plus.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.util.ArrayList;
import java.util.List;

public abstract class MenuBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private static final String USED_ON_MAP_KEY = "used_on_map";
	protected static final int DEFAULT_VALUE = -1;

	protected List<BaseBottomSheetItem> items = new ArrayList<>();

	protected boolean usedOnMap = true;
	protected boolean nightMode;

	protected int themeRes;
	protected DialogButton dismissButton;
	protected DialogButton rightButton;
	protected DialogButton thirdButton;
	protected View buttonsShadow;
	protected LinearLayout itemsContainer;
	protected LinearLayout buttonsContainer;

	public void setUsedOnMap(boolean usedOnMap) {
		this.usedOnMap = usedOnMap;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			usedOnMap = savedInstanceState.getBoolean(USED_ON_MAP_KEY);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		Activity activity = requireActivity();
		nightMode = isNightMode(requiredMyApplication());
		themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		LayoutInflater themedInflater = UiUtilities.getInflater(activity, nightMode);
		View mainView = themedInflater.inflate(R.layout.bottom_sheet_menu_base, null);
		if (useScrollableItemsContainer()) {
			itemsContainer = mainView.findViewById(R.id.scrollable_items_container);
		} else {
			mainView.findViewById(R.id.scroll_view).setVisibility(View.GONE);
			itemsContainer = mainView.findViewById(R.id.non_scrollable_items_container);
			itemsContainer.setVisibility(View.VISIBLE);
		}
		buttonsShadow = mainView.findViewById(R.id.buttons_shadow);

		createMenuItems(savedInstanceState);

		inflateMenuItems();
		setupScrollShadow(mainView);
		setupBottomButtons((ViewGroup) mainView);
		setupHeightAndBackground(mainView);
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

	public void updateMenuItems() {
		View mainView = getView();
		if (mainView != null) {
			items.clear();
			itemsContainer.removeAllViews();
			createMenuItems(null);
			for (BaseBottomSheetItem item : items) {
				item.inflate(mainView.getContext(), itemsContainer, nightMode);
			}
			setupHeightAndBackground(mainView);
		}
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, ColorUtilities.getDefaultIconColorId(nightMode));
	}

	protected Drawable getPaintedContentIcon(@DrawableRes int id) {
		return getPaintedIcon(id, ColorUtilities.getDefaultIconColor(requiredMyApplication(), nightMode));
	}

	protected Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, getActiveColorId());
	}

	@ColorRes
	protected int getActiveColorId() {
		return getActiveColorId(nightMode);
	}

	@ColorRes
	protected static int getActiveColorId(boolean nightMode) {
		return nightMode ? R.color.osmand_orange : R.color.color_myloc_distance;
	}

	public int getDimen(@DimenRes int id) {
		return getResources().getDimensionPixelSize(id);
	}

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		Context ctx = getContext();
		return ctx != null ? ContextCompat.getColor(ctx, colorId) : 0;
	}

	protected void setupHeightAndBackground(View mainView) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		int screenHeight = AndroidUtils.getScreenHeight(activity);
		int statusBarHeight = AndroidUtils.getStatusBarHeight(activity);
		int contentHeight = getContentHeight(screenHeight - statusBarHeight);

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				ViewTreeObserver obs = mainView.getViewTreeObserver();
				obs.removeOnGlobalLayoutListener(this);

				View contentView = useScrollableItemsContainer() ? mainView.findViewById(R.id.scroll_view) : itemsContainer;
				if (contentView.getHeight() > contentHeight) {
					if (useScrollableItemsContainer() || useExpandableList()) {
						contentView.getLayoutParams().height = contentHeight;
						buttonsShadow.setVisibility(View.VISIBLE);
					} else {
						contentView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
					}
					contentView.requestLayout();
				}

				// 8dp is the shadow height
				boolean showTopShadow = screenHeight - statusBarHeight - mainView.getHeight() >= AndroidUtils.dpToPx(activity, 8);
				drawTopShadow(showTopShadow);
			}
		});
	}

	@NonNull
	protected OnGlobalLayoutListener getShadowLayoutListener() {
		return this::setShadowOnScrollableView;
	}

	protected void drawTopShadow(boolean showTopShadow) {
		Activity activity = getActivity();
		View mainView = getView();
		if (activity == null || mainView == null) {
			return;
		}
		if (AndroidUiHelper.isOrientationPortrait(activity)) {
			AndroidUtils.setBackground(mainView, showTopShadow ? getPortraitBg(activity) : getColoredBg(activity));
			if (!showTopShadow) {
				mainView.setPadding(0, 0, 0, 0);
			}
		} else {
			AndroidUtils.setBackground(mainView, showTopShadow ? getLandscapeTopsidesBg(activity) : getLandscapeSidesBg(activity));
		}
	}

	private int getContentHeight(int availableScreenHeight) {
		int customHeight = getCustomHeight();
		int buttonsHeight;
		if (useVerticalButtons()) {
			int padding = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
			int buttonHeight = getResources().getDimensionPixelSize(R.dimen.dialog_button_height);
			buttonsHeight = (buttonHeight + padding) * 2 + getFirstDividerHeight();
			if (getThirdBottomButtonTextId() != DEFAULT_VALUE) {
				buttonsHeight += buttonHeight + getSecondDividerHeight();
			}
		} else {
			buttonsHeight = getResources().getDimensionPixelSize(R.dimen.dialog_button_ex_height);
		}
		int maxHeight = availableScreenHeight - buttonsHeight;
		if (customHeight != DEFAULT_VALUE && customHeight <= maxHeight) {
			return customHeight;
		}
		return maxHeight;
	}

	protected int getCustomHeight() {
		return DEFAULT_VALUE;
	}

	protected boolean useScrollableItemsContainer() {
		return true;
	}

	protected boolean useExpandableList() {
		return false;
	}

	protected boolean hideButtonsContainer() {
		return false;
	}

	@ColorRes
	protected int getDividerColorId() {
		return DEFAULT_VALUE;
	}

	@StringRes
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	protected int getDismissButtonHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height_small);
	}

	protected int getRightButtonHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height_small);
	}

	protected int getThirdButtonHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height_small);
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

	protected int getThirdBottomButtonTextId() {
		return DEFAULT_VALUE;
	}

	protected DialogButtonType getThirdBottomButtonType() {
		return DialogButtonType.PRIMARY;
	}

	protected void onThirdBottomButtonClick() {

	}

	protected boolean isDismissButtonEnabled() {
		return true;
	}

	protected boolean isRightBottomButtonEnabled() {
		return true;
	}

	protected void setupBottomButtons(ViewGroup view) {
		Activity activity = requireActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(activity, nightMode);
		if (!hideButtonsContainer()) {
			if (useVerticalButtons()) {
				buttonsContainer = (LinearLayout) themedInflater.inflate(R.layout.bottom_buttons_vertical, view);
				setupThirdButton();
			} else {
				buttonsContainer = (LinearLayout) themedInflater.inflate(R.layout.bottom_buttons, view);
			}
			setupRightButton();
			setupDismissButton();
			updateBottomButtons();
		}
	}

	protected boolean useVerticalButtons() {
		Activity activity = requireActivity();
		int rightBottomButtonTextId = getRightBottomButtonTextId();
		if (getDismissButtonTextId() != DEFAULT_VALUE && rightBottomButtonTextId != DEFAULT_VALUE) {
			if (getThirdBottomButtonTextId() != DEFAULT_VALUE) {
				return true;
			}
			String rightButtonText = getString(rightBottomButtonTextId);
			boolean portrait = AndroidUiHelper.isOrientationPortrait(activity);
			int outerPadding = getResources().getDimensionPixelSize(R.dimen.content_padding);
			int innerPadding = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
			int dialogWidth = portrait ? AndroidUtils.getScreenWidth(activity) : getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			int availableTextWidth = (dialogWidth - (outerPadding * 3 + innerPadding * 4)) / 2;

			int measuredTextWidth = AndroidUtils.getTextWidth(getResources().getDimensionPixelSize(R.dimen.default_desc_text_size), rightButtonText);
			return measuredTextWidth > availableTextWidth;
		}
		return false;
	}

	protected void updateBottomButtons() {
		if (dismissButton != null) {
			boolean enabled = isDismissButtonEnabled();
			dismissButton.setEnabled(enabled);
		}
		if (rightButton != null) {
			boolean enabled = isRightBottomButtonEnabled();
			rightButton.setEnabled(enabled);
		}
	}

	private void setupDismissButton() {
		dismissButton = buttonsContainer.findViewById(R.id.dismiss_button);
		dismissButton.setButtonHeight(getDismissButtonHeight());
		int buttonTextId = getDismissButtonTextId();
		if (buttonTextId != DEFAULT_VALUE) {
			dismissButton.setTitleId(buttonTextId);
			dismissButton.setButtonType(getDismissButtonType());
			dismissButton.setOnClickListener(v -> {
				onDismissButtonClickAction();
				dismiss();
			});
		}
		AndroidUiHelper.updateVisibility(dismissButton, buttonTextId != DEFAULT_VALUE);
	}

	protected void setupRightButton() {
		rightButton = buttonsContainer.findViewById(R.id.right_bottom_button);
		rightButton.setButtonHeight(getRightButtonHeight());
		int buttonTextId = getRightBottomButtonTextId();
		if (buttonTextId != DEFAULT_VALUE) {
			rightButton.setTitleId(buttonTextId);
			rightButton.setButtonType(getRightBottomButtonType());
			rightButton.setOnClickListener(v -> onRightBottomButtonClick());
		}
		View divider = buttonsContainer.findViewById(R.id.buttons_divider);
		divider.getLayoutParams().height = getFirstDividerHeight();
		AndroidUiHelper.updateVisibility(rightButton, buttonTextId != DEFAULT_VALUE);
		AndroidUiHelper.updateVisibility(divider, buttonTextId != DEFAULT_VALUE);
	}

	protected int getFirstDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.content_padding);
	}

	protected void setupThirdButton() {
		thirdButton = buttonsContainer.findViewById(R.id.third_button);
		thirdButton.setButtonHeight(getThirdButtonHeight());
		int buttonTextId = getThirdBottomButtonTextId();
		if (buttonTextId != DEFAULT_VALUE) {
			thirdButton.setTitleId(buttonTextId);
			thirdButton.setButtonType(getThirdBottomButtonType());
			thirdButton.setOnClickListener(v -> onThirdBottomButtonClick());
		}
		View divider = buttonsContainer.findViewById(R.id.buttons_divider_top);
		divider.getLayoutParams().height = getSecondDividerHeight();
		AndroidUiHelper.updateVisibility(thirdButton, buttonTextId != DEFAULT_VALUE);
		AndroidUiHelper.updateVisibility(divider, buttonTextId != DEFAULT_VALUE);
	}

	protected int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.content_padding);
	}

	@ColorRes
	protected int getBgColorId() {
		return ColorUtilities.getListBgColorId(nightMode);
	}

	protected Drawable getColoredBg(@NonNull Context ctx) {
		int bgColor = ContextCompat.getColor(ctx, getBgColorId());
		return new ColorDrawable(bgColor);
	}

	protected Drawable getPortraitBg(@NonNull Context ctx) {
		return createBackgroundDrawable(ctx, R.drawable.bg_contextmenu_shadow_top_light);
	}

	protected Drawable getLandscapeTopsidesBg(@NonNull Context ctx) {
		return createBackgroundDrawable(ctx, R.drawable.bg_shadow_bottomsheet_topsides);
	}

	protected Drawable getLandscapeSidesBg(@NonNull Context ctx) {
		return createBackgroundDrawable(ctx, R.drawable.bg_shadow_bottomsheet_sides);
	}

	protected LayerDrawable createBackgroundDrawable(@NonNull Context ctx, @DrawableRes int shadowDrawableResId) {
		Drawable shadowDrawable = ContextCompat.getDrawable(ctx, shadowDrawableResId);
		Drawable[] layers = {shadowDrawable, getColoredBg(ctx)};
		return new LayerDrawable(layers);
	}

	public boolean isNightMode(@NonNull OsmandApplication app) {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}

	private void showShadowButton() {
		buttonsShadow.setVisibility(View.VISIBLE);
		buttonsShadow.animate()
				.alpha(0.8f)
				.setDuration(200)
				.setListener(null);
	}

	private void hideShadowButton() {
		buttonsShadow.animate()
				.alpha(0f)
				.setDuration(200);

	}

	private void setupScrollShadow(View view) {
		View scrollView;
		if (useScrollableItemsContainer()) {
			scrollView = view.findViewById(R.id.scroll_view);
		} else {
			scrollView = itemsContainer;
		}
		scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
			boolean scrollToBottomAvailable = scrollView.canScrollVertically(1);
			if (scrollToBottomAvailable) {
				showShadowButton();
			} else {
				hideShadowButton();
			}
		});
	}

	protected void setShadowOnScrollableView() {
		ScrollView scrollView = getView().findViewById(R.id.scroll_view);
		boolean isScrollable = scrollView.getChildAt(0).getHeight() >= scrollView.getHeight();
		if (isScrollable) {
			drawTopShadow(false);
			scrollView.getChildAt(0).setPadding(0, 8, 0, 0);
		} else {
			drawTopShadow(true);
		}
	}
}