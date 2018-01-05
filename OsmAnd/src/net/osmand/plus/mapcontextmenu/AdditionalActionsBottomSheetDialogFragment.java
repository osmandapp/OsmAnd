package net.osmand.plus.mapcontextmenu;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.tools.ExtendedBottomSheetBehavior;
import net.osmand.plus.widgets.tools.ExtendedBottomSheetBehavior.BottomSheetCallback;

public class AdditionalActionsBottomSheetDialogFragment extends net.osmand.plus.base.BottomSheetDialogFragment {

	public static final String TAG = "AdditionalActionsBottomSheetDialogFragment";

	private boolean nightMode;
	private boolean portrait;
	private ContextMenuAdapter adapter;
	private ContextMenuItemClickListener listener;

	public void setAdapter(ContextMenuAdapter adapter, ContextMenuItemClickListener listener) {
		this.adapter = adapter;
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Activity activity = getActivity();
		nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		portrait = AndroidUiHelper.isOrientationPortrait(activity);
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_context_menu_actions_bottom_sheet_dialog, null);
		View scrollView = mainView.findViewById(R.id.bottom_sheet_scroll_view);

//		AndroidUtils.setBackground(getActivity(), mainView, nightMode,
//				portrait ? R.drawable.bg_bottom_menu_light : R.drawable.bg_bottom_sheet_topsides_landscape_light,
//				portrait ? R.drawable.bg_bottom_menu_dark : R.drawable.bg_bottom_sheet_topsides_landscape_dark);

		AndroidUtils.setBackground(activity, scrollView, nightMode,
				R.color.route_info_bottom_view_bg_light, R.color.route_info_bg_dark);
		AndroidUtils.setBackground(activity, mainView.findViewById(R.id.cancel_row), nightMode,
				R.color.route_info_bottom_view_bg_light, R.color.route_info_bg_dark);
		AndroidUtils.setBackground(activity, mainView.findViewById(R.id.divider), nightMode,
				R.color.route_info_divider_light, R.color.route_info_bottom_view_bg_dark);

		View.OnClickListener dismissOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		};

		mainView.findViewById(R.id.cancel_row).setOnClickListener(dismissOnClickListener);
		mainView.findViewById(R.id.scroll_view_container).setOnClickListener(dismissOnClickListener);

		TextView headerTitle = (TextView) mainView.findViewById(R.id.header_title);
		if (nightMode) {
			headerTitle.setTextColor(ContextCompat.getColor(activity, R.color.ctx_menu_info_text_dark));
		}
		headerTitle.setText(R.string.additional_actions);

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.onItemClick((int) view.getTag());
				}
				dismiss();
			}
		};

		LinearLayout itemsLinearLayout = (LinearLayout) mainView.findViewById(R.id.context_menu_items_container);
		LinearLayout row = (LinearLayout) View.inflate(getContext(), R.layout.grid_menu_row, null);
		int itemsAdded = 0;
		for (int i = 0; i < adapter.length(); i++) {
			ContextMenuItem item = adapter.getItem(i);
			int layoutResId = item.getLayout();
			layoutResId = layoutResId == ContextMenuItem.INVALID_ID ? R.layout.grid_menu_item : layoutResId;
			boolean dividerItem = layoutResId == R.layout.bottom_sheet_dialog_fragment_divider;

			if (!dividerItem) {
				View menuItem = View.inflate(new ContextThemeWrapper(getContext(), themeRes), layoutResId, null);
				if (item.getIcon() != ContextMenuItem.INVALID_ID) {
					((ImageView) menuItem.findViewById(R.id.icon)).setImageDrawable(getContentIcon(item.getIcon()));
				}
				((TextView) menuItem.findViewById(R.id.title)).setText(item.getTitle());
				menuItem.setTag(i);
				menuItem.setOnClickListener(onClickListener);
				((FrameLayout) row.findViewById(getMenuItemContainerId(itemsAdded))).addView(menuItem);
				itemsAdded++;
			}

			if (dividerItem || itemsAdded == 3 || (i == adapter.length() - 1 && itemsAdded > 0)) {
				itemsLinearLayout.addView(row);
				row = (LinearLayout) View.inflate(getContext(), R.layout.grid_menu_row, null);
				itemsAdded = 0;
			}
		}

		ExtendedBottomSheetBehavior behavior = ExtendedBottomSheetBehavior.from(scrollView);
		behavior.setPeekHeight(getPeekHeight());
		behavior.setBottomSheetCallback(new BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				if (newState == ExtendedBottomSheetBehavior.STATE_HIDDEN) {
					dismiss();
				}
			}

			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {

			}
		});

		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!portrait) {
			final Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.width = getActivity().getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			window.setAttributes(params);
		}
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getMyApplication().getIconsCache().getIcon(id, nightMode ? R.color.grid_menu_icon_dark : R.color.on_map_icon_color);
	}

	private int getMenuItemContainerId(int itemsAdded) {
		if (itemsAdded == 0) {
			return R.id.first_item_container;
		} else if (itemsAdded == 1) {
			return R.id.second_item_container;
		}
		return R.id.third_item_container;
	}

	private int getPeekHeight() {
		Activity ctx = getActivity();
		int screenH = AndroidUtils.getScreenHeight(ctx);
		int availableH = screenH - AndroidUtils.getNavBarHeight(ctx) - AndroidUtils.getStatusBarHeight(ctx);
		return (availableH * 2 / 3) - getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
	}

	public interface ContextMenuItemClickListener {
		void onItemClick(int position);
	}
}
