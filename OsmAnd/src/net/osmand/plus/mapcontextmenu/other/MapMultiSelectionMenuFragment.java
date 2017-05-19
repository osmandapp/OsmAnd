package net.osmand.plus.mapcontextmenu.other;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu.MenuObject;

import java.util.LinkedList;
import java.util.List;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class MapMultiSelectionMenuFragment extends Fragment implements AdapterView.OnItemClickListener {
	public static final String TAG = "MapMultiSelectionMenuFragment";

	private View view;
	private ArrayAdapter<MenuObject> listAdapter;
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
					R.drawable.bg_left_menu_light, R.drawable.bg_left_menu_dark);
		} else {
			AndroidUtils.setBackground(view.getContext(), view, !menu.isLight(),
					R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		ListView listView = (ListView) view.findViewById(R.id.list);
		listAdapter = createAdapter();
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(this);

		if (!oldAndroid()) {
			runLayoutListener();
		}
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

		int slideInAnim = menu.getSlideInAnimation();
		int slideOutAnim = menu.getSlideOutAnimation();

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

				int maxHeight = (int) (getScreenHeight() * menu.getHalfScreenMaxHeightKoef());
				int height = view.findViewById(R.id.main_view).getHeight();

				ViewTreeObserver obs = view.getViewTreeObserver();

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}

				if (!menu.isLandscapeLayout() && height > maxHeight) {
					ViewGroup.LayoutParams lp = view.getLayoutParams();
					lp.height = maxHeight;
					view.setLayoutParams(lp);
					view.requestLayout();
				}
			}
		});
	}

	private ArrayAdapter<MenuObject> createAdapter() {
		final List<MenuObject> items = new LinkedList<>(menu.getObjects());
		return new ArrayAdapter<MenuObject>(menu.getMapActivity(), R.layout.menu_obj_list_item, items) {

			@SuppressLint("InflateParams")
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				View v = convertView;
				if (v == null) {
					v = menu.getMapActivity().getLayoutInflater().inflate(R.layout.menu_obj_list_item, null);
				}
				final MenuObject item = getItem(position);
				buildHeader(v, item, menu.getMapActivity());
				return v;
			}
		};
	}

	private void buildHeader(View view, MenuObject item, MapActivity mapActivity) {

		AndroidUtils.setBackground(mapActivity, view, !menu.isLight(), R.drawable.expandable_list_item_background_light, R.drawable.expandable_list_item_background_dark);
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		final View iconLayout = view.findViewById(R.id.context_menu_icon_layout);
		final ImageView iconView = (ImageView) view.findViewById(R.id.context_menu_icon_view);
		Drawable icon = item.getLeftIcon();
		int iconId = item.getLeftIconId();
		if (icon != null) {
			iconView.setImageDrawable(icon);
			iconLayout.setVisibility(View.VISIBLE);
		} else if (iconId != 0) {
			iconView.setImageDrawable(iconsCache.getIcon(iconId,
					menu.isLight() ? R.color.osmand_orange : R.color.osmand_orange_dark));
			iconLayout.setVisibility(View.VISIBLE);
		} else {
			iconLayout.setVisibility(View.GONE);
		}

		// Text line 1
		TextView line1 = (TextView) view.findViewById(R.id.context_menu_line1);
		AndroidUtils.setTextPrimaryColor(mapActivity, line1, !menu.isLight());
		line1.setText(item.getTitleStr());

		// Text line 2
		TextView line2 = (TextView) view.findViewById(R.id.context_menu_line2);
		AndroidUtils.setTextSecondaryColor(mapActivity, line2, !menu.isLight());
		line2.setText(item.getTypeStr());
		Drawable slIcon = item.getTypeIcon();
		line2.setCompoundDrawablesWithIntrinsicBounds(slIcon, null, null, null);
		line2.setCompoundDrawablePadding(dpToPx(5f));
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		menu.openContextMenu(listAdapter.getItem(position));
	}

	public void dismissMenu() {
		dismissing = true;
		if (menu.getMapActivity().getContextMenu().isVisible()) {
			menu.getMapActivity().getContextMenu().hide();
		} else {
			menu.getMapActivity().getSupportFragmentManager().popBackStack();
		}
	}

	private int dpToPx(float dp) {
		Resources r = getActivity().getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}

	private int getScreenHeight() {
		DisplayMetrics dm = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	private boolean oldAndroid() {
		return (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH);
	}
}
