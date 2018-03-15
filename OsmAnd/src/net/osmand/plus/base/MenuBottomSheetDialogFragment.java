package net.osmand.plus.base;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
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

	protected List<BaseBottomSheetItem> items = new ArrayList<>();

	protected boolean usedOnMap = true;
	protected boolean nightMode;

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
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		createMenuItems(savedInstanceState);

		OsmandApplication app = getMyApplication();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		View mainView = View.inflate(new ContextThemeWrapper(app, themeRes), R.layout.bottom_sheet_menu_base, null);
		itemsContainer = (LinearLayout) mainView.findViewById(R.id.items_container);

		for (BaseBottomSheetItem item : items) {
			item.inflate(app, itemsContainer, nightMode);
		}

		int bottomDividerColorId = getBottomDividerColorId();
		if (bottomDividerColorId != -1) {
			mainView.findViewById(R.id.bottom_row_divider).setBackgroundColor(getResolvedColor(bottomDividerColorId));
		}
		mainView.findViewById(R.id.dismiss_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onDismissButtonClickAction();
				dismiss();
			}
		});
		((TextView) mainView.findViewById(R.id.dismiss_button_text)).setText(getDismissButtonTextId());

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

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	protected Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.osmand_orange : R.color.color_myloc_distance);
	}

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(getContext(), colorId);
	}

	protected void setupHeightAndBackground(final View mainView) {
		final Activity activity = getActivity();
		final int screenHeight = AndroidUtils.getScreenHeight(activity);
		final int statusBarHeight = AndroidUtils.getStatusBarHeight(activity);
		final int navBarHeight = AndroidUtils.getNavBarHeight(activity);

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				final View scrollView = mainView.findViewById(R.id.scroll_view);
				int scrollViewHeight = scrollView.getHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int bottomRowHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
				int spaceForScrollView = screenHeight - statusBarHeight - navBarHeight - dividerHeight - bottomRowHeight;
				if (scrollViewHeight > spaceForScrollView) {
					scrollView.getLayoutParams().height = spaceForScrollView;
					scrollView.requestLayout();
				}

				// 8dp is the shadow height
				boolean showTopShadow = screenHeight - statusBarHeight - mainView.getHeight() >= AndroidUtils.dpToPx(activity, 8);
				if (AndroidUiHelper.isOrientationPortrait(activity)) {
					mainView.setBackgroundResource(showTopShadow ? getPortraitBgResId() : getBgColorId());
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

	@ColorRes
	protected int getBottomDividerColorId() {
		return -1;
	}

	@StringRes
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	protected void onDismissButtonClickAction() {

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

	protected boolean isNightMode() {
		if (usedOnMap) {
			return getMyApplication().getDaynightHelper().isNightModeForMapControls();
		}
		return !getMyApplication().getSettings().isLightContent();
	}
}
