package net.osmand.plus.mapcontextmenu.other;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu.MenuObject;
import net.osmand.plus.widgets.TextViewEx;

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
			AndroidUtils.setBackground(view.getContext(), view, !menu.isLight(),
					R.drawable.multi_selection_menu_bg_light, R.drawable.multi_selection_menu_bg_dark);
		}

		ListView listView = (ListView) view.findViewById(R.id.list);
		if (menu.isLandscapeLayout() && Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(getActivity(), listView);
		}
		View headerView = inflater.inflate(R.layout.menu_obj_selection_header, listView, false);
		headerView.setOnClickListener(null);
		listView.addHeaderView(headerView);
		listAdapter = createAdapter();
		listAdapter.setListener(this);
		listView.setAdapter(listAdapter);

		runLayoutListener();

		view.findViewById(R.id.divider).setBackgroundColor(ContextCompat.getColor(getContext(), menu.isLight() ? R.color.multi_selection_menu_divider_light : R.color.multi_selection_menu_divider_dark));

		((TextView) view.findViewById(R.id.cancel_row_text)).setTextColor(ContextCompat.getColor(getContext(), menu.isLight() ? R.color.multi_selection_menu_close_btn_light : R.color.multi_selection_menu_close_btn_dark));
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
		if (MapRouteInfoMenu.isVisible()) {
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

	private void runLayoutListener() {
		ViewTreeObserver vto = view.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {

				if (!menu.isLandscapeLayout() && listAdapter.getCount() > 3) {
					View contentView = view.findViewById(R.id.content);
					float headerHeight = contentView.getResources().getDimension(R.dimen.multi_selection_header_height);
					float cancelRowHeight = contentView.getResources().getDimension(R.dimen.bottom_sheet_cancel_button_height);
					int maxHeight = (int) (headerHeight + cancelRowHeight);
					for (int i = 0; i < 3; i++) {
						View childView = listAdapter.getView(0, null, (ListView) contentView.findViewById(R.id.list));
						childView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
						maxHeight += childView.getMeasuredHeight();
					}
					int height = contentView.getHeight();

					if (height > maxHeight) {
						ViewGroup.LayoutParams lp = contentView.getLayoutParams();
						lp.height = maxHeight;
						contentView.setLayoutParams(lp);
						contentView.requestLayout();
					}
				}

				ViewTreeObserver obs = view.getViewTreeObserver();

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
			}
		});
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
			menu.getMapActivity().getSupportFragmentManager().popBackStack();
		}
	}
}
