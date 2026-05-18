package net.osmand.plus.mapcontextmenu.other;

import static net.osmand.plus.utils.ColorUtilities.getDividerColor;
import static net.osmand.plus.utils.ColorUtilities.getListBgColorId;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.other.MultiSelectionArrayAdapter.OnClickListener;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class MapMultiSelectionMenuFragment extends Fragment
		implements OnClickListener, OnGlobalLayoutListener, ObservableScrollViewCallbacks {

	public static final String TAG = "MapMultiSelectionMenuFragment";

	private View view;
	private ListView listView;
	private MultiSelectionArrayAdapter listAdapter;
	private MapMultiSelectionMenu menu;

	private int minHeight;
	private boolean initialScroll = true;
	private boolean dismissing;
	private boolean wasDrawerDisabled;

	private LayoutInflater themedInflater;
	private boolean nightMode;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		MapActivity mapActivity = (MapActivity) requireActivity();
		menu = mapActivity.getContextMenu().getMultiSelectionMenu();
		updateNightMode();
		view = inflate(R.layout.menu_obj_selection_fragment, container);
		Context context = view.getContext();

		if (menu.isLandscapeLayout()) {
			int backgroundId = nightMode
					? R.drawable.multi_selection_menu_bg_dark_land
					: R.drawable.multi_selection_menu_bg_light_land;
			AndroidUtils.setBackground(context, view, backgroundId);
		} else {
			View cancelRow = view.findViewById(R.id.cancel_row);
			AndroidUtils.setBackground(context, cancelRow, getListBgColorId(nightMode));
		}

		listView = view.findViewById(R.id.list);
		if (menu.isLandscapeLayout()) {
			AndroidUtils.addStatusBarPadding21v(mapActivity, listView);
		}
		listAdapter = new MultiSelectionArrayAdapter(menu);
		listAdapter.setListener(this);

		if (!menu.isLandscapeLayout()) {
			FrameLayout paddingView = new FrameLayout(context);
			int screenHeight = AndroidUtils.getScreenHeight(mapActivity);
			int cancelButtonHeight = getDimension(R.dimen.bottom_sheet_cancel_button_height);
			int padding = screenHeight - cancelButtonHeight;
			paddingView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, padding));
			paddingView.setClickable(true);
			paddingView.setOnClickListener(v -> dismiss());

			FrameLayout shadowContainer = new FrameLayout(context);
			shadowContainer.setLayoutParams(new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.BOTTOM
			));

			ImageView shadow = new ImageView(context);
			shadow.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.bg_shadow_onmap));
			shadow.setLayoutParams(new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
			));
			shadow.setScaleType(ImageView.ScaleType.FIT_XY);

			shadowContainer.addView(shadow);
			paddingView.addView(shadowContainer);
			listView.addHeaderView(paddingView);

			view.getViewTreeObserver().addOnGlobalLayoutListener(this);
			((ObservableListView) listView).setScrollViewCallbacks(this);
		}

		View headerView = inflate(R.layout.menu_obj_selection_header, listView);
		if (!menu.isLandscapeLayout()) {
			AndroidUtils.setBackground(context, headerView, getListBgColorId(nightMode));
		}
		headerView.setOnClickListener(null);
		listView.addHeaderView(headerView);
		listView.setAdapter(listAdapter);

		View divider = view.findViewById(R.id.divider);
		divider.setBackgroundColor(getDividerColor(context, nightMode));

		TextView tvCancelRow = view.findViewById(R.id.cancel_row_text);
		int cancelRowColorId = nightMode
				? R.color.multi_selection_menu_close_btn_dark
				: R.color.multi_selection_menu_close_btn_light;
		tvCancelRow.setTextColor(ColorUtilities.getColor(context, cancelRowColorId));
		View cancelRow = view.findViewById(R.id.cancel_row);
		cancelRow.setOnClickListener(view -> dismiss());

		return view;
	}

	@Override
	public void onGlobalLayout() {
		float titleHeight = getResources().getDimension(R.dimen.multi_selection_header_height);
		int maxHeight = (int) (titleHeight);
		for (int i = 0; i < 3 && i < listAdapter.getCount(); i++) {
			View childView = listAdapter.getView(0, null, view.findViewById(R.id.list));
			childView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
			maxHeight += childView.getMeasuredHeight();
		}

		listView.setSelectionFromTop(0, -maxHeight);

		ViewTreeObserver obs = view.getViewTreeObserver();
		obs.removeOnGlobalLayoutListener(this);
	}

	@Override
	public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
		if (minHeight == 0) {
			int headerHeight = getDimension(R.dimen.multi_selection_header_height);
			int listItemHeight = getDimension(R.dimen.list_item_height);
			minHeight = headerHeight + listItemHeight;
		}
		if (scrollY <= minHeight && !initialScroll) {
			dismiss();
		}
	}

	@Override
	public void onDownMotionEvent() {
		initialScroll = false;
	}

	@Override
	public void onItemClicked(int position) {
		MenuObject menuObject = listAdapter.getItem(position);
		if (menuObject != null) {
			menu.openContextMenu(menuObject);
		}
	}

	public void updateContent() {
		if (listAdapter != null) {
			listAdapter.notifyDataSetChanged();
		}
	}

	private void updateNightMode() {
		menu.updateNightMode();
		nightMode = !menu.isLight();
		themedInflater = UiUtilities.getInflater(menu.getMapActivity(), nightMode);
	}

	private View inflate(@LayoutRes int layoutId, @Nullable ViewGroup container) {
		return themedInflater.inflate(layoutId, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();
		MapActivity mapActivity = menu.getMapActivity();
		if (mapActivity == null) {
			return;
		}
		mapActivity.getMapLayers().getMapControlsLayer().setControlsClickable(false);
		mapActivity.getContextMenu().setBaseFragmentVisibility(false);
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = menu.getMapActivity();
		if (mapActivity == null) {
			return;
		}
		MapRouteInfoMenu mapRouteInfoMenu = mapActivity.getMapRouteInfoMenu();
		if (mapRouteInfoMenu.isVisible()) {
			dismiss();
			return;
		}
		wasDrawerDisabled = mapActivity.isDrawerDisabled();
		if (!wasDrawerDisabled) {
			menu.getMapActivity().disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = menu.getMapActivity();
		if (mapActivity != null && !wasDrawerDisabled) {
			mapActivity.enableDrawer();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (!dismissing) {
			menu.onStop();
		}
		MapActivity mapActivity = menu.getMapActivity();
		if (mapActivity != null) {
			mapActivity.getContextMenu().setBaseFragmentVisibility(true);
			mapActivity.getMapLayers().getMapControlsLayer().setControlsClickable(true);
		}
	}

	public void dismiss() {
		dismissing = true;
		MapActivity mapActivity = menu.getMapActivity();
		if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
			MapContextMenu contextMenu = mapActivity.getContextMenu();
			if (contextMenu.isVisible()) {
				contextMenu.hide();
			} else {
				FragmentManager manager = mapActivity.getSupportFragmentManager();
				if (!manager.isStateSaved()) {
					manager.popBackStack();
				}
			}
		}
	}

	private int getDimension(@DimenRes int dimensionResId) {
		return requireContext().getResources().getDimensionPixelSize(dimensionResId);
	}

	public static void showInstance(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		MapContextMenu contextMenu = mapActivity.getContextMenu();
		MapMultiSelectionMenu menu = contextMenu.getMultiSelectionMenu();

		FragmentManager manager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			if (contextMenu.isVisible()) {
				contextMenu.hide();
			}
			int slideInAnim = 0;
			int slideOutAnim = 0;
			if (menu != null && !settings.DO_NOT_USE_ANIMATIONS.get()) {
				slideInAnim = menu.getSlideInAnimation();
				slideOutAnim = menu.getSlideOutAnimation();
			}
			MapMultiSelectionMenuFragment fragment = new MapMultiSelectionMenuFragment();
			manager.beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
