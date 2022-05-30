package net.osmand.plus.configmap;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.CtxMenuUtils;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_STYLE_ID;

public class ConfigureMapFragment extends BaseOsmAndFragment implements OnDataChangeUiAdapter {

	public static final String TAG = ConfigureMapFragment.class.getSimpleName();

	private OsmandApplication app;
	private MapActivity mapActivity;
	private OsmandSettings settings;
	private ApplicationMode appMode;

	private ContextMenuAdapter adapter;
	private Map<ContextMenuItem, List<ContextMenuItem>> menuItems;
	private ViewCreator viewCreator;

	private LinearLayout llList;
	private ListStringPreference collapsedIds;
	private View.OnClickListener itemsClickListener;
	private final Map<Integer, View> views = new HashMap<>();

	private boolean nightMode;
	private boolean useAnimation;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		mapActivity = (MapActivity) getMyActivity();
		settings = app.getSettings();
		collapsedIds = settings.COLLAPSED_CONFIGURE_MAP_CATEGORIES;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_configure_map, container, false);
		llList = view.findViewById(R.id.list);
		onDataSetInvalidated();
		return view;
	}

	@Override
	public void onDataSetChanged() {
		updateItemsData();
		updateItemsView();
	}

	@Override
	public void onDataSetInvalidated() {
		recreateView();
	}

	private void recreateView() {
		appMode = settings.getApplicationMode();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		useAnimation = !settings.DO_NOT_USE_ANIMATIONS.getModeValue(appMode);

		viewCreator = new ViewCreator(requireActivity(), nightMode);
		viewCreator.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		viewCreator.setCustomControlsColor(appMode.getProfileColor(nightMode));
		viewCreator.setUiAdapter(this);

		int bgColor = ColorUtilities.getActivityBgColor(app, nightMode);
		llList.setBackgroundColor(bgColor);

		views.clear();
		llList.removeAllViews();
		updateItemsData();
		updateItemsView();
	}

	private void updateItemsData() {
		ConfigureMapMenu menu = new ConfigureMapMenu();
		adapter = menu.createListAdapter(mapActivity);
		CtxMenuUtils.removeHiddenItems(adapter);
		CtxMenuUtils.hideExtraDividers(adapter);
		menuItems = CtxMenuUtils.collectItemsByCategories(adapter.getItems());

		ContextMenuItem bottomShadow = new ContextMenuItem(null);
		bottomShadow.setLayout(R.layout.card_bottom_divider);
		menuItems.put(bottomShadow, null);
	}

	private void updateItemsView() {
		for (ContextMenuItem topItem : menuItems.keySet()) {
			List<ContextMenuItem> nestedItems = menuItems.get(topItem);
			if (topItem.isCategory() && nestedItems != null) {
				bindCategoryView(topItem, nestedItems);
			} else {
				bindItemView(topItem, llList);
			}
		}
	}

	private void bindCategoryView(@NonNull ContextMenuItem category,
	                              @NonNull List<ContextMenuItem> nestedItems) {
		// Use the same layout for all categories views
		category.setLayout(R.layout.list_item_expandable_category);
		List<String> titles = CtxMenuUtils.getNames(nestedItems);
		String description = TextUtils.join(", ", titles);
		category.setDescription(description);

		String id = category.getId();
		int standardId = category.getTitleId();
		View existedView = views.get(standardId);

		View view = viewCreator.getView(category, existedView);
		LinearLayout container = view.findViewById(R.id.items_container);
		view.setClickable(true);
		view.setFocusable(true);
		if (existedView == null) {
			views.put(standardId, view);
			llList.addView(view);
		}
		updateCategoryView(category);

		View btnView = view.findViewById(R.id.button_container);
		setupSelectableBg(btnView);
		btnView.setOnClickListener(v -> {
			boolean isCollapsed = collapsedIds.containsValue(appMode, id);
			if (isCollapsed) {
				expand(category);
			} else {
				collapse(category);
			}
		});

		for (ContextMenuItem item : nestedItems) {
			bindItemView(item, container);
		}
	}

	private void bindItemView(@NonNull ContextMenuItem item, @NonNull ViewGroup container) {
		int standardId = item.getTitleId();
		View existedView = views.get(standardId);
		View view = viewCreator.getView(item, existedView);
		view.setTag(R.id.item_as_tag, item);
		view.setOnClickListener(getItemsClickListener());
		if (existedView == null) {
			views.put(standardId, view);
			container.addView(view);
			if (item.getLayout() != R.layout.mode_toggles) {
				setupSelectableBg(view);
			}
		}
	}

	private void updateCategoryView(@NonNull ContextMenuItem category) {
		View view = views.get(category.getTitleId());
		if (view != null) {
			String id = category.getId();
			boolean isCollapsed = collapsedIds.containsValue(appMode, id);

			ImageView ivIndicator = view.findViewById(R.id.explicit_indicator);
			ivIndicator.setImageResource(isCollapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up);

			AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), isCollapsed);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.items_container), !isCollapsed);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), !isCollapsed);
		}
	}

	private void expand(@NonNull ContextMenuItem category) {
		collapsedIds.removeValueForProfile(appMode, category.getId());
		if (useAnimation) {
			View view = views.get(category.getTitleId());
			CategoryAnimator.startExpanding(category, Objects.requireNonNull(view));
		} else {
			updateCategoryView(category);
		}
	}

	private void collapse(@NonNull ContextMenuItem category) {
		collapsedIds.addModeValue(appMode, category.getId());
		if (useAnimation) {
			View view = views.get(category.getTitleId());
			CategoryAnimator.startCollapsing(category, Objects.requireNonNull(view));
		} else {
			updateCategoryView(category);
		}
	}

	public View.OnClickListener getItemsClickListener() {
		if (itemsClickListener == null) {
			itemsClickListener = v -> {
				ContextMenuItem item = (ContextMenuItem) v.getTag(R.id.item_as_tag);
				ItemClickListener click = item.getItemClickListener();
				DashboardOnMap dashboard = mapActivity.getDashboard();
				if (click instanceof OnRowItemClick) {
					boolean cl = ((OnRowItemClick) click).onRowItemClick(this, v, item);
					if (cl) {
						dashboard.hideDashboard();
					}
				} else if (click != null) {
					CompoundButton btn = v.findViewById(R.id.toggle_item);
					if (btn != null && btn.getVisibility() == View.VISIBLE) {
						btn.setChecked(!btn.isChecked());
					} else if (click.onContextMenuClick(this, v, item, false)) {
						dashboard.hideDashboard();
					}
				} else if (!item.isCategory()) {
					dashboard.hideDashboard();
				}
			};
		}
		return itemsClickListener;
	}

	public void onMapStyleChanged() {
		ContextMenuItem item = adapter.getItemById(MAP_STYLE_ID);
		if (item != null) {
			item.refreshWithActualData();
			View view = views.get(item.getTitleId());
			if (view != null) {
				bindItemView(item, llList);
			}
		}
	}

	private void setupSelectableBg(@NonNull View view) {
		int profileColor = appMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, profileColor, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	@Nullable
	public static ConfigureMapFragment getVisibleInstance(@NonNull MapActivity mapActivity) {
		FragmentManager fm = mapActivity.getSupportFragmentManager();;
		return (ConfigureMapFragment) fm.findFragmentByTag(TAG);
	}

	public static void showInstance(@NonNull FragmentManager fm) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			fm.beginTransaction()
					.replace(R.id.content, new ConfigureMapFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}

}
