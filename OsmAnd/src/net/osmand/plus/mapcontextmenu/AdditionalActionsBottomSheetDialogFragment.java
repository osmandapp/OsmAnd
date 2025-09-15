package net.osmand.plus.mapcontextmenu;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.tools.ExtendedBottomSheetBehavior;
import net.osmand.plus.widgets.tools.ExtendedBottomSheetBehavior.BottomSheetCallback;

import java.util.ArrayList;
import java.util.List;

public class AdditionalActionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = AdditionalActionsBottomSheetDialogFragment.class.getSimpleName();

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
		updateNightMode();
		Activity activity = requireActivity();
		portrait = AndroidUiHelper.isOrientationPortrait(activity);
		availableScreenH = AndroidUtils.getScreenHeight(activity) - AndroidUtils.getStatusBarHeight(activity);
		if (portrait) {
			availableScreenH -= AndroidUtils.getNavBarHeight(activity);
		}

		View mainView = inflate(R.layout.fragment_context_menu_actions_bottom_sheet_dialog);
		scrollView = mainView.findViewById(R.id.bottom_sheet_scroll_view);
		cancelRowBgView = mainView.findViewById(R.id.cancel_row_background);

		updateBackground(false);
		cancelRowBgView.setBackgroundResource(getCancelRowBgResId());
		mainView.findViewById(R.id.divider).setBackgroundResource(nightMode
				? R.color.card_and_list_background_dark : R.color.divider_color_light);

		View.OnClickListener dismissOnClickListener = view -> dismiss();

		mainView.findViewById(R.id.cancel_row).setOnClickListener(dismissOnClickListener);
		mainView.findViewById(R.id.scroll_view_container).setOnClickListener(dismissOnClickListener);

		View.OnClickListener onClickListener = view -> {
			if (listener != null) {
				listener.onItemClick(view, (int) view.getTag());
			}
			dismiss();
		};

		if (adapter != null) {
			LinearLayout itemsLinearLayout = mainView.findViewById(R.id.context_menu_items_container);
			LinearLayout row = (LinearLayout) inflate(R.layout.grid_menu_row);
			int itemsAdded = 0;
			for (int i = 0; i < adapter.length(); i++) {
				ContextMenuItem item = adapter.getItem(i);

				View menuItem = inflate(R.layout.grid_menu_item);
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
					row = (LinearLayout) inflate(R.layout.grid_menu_row);
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
			behavior.setPeekHeight(getDimensionPixelSize(R.dimen.bottom_sheet_menu_peek_height));
		} else {
			requireDialog().setOnShowListener(dialog -> behavior.setState(ExtendedBottomSheetBehavior.STATE_EXPANDED));
		}

		return mainView;
	}

	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.cancel_row_background);
		return ids;
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
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	@Nullable
	public Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.grid_menu_icon_dark : R.color.on_map_icon_color);
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

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull ContextMenuAdapter adapter,
	                                @NonNull ContextMenuItemClickListener listener) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			AdditionalActionsBottomSheetDialogFragment fragment =
					new AdditionalActionsBottomSheetDialogFragment();
			fragment.setAdapter(adapter, listener);
			fragment.show(manager, TAG);
		}
	}

	public interface ContextMenuItemClickListener {
		void onItemClick(View view, int position);
	}
}
