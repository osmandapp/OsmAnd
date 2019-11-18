package net.osmand.plus.base;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

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

	private LinearLayout itemsContainer;

	@StringRes
	protected int dismissButtonStringRes = R.string.shared_string_cancel;

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
		Context ctx = requireContext();
		View mainView = View.inflate(new ContextThemeWrapper(ctx, themeRes), R.layout.bottom_sheet_menu_base, null);
		if (useScrollableItemsContainer()) {
			itemsContainer = (LinearLayout) mainView.findViewById(R.id.scrollable_items_container);
		} else {
			mainView.findViewById(R.id.scroll_view).setVisibility(View.GONE);
			itemsContainer = (LinearLayout) mainView.findViewById(R.id.non_scrollable_items_container);
			itemsContainer.setVisibility(View.VISIBLE);
		}

		inflateMenuItems();

		dismissButton = mainView.findViewById(R.id.dismiss_button);
		UiUtilities.setupDialogButton(nightMode, dismissButton, getDismissByttonType(), getDismissButtonTextId());
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
				UiUtilities.setupDialogButton(nightMode, rightButton, getRightBottomByttonType(), rightBottomButtonTextId);
				rightButton.setVisibility(View.VISIBLE);
				rightButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onRightBottomButtonClick();
					}
				});
			}
		}
		updateBottomButtons();
		setupHeightAndBackground(mainView);
		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		FragmentActivity activity = requireActivity();
		if (!AndroidUiHelper.isOrientationPortrait(activity)) {
			final Window window = getDialog().getWindow();
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
		OsmandApplication app = getMyApplication();
		for (BaseBottomSheetItem item : items) {
			item.inflate(app, itemsContainer, nightMode);
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
					if (useScrollableItemsContainer()) {
						contentView.getLayoutParams().height = contentHeight;
					} else {
						contentView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
					}
					contentView.requestLayout();
				}

				// 8dp is the shadow height
				boolean showTopShadow = screenHeight - statusBarHeight - mainView.getHeight() >= AndroidUtils.dpToPx(activity, 8);
				if (AndroidUiHelper.isOrientationPortrait(activity)) {
					mainView.setBackgroundResource(showTopShadow ? getPortraitBgResId() : getBgColorId());
					if (!showTopShadow) {
						mainView.setPadding(0, 0, 0, 0);
					}
				} else {
					mainView.setBackgroundResource(showTopShadow ? getLandscapeTopsidesBgResId() : getLandscapeSidesBgResId());
				}
			}
		});
	}

	private int getContentHeight(int availableScreenHeight) {
		int customHeight = getCustomHeight();
		int maxHeight = availableScreenHeight - getResources().getDimensionPixelSize(R.dimen.dialog_button_ex_height);
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

	protected boolean hideButtonsContainer() {
		return false;
	}

	@ColorRes
	protected int getDividerColorId() {
		return DEFAULT_VALUE;
	}

	@StringRes
	protected int getDismissButtonTextId() {
		return dismissButtonStringRes;
	}

	protected void setDismissButtonTextId(@StringRes int stringRes) {
		dismissButtonStringRes = stringRes;
	}

	protected DialogButtonType getDismissByttonType() {
		return DialogButtonType.SECONDARY;
	}

	protected void onDismissButtonClickAction() {

	}

	@StringRes
	protected int getRightBottomButtonTextId() {
		return DEFAULT_VALUE;
	}

	protected DialogButtonType getRightBottomByttonType() {
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

	protected boolean isNightMode(@NonNull OsmandApplication app) {
		if (usedOnMap) {
			return app.getDaynightHelper().isNightModeForMapControls();
		}
		return !app.getSettings().isLightContent();
	}
}
