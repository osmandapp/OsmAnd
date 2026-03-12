package net.osmand.plus.mapcontextmenu;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.tools.ExtendedBottomSheetBehavior;
import net.osmand.plus.widgets.tools.ExtendedBottomSheetBehavior.BottomSheetCallback;

public class AdditionalActionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

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
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Activity activity = requireActivity();
		nightMode = requiredMyApplication().getDaynightHelper().isNightModeForMapControls();
		portrait = AndroidUiHelper.isOrientationPortrait(activity);
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		availableScreenH = AndroidUtils.getScreenHeight(activity) - AndroidUtils.getStatusBarHeight(activity);
		if (portrait) {
			availableScreenH -= AndroidUtils.getNavBarHeight(activity);
		}

		ContextThemeWrapper context = new ContextThemeWrapper(getContext(), themeRes);
		View mainView = View.inflate(context, R.layout.fragment_context_menu_actions_bottom_sheet_dialog, null);
		scrollView = mainView.findViewById(R.id.bottom_sheet_scroll_view);
		cancelRowBgView = mainView.findViewById(R.id.cancel_row_background);

		updateBackground(false);
		cancelRowBgView.setBackgroundResource(getCancelRowBgResId());
		mainView.findViewById(R.id.divider).setBackgroundResource(nightMode
				? R.color.card_and_list_background_dark : R.color.divider_color_light);

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
					listener.onItemClick(view, (int) view.getTag());
				}
				dismiss();
			}
		};

		if (adapter != null) {
			LinearLayout itemsLinearLayout = mainView.findViewById(R.id.context_menu_items_container);
			LinearLayout row = (LinearLayout) View.inflate(context, R.layout.grid_menu_row, null);
			int itemsAdded = 0;
			for (int i = 0; i < adapter.length(); i++) {
				ContextMenuItem item = adapter.getItem(i);

				View menuItem = View.inflate(context, R.layout.grid_menu_item, null);
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
					row = (LinearLayout) View.inflate(context, R.layout.grid_menu_row, null);
					itemsAdded = 0;
				}
			}
		}

		ExtendedBottomSheetBehavior behavior = ExtendedBottomSheetBehavior.from(scrollView);
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
			Window window = requireDialog().getWindow();
			if (window != null){
				WindowManager.LayoutParams params = window.getAttributes();
				params.width = getResources().getDisplayMetrics().widthPixels / 2;
				window.setAttributes(params);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (adapter == null) {
			dismiss();
		}
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getMyApplication().getUIUtilities().getIcon(id, nightMode ? R.color.grid_menu_icon_dark : R.color.on_map_icon_color);
	}

	private int getCancelRowBgResId() {
		if (portrait) {
			return nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light;
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
					? (nightMode ? R.color.list_background_color_dark : R.color.activity_background_color_light)
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
		void onItemClick(View view, int position);
	}
}
