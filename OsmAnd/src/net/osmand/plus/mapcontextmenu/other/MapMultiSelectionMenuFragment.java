package net.osmand.plus.mapcontextmenu.other;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu.MenuObject;

import java.util.LinkedList;
import java.util.List;

public class MapMultiSelectionMenuFragment extends Fragment implements MultiSelectionArrayAdapter.OnClickListener {
	public static final String TAG = "MapMultiSelectionMenuFragment";

	private View view;
	private MultiSelectionArrayAdapter listAdapter;
	private MapMultiSelectionMenu menu;
	private boolean dismissing = false;
	private boolean wasDrawerDisabled;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		menu = ((MapActivity) getActivity()).getContextMenu().getMultiSelectionMenu();

		view = inflater.inflate(R.layout.menu_obj_selection_fragment, container, false);

		if (menu.isLandscapeLayout()) {
			AndroidUtils.setBackground(view.getContext(), view, !menu.isLight(),
					R.drawable.multi_selection_menu_bg_light_land, R.drawable.multi_selection_menu_bg_dark_land);
		} else {
			AndroidUtils.setBackground(view.getContext(), view.findViewById(R.id.cancel_row), !menu.isLight(),
					R.color.list_background_color_light, R.color.list_background_color_dark);
		}

		final ListView listView = (ListView) view.findViewById(R.id.list);
		if (menu.isLandscapeLayout() && Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(getActivity(), listView);
		}
		listAdapter = createAdapter();
		listAdapter.setListener(this);

		if (!menu.isLandscapeLayout()) {
			final Context context = getContext();

			FrameLayout paddingView = new FrameLayout(context);
			paddingView.setLayoutParams(new AbsListView.LayoutParams(
					AbsListView.LayoutParams.MATCH_PARENT, getPaddingViewHeight())
			);
			paddingView.setClickable(true);
			paddingView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissMenu();
				}
			});

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

			view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					float titleHeight = getResources().getDimension(R.dimen.multi_selection_header_height);
					int maxHeight = (int) (titleHeight);
					for (int i = 0; i < 3 && i < listAdapter.getCount(); i++) {
						View childView = listAdapter.getView(0, null, (ListView) view.findViewById(R.id.list));
						childView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
								View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
						maxHeight += childView.getMeasuredHeight();
					}

					listView.setSelectionFromTop(0, -maxHeight);

					ViewTreeObserver obs = view.getViewTreeObserver();
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						obs.removeOnGlobalLayoutListener(this);
					} else {
						obs.removeGlobalOnLayoutListener(this);
					}
				}
			});

			((ObservableListView) listView).setScrollViewCallbacks(new ObservableScrollViewCallbacks() {

				boolean initialScroll = true;
				int minHeight = getResources().getDimensionPixelSize(R.dimen.multi_selection_header_height)
						+ getResources().getDimensionPixelSize(R.dimen.list_item_height);

				@Override
				public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
					if (scrollY <= minHeight && !initialScroll) {
						dismissMenu();
					}
				}

				@Override
				public void onDownMotionEvent() {
					initialScroll = false;
				}

				@Override
				public void onUpOrCancelMotionEvent(ScrollState scrollState) {

				}
			});
		}
		View headerView = inflater.inflate(R.layout.menu_obj_selection_header, listView, false);
		if (!menu.isLandscapeLayout()) {
			AndroidUtils.setBackground(getContext(), headerView, !menu.isLight(), R.color.list_background_color_light, R.color.list_background_color_dark);
		}
		headerView.setOnClickListener(null);
		listView.addHeaderView(headerView);
		listView.setAdapter(listAdapter);

		view.findViewById(R.id.divider).setBackgroundColor(ContextCompat.getColor(getContext(), menu.isLight()
				? R.color.multi_selection_menu_divider_light : R.color.multi_selection_menu_divider_dark));

		((TextView) view.findViewById(R.id.cancel_row_text)).setTextColor(ContextCompat.getColor(getContext(),
				menu.isLight() ? R.color.multi_selection_menu_close_btn_light : R.color.multi_selection_menu_close_btn_dark));
		view.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismissMenu();
			}
		});

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		menu.getMapActivity().getMapLayers().getMapControlsLayer().setControlsClickable(false);
		menu.getMapActivity().getContextMenu().setBaseFragmentVisibility(false);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (menu.getMapActivity().getMapRouteInfoMenu().isVisible()) {
			dismissMenu();
			return;
		}
		wasDrawerDisabled = menu.getMapActivity().isDrawerDisabled();
		if (!wasDrawerDisabled) {
			menu.getMapActivity().disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (!wasDrawerDisabled) {
			menu.getMapActivity().enableDrawer();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (!dismissing) {
			menu.onStop();
		}
		menu.getMapActivity().getContextMenu().setBaseFragmentVisibility(true);
		menu.getMapActivity().getMapLayers().getMapControlsLayer().setControlsClickable(true);
	}

	private int getPaddingViewHeight() {
		Activity activity = getActivity();
		return AndroidUtils.getScreenHeight(activity)
				- activity.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
	}

	public static void showInstance(final MapActivity mapActivity) {

		if (mapActivity.isActivityDestroyed()) {
			return;
		}
		if (mapActivity.getContextMenu().isVisible()) {
			mapActivity.getContextMenu().hide();
		}

		MapMultiSelectionMenu menu = mapActivity.getContextMenu().getMultiSelectionMenu();

		int slideInAnim = 0;
		int slideOutAnim = 0;

		if (!mapActivity.getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
			slideInAnim = menu.getSlideInAnimation();
			slideOutAnim = menu.getSlideOutAnimation();
		}

		MapMultiSelectionMenuFragment fragment = new MapMultiSelectionMenuFragment();
		menu.getMapActivity().getSupportFragmentManager().beginTransaction()
				.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(TAG).commitAllowingStateLoss();
	}

	private MultiSelectionArrayAdapter createAdapter() {
		final List<MenuObject> items = new LinkedList<>(menu.getObjects());
		return new MultiSelectionArrayAdapter(menu, R.layout.menu_obj_list_item, items);
	}

	@Override
	public void onClick(int position) {
		MenuObject menuObject = listAdapter.getItem(position);
		if (menuObject != null) {
			menu.openContextMenu(menuObject);
		}
	}

	public void dismissMenu() {
		dismissing = true;
		if (menu.getMapActivity().getContextMenu().isVisible()) {
			menu.getMapActivity().getContextMenu().hide();
		} else {
			FragmentManager fragmentManager = menu.getMapActivity().getSupportFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack();
			}
		}
	}
}
