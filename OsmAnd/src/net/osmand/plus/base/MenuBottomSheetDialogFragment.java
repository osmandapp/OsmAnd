package net.osmand.plus.base;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
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
		nightMode = isNightMode();
		themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		createMenuItems(savedInstanceState);

		OsmandApplication app = getMyApplication();

		View mainView = View.inflate(new ContextThemeWrapper(app, themeRes), R.layout.bottom_sheet_menu_base, null);
		if (useScrollableItemsContainer()) {
			itemsContainer = (LinearLayout) mainView.findViewById(R.id.scrollable_items_container);
		} else {
			mainView.findViewById(R.id.scroll_view).setVisibility(View.GONE);
			itemsContainer = (LinearLayout) mainView.findViewById(R.id.non_scrollable_items_container);
			itemsContainer.setVisibility(View.VISIBLE);
		}

		inflateMenuItems();

		int bottomDividerColorId = getBottomDividerColorId();
		if (bottomDividerColorId != DEFAULT_VALUE) {
			mainView.findViewById(R.id.bottom_row_divider).setBackgroundColor(getResolvedColor(bottomDividerColorId));
		}

		mainView.findViewById(R.id.dismiss_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onDismissButtonClickAction();
				dismiss();
			}
		});
		if (hideButtonsContainer()) {
			mainView.findViewById(R.id.bottom_row_divider).setVisibility(View.GONE);
			mainView.findViewById(R.id.buttons_container).setVisibility(View.GONE);
		} else {
			((TextView) mainView.findViewById(R.id.dismiss_button_text)).setText(getDismissButtonTextId());

			int rightBottomButtonTextId = getRightBottomButtonTextId();
			if (rightBottomButtonTextId != DEFAULT_VALUE) {
				View buttonsDivider = mainView.findViewById(R.id.bottom_buttons_divider);
				buttonsDivider.setVisibility(View.VISIBLE);
				if (bottomDividerColorId != DEFAULT_VALUE) {
					buttonsDivider.setBackgroundColor(getResolvedColor(bottomDividerColorId));
				}
				View rightButton = mainView.findViewById(R.id.right_bottom_button);
				rightButton.setVisibility(View.VISIBLE);
				rightButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onRightBottomButtonClick();
					}
				});
				((TextView) rightButton.findViewById(R.id.right_bottom_button_text)).setText(rightBottomButtonTextId);
			}
		}

		setupHeightAndBackground(mainView);

		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!AndroidUiHelper.isOrientationPortrait(getActivity())) {
			final Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.width = getActivity().getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			window.setAttributes(params);
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

	protected void inflateMenuItems(){
		OsmandApplication app = getMyApplication();
		for (BaseBottomSheetItem item : items) {
			item.inflate(app, itemsContainer, nightMode);
		}
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
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
		return ContextCompat.getColor(getContext(), colorId);
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

				ViewTreeObserver obs = mainView.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
			}
		});
	}

	private int getContentHeight(int availableScreenHeight) {
		int customHeight = getCustomHeight();
		int maxHeight = availableScreenHeight
				- AndroidUtils.dpToPx(getContext(), 1) // divider height
				- getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
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
	protected int getBottomDividerColorId() {
		return DEFAULT_VALUE;
	}

	@StringRes
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	protected void onDismissButtonClickAction() {

	}

	@StringRes
	protected int getRightBottomButtonTextId() {
		return DEFAULT_VALUE;
	}

	protected void onRightBottomButtonClick() {

	}

	@ColorRes
	protected int getBgColorId() {
		return nightMode ? R.color.bg_color_dark : R.color.bg_color_light;
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

	private boolean isNightMode() {
		if (usedOnMap) {
			return getMyApplication().getDaynightHelper().isNightModeForMapControls();
		}
		return !getMyApplication().getSettings().isLightContent();
	}
}
