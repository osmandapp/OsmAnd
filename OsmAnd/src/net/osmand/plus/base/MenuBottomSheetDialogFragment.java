package net.osmand.plus.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnScrollChangedListener;
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

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.util.ArrayList;
import java.util.List;

public abstract class MenuBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private static final String USED_ON_MAP_KEY = "used_on_map";
	private static final int DEFAULT_VALUE = -1;

	protected List<BaseBottomSheetItem> items = new ArrayList<>();

	protected boolean usedOnMap = true;
	protected boolean nightMode;

	protected int themeRes;
	protected View dismissButton;
	protected View rightButton;
	protected View thirdButton;

	private View buttonsShadow;
	private LinearLayout itemsContainer;
	private LinearLayout buttonsContainer;

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
		themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		createMenuItems(savedInstanceState);
		Activity activity = requireActivity();
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
	public void onSaveInstanceState(Bundle outState) {
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

	protected void setupHeightAndBackground(final View mainView) {
		final Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		final int screenHeight = AndroidUtils.getScreenHeight(activity);
		final int statusBarHeight = AndroidUtils.getStatusBarHeight(activity);
		final int contentHeight = getContentHeight(screenHeight - statusBarHeight);

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				ViewTreeObserver obs = mainView.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}

				final View contentView = useScrollableItemsContainer() ? mainView.findViewById(R.id.scroll_view) : itemsContainer;
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

	protected ViewTreeObserver.OnGlobalLayoutListener getShadowLayoutListener(){
		return new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				setShadowOnScrollableView();
			}
		};
	}

	protected void drawTopShadow(boolean showTopShadow) {
		final Activity activity = getActivity();
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

	protected int getDismissButtonHeight(){
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height_small);
	}

	protected int getRightButtonHeight(){
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height_small);
	}

	protected int getThirdButtonHeight(){
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
			dismissButton.findViewById(R.id.button_text).setEnabled(enabled);
		}
		if (rightButton != null) {
			boolean enabled = isRightBottomButtonEnabled();
			rightButton.setEnabled(enabled);
			rightButton.findViewById(R.id.button_text).setEnabled(enabled);
		}
	}

	private void setupDismissButton() {
		dismissButton = buttonsContainer.findViewById(R.id.dismiss_button);
		dismissButton.getLayoutParams().height = getDismissButtonHeight();
		int buttonTextId = getDismissButtonTextId();
		if (buttonTextId != DEFAULT_VALUE) {
			UiUtilities.setupDialogButton(nightMode, dismissButton, getDismissButtonType(), buttonTextId);
			dismissButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onDismissButtonClickAction();
					dismiss();
				}
			});
		}
		AndroidUiHelper.updateVisibility(dismissButton, buttonTextId != DEFAULT_VALUE);
	}

	protected void setupRightButton() {
		rightButton = buttonsContainer.findViewById(R.id.right_bottom_button);
		rightButton.getLayoutParams().height = getRightButtonHeight();
		int buttonTextId = getRightBottomButtonTextId();
		if (buttonTextId != DEFAULT_VALUE) {
			UiUtilities.setupDialogButton(nightMode, rightButton, getRightBottomButtonType(), buttonTextId);
			rightButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onRightBottomButtonClick();
				}
			});
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
		thirdButton.getLayoutParams().height = getThirdButtonHeight();
		int buttonTextId = getThirdBottomButtonTextId();
		if (buttonTextId != DEFAULT_VALUE) {
			UiUtilities.setupDialogButton(nightMode, thirdButton, getThirdBottomButtonType(), buttonTextId);
			thirdButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onThirdBottomButtonClick();
				}
			});
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
		return nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light;
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

	private LayerDrawable createBackgroundDrawable(@NonNull Context ctx, @DrawableRes int shadowDrawableResId) {
		Drawable shadowDrawable = ContextCompat.getDrawable(ctx, shadowDrawableResId);
		Drawable[] layers = new Drawable[]{shadowDrawable, getColoredBg(ctx)};
		return new LayerDrawable(layers);
	}

	protected boolean isNightMode(@NonNull OsmandApplication app) {
		if (usedOnMap) {
			return app.getDaynightHelper().isNightModeForMapControls();
		}
		return !app.getSettings().isLightContent();
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
		final View scrollView;
		if (useScrollableItemsContainer()) {
			scrollView = view.findViewById(R.id.scroll_view);
		} else {
			scrollView = itemsContainer;
		}
		scrollView.getViewTreeObserver().addOnScrollChangedListener(new OnScrollChangedListener() {

			@Override
			public void onScrollChanged() {
				boolean scrollToBottomAvailable = scrollView.canScrollVertically(1);
				if (scrollToBottomAvailable) {
					showShadowButton();
				} else {
					hideShadowButton();
				}
			}
		});
	}

	protected void setShadowOnScrollableView() {
		ScrollView scrollView = getView().findViewById(R.id.scroll_view);
		boolean isScrollable = scrollView.getChildAt(0).getHeight() >= scrollView.getHeight();;
		if (isScrollable) {
			drawTopShadow(false);
			scrollView.getChildAt(0).setPadding(0,8,0,0);
		} else {
			drawTopShadow(true);
		}
	}
}