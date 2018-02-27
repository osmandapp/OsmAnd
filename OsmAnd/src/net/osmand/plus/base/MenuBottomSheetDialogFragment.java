package net.osmand.plus.base;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.View;
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
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	protected Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.osmand_orange : R.color.color_myloc_distance);
	}

	protected void setupHeightAndBackground(final View mainView, final int scrollViewId) {
		final Activity activity = getActivity();
		final int screenHeight = AndroidUtils.getScreenHeight(activity);
		final int statusBarHeight = AndroidUtils.getStatusBarHeight(activity);
		final int navBarHeight = AndroidUtils.getNavBarHeight(activity);

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				final View scrollView = mainView.findViewById(scrollViewId);
				int scrollViewHeight = scrollView.getHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int cancelButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
				int spaceForScrollView = screenHeight - statusBarHeight - navBarHeight - dividerHeight - cancelButtonHeight;
				if (scrollViewHeight > spaceForScrollView) {
					scrollView.getLayoutParams().height = spaceForScrollView;
					scrollView.requestLayout();
				}

				if (AndroidUiHelper.isOrientationPortrait(activity)) {
					mainView.setBackgroundResource(getPortraitBgResId());
				} else {
					if (screenHeight - statusBarHeight - mainView.getHeight() >= getResources().getDimension(R.dimen.bottom_sheet_content_padding_small)) {
						mainView.setBackgroundResource(getLandscapeTopsidesBgResId());
					} else {
						mainView.setBackgroundResource(getLandscapeSidesBgResId());
					}
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

	protected View inflateMainView() {
		OsmandApplication app = getMyApplication();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		View mainView = View.inflate(new ContextThemeWrapper(app, themeRes), R.layout.bottom_sheet_menu_base, null);
		LinearLayout container = (LinearLayout) mainView.findViewById(R.id.items_container);

		for (BaseBottomSheetItem item : items) {
			item.inflate(app, container, nightMode);
		}

		int closeRowDividerColorId = getCloseRowDividerColorId();
		if (closeRowDividerColorId != -1) {
			mainView.findViewById(R.id.close_row_divider)
					.setBackgroundColor(ContextCompat.getColor(getContext(), closeRowDividerColorId));
		}
		mainView.findViewById(R.id.close_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		int closeRowTextId = getCloseRowTextId();
		if (closeRowTextId != -1) {
			((TextView) mainView.findViewById(R.id.close_row_text)).setText(closeRowTextId);
		}

		setupHeightAndBackground(mainView, R.id.scroll_view);

		return mainView;
	}

	@ColorRes
	protected int getCloseRowDividerColorId() {
		return -1;
	}

	@StringRes
	protected int getCloseRowTextId() {
		return -1;
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
