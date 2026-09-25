package net.osmand.plus.configmap;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.GPX_FILES_ID;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.track.helpers.SelectGpxTask.SelectGpxTaskListener;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuUtils;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfigureMapFragment extends BaseOsmAndFragment implements OnDataChangeUiAdapter,
		InAppPurchaseListener, SelectGpxTaskListener {

	public static final String TAG = ConfigureMapFragment.class.getSimpleName();

	private MapActivity mapActivity;
	private ApplicationMode appMode;

	private ContextMenuAdapter adapter;
	private Map<ContextMenuItem, List<ContextMenuItem>> items;
	private ViewCreator viewCreator;

	private LinearLayout itemsContainer;
	private ListStringPreference collapsedIds;
	private View.OnClickListener itemsClickListener;
	private final Map<Integer, View> views = new HashMap<>();

	private boolean useAnimation;

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mapActivity = (MapActivity) getMyActivity();
		collapsedIds = settings.COLLAPSED_CONFIGURE_MAP_CATEGORIES;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflater.inflate(R.layout.fragment_configure_map, container, false);
		itemsContainer = view.findViewById(R.id.list);
		onDataSetInvalidated();
		return view;
	}

	@Override
	public void onDataSetChanged() {
		updateItemsView();
	}

	@Override
	public void onDataSetInvalidated() {
		recreateView();
	}

	@Override
	public void onRefreshItem(@NonNull String itemId) {
		ContextMenuItem item = adapter.getItemById(itemId);
		if (item != null) {
			item.refreshWithActualData();
			View view = views.get(item.getTitleId());
			if (view != null) {
				bindItemView(item, itemsContainer);
			}
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		recreateView();
	}

	@Override
	public void onGpxSelectionInProgress(@NonNull SelectedGpxFile selectedGpxFile) {
		onRefreshItem(GPX_FILES_ID);
	}

	@Override
	public void onGpxSelectionFinished() {
		onRefreshItem(GPX_FILES_ID);
	}

	private void recreateView() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			appMode = settings.getApplicationMode();
			useAnimation = !settings.DO_NOT_USE_ANIMATIONS.getModeValue(appMode);

			updateNightMode();
			viewCreator = new ViewCreator(activity, nightMode);
			viewCreator.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
			viewCreator.setCustomControlsColor(appMode.getProfileColor(nightMode));
			viewCreator.setUiAdapter(this);

			views.clear();
			itemsContainer.removeAllViews();
			itemsContainer.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));

			updateItemsData();
			updateItemsView();
		}
	}

	private void updateItemsData() {
		ConfigureMapMenu menu = new ConfigureMapMenu(app);
		adapter = menu.createListAdapter(mapActivity);
		ContextMenuUtils.removeHiddenItems(adapter);
		ContextMenuUtils.hideExtraDividers(adapter);
		items = ContextMenuUtils.collectItemsByCategories(adapter.getItems());
		ContextMenuItem bottomShadow = new ContextMenuItem(null);
		bottomShadow.setLayout(R.layout.card_bottom_divider);
		items.put(bottomShadow, null);
	}

	private void updateItemsView() {
		for (ContextMenuItem topItem : items.keySet()) {
			List<ContextMenuItem> nestedItems = items.get(topItem);
			if (topItem.isCategory() && nestedItems != null) {
				bindCategoryView(topItem, nestedItems);
			} else {
				bindItemView(topItem, itemsContainer);
			}
		}
	}

	private void bindCategoryView(@NonNull ContextMenuItem category,
	                              @NonNull List<ContextMenuItem> nestedItems) {
		// Use the same layout for all categories views
		category.setLayout(R.layout.list_item_expandable_category);
		category.setDescription(ContextMenuUtils.getCategoryDescription(nestedItems));

		String id = category.getId();
		int standardId = category.getTitleId();
		View existedView = views.get(standardId);

		View view = viewCreator.getView(category, existedView);
		LinearLayout container = view.findViewById(R.id.items_container);
		view.setClickable(true);
		view.setFocusable(true);
		if (existedView == null) {
			views.put(standardId, view);
			itemsContainer.addView(view);
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

		boolean clickable = item.isClickable();
		if (clickable) {
			view.setOnClickListener(getItemsClickListener());
		}
		if (existedView == null) {
			views.put(standardId, view);
			container.addView(view);
			if (item.getLayout() != R.layout.mode_toggles && clickable) {
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

			boolean hasDescription = category.getDescription() != null;
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), isCollapsed && hasDescription);
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

	private void setupSelectableBg(@NonNull View view) {
		int profileColor = appMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, profileColor, 0.3f);
		AndroidUtils.setBackground(view, background);
	}

	@Override
	public void onResume() {
		super.onResume();
		app.getSelectedGpxHelper().addListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		app.getSelectedGpxHelper().removeListener(this);
	}

	@Nullable
	public static ConfigureMapFragment getVisibleInstance(@NonNull MapActivity mapActivity) {
		FragmentManager fm = mapActivity.getSupportFragmentManager();
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
