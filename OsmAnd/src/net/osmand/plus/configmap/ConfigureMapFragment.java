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

public class ConfigureMapFragment extends BaseOsmAndFragment implements OnDataChangeUiAdapter {

	public static final String TAG = ConfigureMapFragment.class.getSimpleName();

	private OsmandApplication app;
	private MapActivity mapActivity;
	private OsmandSettings settings;
	private ApplicationMode appMode;
	private ViewCreator viewCreator;
	Map<ContextMenuItem, List<ContextMenuItem>> items;
	private boolean nightMode;

	private final Map<Integer, View> views = new HashMap<>();
	private LinearLayout llList;
	private ListStringPreference collapsedIds;
	private View.OnClickListener itemsClickListener;

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
		appMode = settings.getApplicationMode();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		inflater = UiUtilities.getInflater(getContext(), nightMode);
		View view = inflater.inflate(R.layout.fragment_configure_map, container, false);

		viewCreator = new ViewCreator(requireActivity(), nightMode);
		viewCreator.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		viewCreator.setCustomControlsColor(appMode.getProfileColor(nightMode));
		viewCreator.setUiAdapter(this);

		llList = view.findViewById(R.id.list);
		updateList(true);
		return view;
	}

	private void updateList(boolean reset) {
		if (reset) {
			ConfigureMapMenu menu = new ConfigureMapMenu();
			ContextMenuAdapter adapter = menu.createListAdapter(mapActivity);
			CtxMenuUtils.removeHiddenItems(adapter);
			CtxMenuUtils.hideExtraDividers(adapter);
			items = CtxMenuUtils.collectItemsByCategories(adapter.getItems());
			ContextMenuItem bottomShadow = new ContextMenuItem(null).setLayout(R.layout.card_bottom_divider);
			items.put(bottomShadow, null);
			views.clear();
			llList.removeAllViews();
		}

		for (ContextMenuItem topItem : items.keySet()) {
			List<ContextMenuItem> nestedItems = items.get(topItem);
			if (topItem.isCategory() && nestedItems != null) {
				bindCategoryView(topItem, nestedItems, llList);
			} else {
				bindItemView(topItem, llList);
			}
		}
	}

	private void bindCategoryView(@NonNull ContextMenuItem category,
	                              @NonNull List<ContextMenuItem> items, @NonNull LinearLayout llList) {
		// Use the same layout for all categories views
		category.setLayout(R.layout.list_item_expandable_category);
		List<String> titles = CtxMenuUtils.getNames(items);
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

		for (ContextMenuItem item : items) {
			bindItemView(item, container);
		}
	}

	private void bindItemView(@NonNull ContextMenuItem item, @NonNull ViewGroup container) {
		int standardId = item.getTitleId();
		View existedView = views.get(standardId);
		View view = viewCreator.getView(item, existedView);
		view.setTag(R.id.item_as_tag, item);
		view.setOnClickListener(getItemsClickListener());
		if (item.getLayout() != R.layout.mode_toggles) {
			setupSelectableBg(view);
		}
		if (existedView == null) {
			views.put(standardId, view);
			container.addView(view);
		}
	}

	private void updateCategoryView(@NonNull ContextMenuItem category) {
		String id = category.getId();
		int standardId = category.getTitleId();
		View view = views.get(standardId);
		if (view == null) {
			return;
		}
		boolean isCollapsed = collapsedIds.containsValue(appMode, id);
		ImageView ivIndicator = view.findViewById(R.id.explicit_indicator);
		ivIndicator.setImageResource(isCollapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), isCollapsed);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.items_container), !isCollapsed);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), !isCollapsed);
	}

	private void expand(@NonNull ContextMenuItem category) {
		collapsedIds.removeValueForProfile(appMode, category.getId());
		updateCategoryView(category);
	}

	private void collapse(@NonNull ContextMenuItem category) {
		collapsedIds.addModeValue(appMode, category.getId());
		updateCategoryView(category);
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
	public void onDataSetChanged() {
		updateList(false);
	}

	@Override
	public void onDataSetInvalidated() {
		updateList(true);
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new ConfigureMapFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}

}
