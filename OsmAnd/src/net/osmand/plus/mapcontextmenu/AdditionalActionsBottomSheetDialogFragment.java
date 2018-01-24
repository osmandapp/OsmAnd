package net.osmand.plus.mapcontextmenu;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
	private int availableScreenH;

	private ContextMenuAdapter adapter;
	private ContextMenuItemClickListener listener;

	private View scrollView;
	private View cancelRowBgView;

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
		availableScreenH = AndroidUtils.getScreenHeight(activity) - AndroidUtils.getStatusBarHeight(activity);
		if (portrait) {
			availableScreenH -= AndroidUtils.getNavBarHeight(activity);
		}

		View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_context_menu_actions_bottom_sheet_dialog, null);
		scrollView = mainView.findViewById(R.id.bottom_sheet_scroll_view);
		cancelRowBgView = mainView.findViewById(R.id.cancel_row_background);

		updateBackground(false);
		cancelRowBgView.setBackgroundResource(getCancelRowBgResId());
		mainView.findViewById(R.id.divider).setBackgroundResource(nightMode
				? R.color.route_info_bottom_view_bg_dark : R.color.route_info_divider_light);

		View.OnClickListener dismissOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		};

		mainView.findViewById(R.id.cancel_row).setOnClickListener(dismissOnClickListener);
		mainView.findViewById(R.id.scroll_view_container).setOnClickListener(dismissOnClickListener);

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

			View menuItem = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.grid_menu_item, null);
			if (item.getIcon() != ContextMenuItem.INVALID_ID) {
				((ImageView) menuItem.findViewById(R.id.icon)).setImageDrawable(getContentIcon(item.getIcon()));
			}
			((TextView) menuItem.findViewById(R.id.title)).setText(item.getTitle());
			if (item.isClickable()) {
				menuItem.setTag(i);
				menuItem.setOnClickListener(onClickListener);
			} else {
				menuItem.setEnabled(false);
				menuItem.setAlpha(.5f);
			}
			((FrameLayout) row.findViewById(getMenuItemContainerId(itemsAdded))).addView(menuItem);
			itemsAdded++;

			if (itemsAdded == 3 || (i == adapter.length() - 1 && itemsAdded > 0)) {
				itemsLinearLayout.addView(row);
				row = (LinearLayout) View.inflate(getContext(), R.layout.grid_menu_row, null);
				itemsAdded = 0;
			}
		}

		final ExtendedBottomSheetBehavior behavior = ExtendedBottomSheetBehavior.from(scrollView);
		behavior.setBottomSheetCallback(new BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				if (newState == ExtendedBottomSheetBehavior.STATE_HIDDEN) {
					dismiss();
				} else {
					updateBackground(newState == ExtendedBottomSheetBehavior.STATE_EXPANDED);
				}
			}

			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {

			}
		});
		if (portrait) {
			behavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_menu_peek_height));
		} else {
			getDialog().setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					behavior.setState(ExtendedBottomSheetBehavior.STATE_EXPANDED);
				}
			});
		}

		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!portrait) {
			final Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.width = getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width)
					+ AndroidUtils.dpToPx(getContext(), 16); // 8 dp is shadow width on each side
			window.setAttributes(params);
		}
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getMyApplication().getIconsCache().getIcon(id, nightMode ? R.color.grid_menu_icon_dark : R.color.on_map_icon_color);
	}

	private int getCancelRowBgResId() {
		if (portrait) {
			return nightMode ? R.color.ctx_menu_bg_dark : R.color.route_info_bottom_view_bg_light;
		}
		return nightMode ? R.drawable.bg_additional_menu_sides_dark : R.drawable.bg_additional_menu_sides_light;
	}

	private boolean expandedToFullScreen() {
		return availableScreenH - scrollView.getHeight() - cancelRowBgView.getHeight() <= 0;
	}

	private void updateBackground(boolean expanded) {
		int bgResId;
		if (portrait) {
			bgResId = expanded && expandedToFullScreen()
					? (nightMode ? R.color.ctx_menu_bg_dark : R.color.route_info_bottom_view_bg_light)
					: (nightMode ? R.drawable.bg_additional_menu_dark : R.drawable.bg_additional_menu_light);
		} else {
			bgResId = expanded && expandedToFullScreen()
					? (nightMode ? R.drawable.bg_additional_menu_sides_dark : R.drawable.bg_additional_menu_sides_light)
					: (nightMode ? R.drawable.bg_additional_menu_topsides_dark : R.drawable.bg_additional_menu_topsides_light);
		}
		scrollView.setBackgroundResource(bgResId);
	}

	private int getMenuItemContainerId(int itemsAdded) {
		if (itemsAdded == 0) {
			return R.id.first_item_container;
		} else if (itemsAdded == 1) {
			return R.id.second_item_container;
		}
		return R.id.third_item_container;
	}

	public interface ContextMenuItemClickListener {
		void onItemClick(int position);
	}
}
