package net.osmand.plus.views.mapwidgets.configure.reorder;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.DEFAULT_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.dialogs.AddWidgetFragment;
import net.osmand.plus.views.mapwidgets.configure.dialogs.AddWidgetFragment.AddWidgetListener;
import net.osmand.plus.views.mapwidgets.configure.panel.ConfigureWidgetsFragment;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsConfigurationChangeListener;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ItemType;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ListItem;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.WidgetAdapterDragListener;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.WidgetsAdapterActionsListener;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.ActionButtonViewHolder.ActionButtonInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AddedWidgetViewHolder.AddedWidgetUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AvailableItemViewHolder.AvailableWidgetUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.PageViewHolder.PageUiInfo;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ReorderWidgetsFragment extends BaseOsmAndFragment implements
		CopyAppModePrefsListener, AddWidgetListener {

	public static final String TAG = ReorderWidgetsFragment.class.getSimpleName();

	private static final String APP_MODE_ATTR = "app_mode_key";

	private MapWidgetRegistry widgetRegistry;

	private ApplicationMode selectedAppMode;
	private final WidgetsDataHolder dataHolder = new WidgetsDataHolder();

	private View view;
	private Toolbar toolbar;
	private RecyclerView recyclerView;
	private ReorderWidgetsAdapter adapter;


	public void setSelectedAppMode(@NonNull ApplicationMode selectedAppMode) {
		this.selectedAppMode = selectedAppMode;
	}

	public void setSelectedPanel(@NonNull WidgetsPanel selectedPanel) {
		dataHolder.setSelectedPanel(selectedPanel);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();

		if (savedInstanceState != null) {
			String appModeKey = savedInstanceState.getString(APP_MODE_ATTR);
			selectedAppMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
			dataHolder.restoreData(savedInstanceState);
		} else {
			dataHolder.initOrders(requireMapActivity(), app, selectedAppMode);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		view = themedInflater.inflate(R.layout.fragment_reorder_widgets, container, false);
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		}

		toolbar = view.findViewById(R.id.toolbar);
		recyclerView = view.findViewById(R.id.content_list);

		setupToolbar();
		setupContent();
		setupApplyButton();

		return view;
	}

	private void setupToolbar() {
		TextView title = toolbar.findViewById(R.id.toolbar_title);
		TextView subtitle = toolbar.findViewById(R.id.toolbar_subtitle);

		title.setText(dataHolder.getSelectedPanel().getTitleId(AndroidUtils.isLayoutRtl(app)));
		subtitle.setText(selectedAppMode.toHumanString());

		toolbar.findViewById(R.id.back_button).setOnClickListener(v -> dismiss());
		toolbar.findViewById(R.id.reset_button).setOnClickListener(v -> resetToDefault());
		toolbar.findViewById(R.id.copy_button).setOnClickListener(v -> copyFromProfile());
	}

	private void setupContent() {
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		adapter = new ReorderWidgetsAdapter(app, dataHolder, nightMode);

		ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(adapter));
		touchHelper.attachToRecyclerView(recyclerView);

		adapter.setDragListener(new WidgetAdapterDragListener() {
			private int fromPosition;

			@Override
			public void onDragStarted(ViewHolder holder) {
				fromPosition = holder.getAdapterPosition();
				touchHelper.startDrag(holder);
			}

			@Override
			public void onDragOrSwipeEnded(ViewHolder holder) {
				int toPosition = holder.getAdapterPosition();
				if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
					adapter.notifyDataSetChanged();
				}
			}
		});

		adapter.setActionsListener(new WidgetsAdapterActionsListener() {

			@Override
			public void onPageDeleted(int page, int position) {
				String snackbarText = getString(R.string.snackbar_page_removed, String.valueOf(page + 1));
				Snackbar snackbar = Snackbar.make(view, snackbarText, Snackbar.LENGTH_LONG)
						.setAnchorView(R.id.apply_button_container)
						.setAction(R.string.shared_string_undo, v -> adapter.restorePage(page, position));
				UiUtilities.setupSnackbar(snackbar, nightMode);
				snackbar.show();
			}

			@Override
			public void showWidgetGroupInfo(@NonNull WidgetGroup widgetGroup, @NonNull List<String> addedGroupWidgets) {
				FragmentManager fragmentManager = getSupportFragmentManager();
				if (fragmentManager != null) {
					AddWidgetFragment.showGroupDialog(fragmentManager, ReorderWidgetsFragment.this,
							selectedAppMode, dataHolder.getSelectedPanel(), widgetGroup, addedGroupWidgets);
				}
			}

			@Override
			public void showWidgetInfo(@NonNull WidgetType widget) {
				FragmentManager fragmentManager = getSupportFragmentManager();
				if (fragmentManager != null) {
					AddWidgetFragment.showWidgetDialog(fragmentManager, ReorderWidgetsFragment.this,
							selectedAppMode, dataHolder.getSelectedPanel(), widget, Collections.emptyList());
				}
			}

			@Override
			public void showExternalWidgetIndo(@NonNull String widgetId, @NonNull String externalProviderPackage) {
				FragmentManager fragmentManager = getSupportFragmentManager();
				if (fragmentManager != null) {
					AddWidgetFragment.showExternalWidgetDialog(fragmentManager, ReorderWidgetsFragment.this,
							selectedAppMode, dataHolder.getSelectedPanel(), widgetId, externalProviderPackage,
							Collections.emptyList());
				}
			}

			@Nullable
			private FragmentManager getSupportFragmentManager() {
				return getActivity() != null ? getActivity().getSupportFragmentManager() : null;
			}
		});

		recyclerView.setAdapter(adapter);
		updateItems();
	}

	private void setupApplyButton() {
		DialogButton applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> onApplyChanges());
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	private void copyFromProfile() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SelectCopyAppModeBottomSheet.showInstance(activity.getSupportFragmentManager(), this, selectedAppMode);
		}
	}

	private void onApplyChanges() {
		applyWidgetsConfiguration();

		Fragment fragment = getTargetFragment();
		if (fragment instanceof WidgetsConfigurationChangeListener) {
			((WidgetsConfigurationChangeListener) fragment).onWidgetsConfigurationChanged();
		}
		dismiss();
	}

	private void updateItems() {
		List<ListItem> enabledItems = createEnabledWidgetsList(selectedAppMode, false);
		List<ListItem> availableWidgets = createAvailableWidgetsList(selectedAppMode);
		updateItems(availableWidgets, enabledItems);
	}

	private void updateItems(@NonNull List<ListItem> availableItems, @NonNull List<ListItem> enabledItems) {
		List<ListItem> items = new ArrayList<>();
		items.add(new ListItem(ItemType.CARD_TOP_DIVIDER, null));
		items.add(new ListItem(ItemType.HEADER, getString(R.string.shared_string_visible_widgets)));
		items.addAll(enabledItems);
		items.add(new ListItem(ItemType.ADD_PAGE_BUTTON, null));
		items.add(new ListItem(ItemType.CARD_DIVIDER, null));

		if (!Algorithms.isEmpty(availableItems)) {
			items.add(new ListItem(ItemType.HEADER, getString(R.string.available_widgets)));
			items.addAll(availableItems);
			items.add(new ListItem(ItemType.CARD_DIVIDER, null));
		}

		items.add(new ListItem(ItemType.HEADER, getString(R.string.shared_string_actions)));
		items.add(new ListItem(ItemType.ACTION_BUTTON, new ActionButtonInfo(
				getString(R.string.reset_to_default),
				R.drawable.ic_action_reset,
				v -> resetToDefault()
		)));
		items.add(new ListItem(ItemType.ACTION_BUTTON, new ActionButtonInfo(
				getString(R.string.copy_from_other_profile),
				R.drawable.ic_action_copy,
				v -> copyFromProfile()
		)));
		items.add(new ListItem(ItemType.CARD_BOTTOM_DIVIDER, null));
		items.add(new ListItem(ItemType.SPACE, getResources().getDimensionPixelSize(R.dimen.bottom_space_height)));
		adapter.setItems(items);
	}

	private List<ListItem> createEnabledWidgetsList(@NonNull ApplicationMode appMode, boolean newWidgetsToCreate) {
		List<ListItem> widgetsItems = new ArrayList<>();

		MapActivity mapActivity = requireMapActivity();
		WidgetsPanel selectedPanel = dataHolder.getSelectedPanel();
		List<Set<MapWidgetInfo>> widgets = widgetRegistry.getPagedWidgetsForPanel(mapActivity, appMode, selectedPanel, AVAILABLE_MODE | MATCHING_PANELS_MODE);
		for (int pageIndex = 0; pageIndex < widgets.size(); pageIndex++) {
			for (MapWidgetInfo widgetInfo : widgets.get(pageIndex)) {
				boolean enabled = dataHolder.getOrders().containsKey(widgetInfo.key);
				if (!enabled) {
					continue;
				}

				Map<String, Integer> orders = dataHolder.getOrders();
				int page = dataHolder.getWidgetPage(widgetInfo.key);
				Integer order = orders.get(widgetInfo.key);
				if (page == -1) {
					page = pageIndex;
					dataHolder.addWidgetToPage(widgetInfo.key, page);
				}
				if (order == null) {
					order = widgetInfo.priority;
					orders.put(widgetInfo.key, order);
				}
				AddedWidgetUiInfo info = new AddedWidgetUiInfo();
				info.key = widgetInfo.key;
				info.title = widgetInfo.getTitle(app);
				info.iconId = widgetInfo.getMapIconId(nightMode);
				info.page = page;
				info.order = order;
				info.info = widgetInfo;
				info.newWidgetToCreate = newWidgetsToCreate;
				widgetsItems.add(new ListItem(ItemType.ADDED_WIDGET, info));
			}
		}

		Collections.sort(widgetsItems, (o1, o2) -> {
			AddedWidgetUiInfo info1 = ((AddedWidgetUiInfo) o1.value);
			AddedWidgetUiInfo info2 = ((AddedWidgetUiInfo) o2.value);
			if (info1 == null || info2 == null) {
				return 0;
			} else if (info1.page != info2.page) {
				return Integer.compare(info1.page, info2.page);
			}
			return Integer.compare(info1.order, info2.order);
		});
		return getPagedWidgetItems(widgetsItems);
	}

	@NonNull
	private List<ListItem> createAvailableWidgetsList(@NonNull ApplicationMode appMode) {
		Map<Integer, ListItem> defaultWidgetsItems = new TreeMap<>();
		List<ListItem> externalWidgetsItems = new ArrayList<>();
		List<WidgetGroup> availableGroups = new ArrayList<>();

		int filter = AVAILABLE_MODE | DEFAULT_MODE;
		MapActivity mapActivity = requireMapActivity();
		WidgetsPanel selectedPanel = dataHolder.getSelectedPanel();
		Set<MapWidgetInfo> widgets = widgetRegistry.getWidgetsForPanel(mapActivity, appMode, filter, Collections.singletonList(selectedPanel));

		for (MapWidgetInfo widgetInfo : widgets) {
			if (!WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetInfo.key, selectedAppMode)) {
				continue;
			}

			WidgetType widgetType = WidgetType.getById(widgetInfo.key);
			boolean defaultWidget = widgetType != null;
			if (defaultWidget) {
				WidgetGroup group = widgetType.getGroup();
				if (group != null && !availableGroups.contains(group)) {
					availableGroups.add(group);
					defaultWidgetsItems.put(group.getOrder(), new ListItem(ItemType.AVAILABLE_GROUP, group));
				} else if (group == null) {
					AvailableWidgetUiInfo availableWidgetInfo = getWidgetInfo(widgetInfo);
					defaultWidgetsItems.put(widgetType.ordinal(), new ListItem(ItemType.AVAILABLE_WIDGET, availableWidgetInfo));
				}
			} else {
				AvailableWidgetUiInfo availableWidgetInfo = getWidgetInfo(widgetInfo);
				externalWidgetsItems.add(new ListItem(ItemType.AVAILABLE_WIDGET, availableWidgetInfo));
			}
		}

		List<ListItem> widgetItems = new ArrayList<>();
		widgetItems.addAll(defaultWidgetsItems.values());
		widgetItems.addAll(externalWidgetsItems);
		return widgetItems;
	}

	@NonNull
	private AvailableWidgetUiInfo getWidgetInfo(@NonNull MapWidgetInfo widgetInfo) {
		AvailableWidgetUiInfo info = new AvailableWidgetUiInfo();
		info.key = widgetInfo.key;
		info.title = widgetInfo.getTitle(app);
		info.iconId = widgetInfo.getMapIconId(nightMode);
		info.order = dataHolder.getSelectedPanel().getOriginalWidgetOrder(widgetInfo.key);
		info.info = widgetInfo;
		return info;
	}

	@NonNull
	private List<ListItem> getPagedWidgetItems(@NonNull List<ListItem> widgetsItems) {
		List<ListItem> pagedWidgetsItems = new ArrayList<>();
			for (int page : dataHolder.getPages().keySet()) {
				pagedWidgetsItems.add(new ListItem(ItemType.PAGE, new PageUiInfo(page)));
				for (ListItem widgetItem : widgetsItems) {
					int widgetPage = widgetItem.value instanceof AddedWidgetUiInfo
							? ((AddedWidgetUiInfo) widgetItem.value).page
							: -1;
					if (widgetPage == page) {
						pagedWidgetsItems.add(widgetItem);
					}
				}
			}
		return pagedWidgetsItems;
	}

	private void applyWidgetsConfiguration() {
		List<ListItem> items = adapter.getItems();
		Map<Integer, List<String>> pagedOrder = new TreeMap<>();
		List<String> enabledWidgetsIds = new ArrayList<>();
		List<String> newWidgetToCreate = new ArrayList<>();

		for (ListItem item : items) {
			if (item.value instanceof AddedWidgetUiInfo) {
				AddedWidgetUiInfo widgetInfo = (AddedWidgetUiInfo) item.value;
				if (widgetInfo.newWidgetToCreate) {
					newWidgetToCreate.add(widgetInfo.key);
				}
				List<String> widgetsOrder = pagedOrder.get(widgetInfo.page);
				if (widgetsOrder == null) {
					widgetsOrder = new ArrayList<>();
					pagedOrder.put(widgetInfo.page, widgetsOrder);
				}
				widgetsOrder.add(widgetInfo.key);
				enabledWidgetsIds.add(widgetInfo.key);
			}
		}

		applyWidgetsPanel(newWidgetToCreate);
		applyWidgetsVisibility(enabledWidgetsIds);
		applyWidgetsOrder(new ArrayList<>(pagedOrder.values()));
		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateControls();
		}
	}

	private void applyWidgetsPanel(@NonNull List<String> enabledWidgetsIds) {
		WidgetsPanel currentPanel = dataHolder.getSelectedPanel();
		Fragment fragment = getTargetFragment();
		if (fragment instanceof ConfigureWidgetsFragment) {
			((ConfigureWidgetsFragment) fragment).onWidgetsSelectedToAdd(enabledWidgetsIds, currentPanel, false);
		}
	}

	private void applyWidgetsVisibility(@NonNull List<String> enabledWidgetsIds) {
		WidgetsPanel panel = dataHolder.getSelectedPanel();
		for (MapWidgetInfo widget : widgetRegistry.getWidgetsForPanel(panel)) {
			boolean enabledFromApply = enabledWidgetsIds.contains(widget.key);
			if (widget.isEnabledForAppMode(selectedAppMode) != enabledFromApply) {
				widgetRegistry.enableDisableWidgetForMode(selectedAppMode, widget, enabledFromApply, false);
			}
		}
	}

	private void applyWidgetsOrder(@NonNull List<List<String>> pagedOrder) {
		WidgetsPanel selectedPanel = dataHolder.getSelectedPanel();
		selectedPanel.setWidgetsOrder(selectedAppMode, pagedOrder, settings);
		widgetRegistry.reorderWidgets();
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		MapActivity mapActivity = requireMapActivity();
		dataHolder.copyAppModePrefs(mapActivity, selectedAppMode, appMode);
		List<ListItem> enabledItems = createEnabledWidgetsList(appMode, true);
		List<ListItem> availableItems = createAvailableWidgetsList(appMode);
		updateItems(availableItems, enabledItems);
	}

	private void resetToDefault() {
		dataHolder.resetToDefault(app, selectedAppMode);
		updateItems();
	}

	@Override
	public void onWidgetsSelectedToAdd(@NonNull List<String> widgetsIds, @NonNull WidgetsPanel widgetsPanel, boolean recreateControls) {
		for (String widgetId : widgetsIds) {
			MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
			if (widgetInfo != null) {
				adapter.addWidget(widgetInfo);
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		dataHolder.onSaveInstanceState(outState);
		outState.putString(APP_MODE_ATTR, selectedAppMode.getStringKey());
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@NonNull
	public MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (!(activity instanceof MapActivity)) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return (MapActivity) activity;
	}

	public static void showInstance(@NonNull FragmentManager manager,
									@NonNull WidgetsPanel panel,
									@NonNull ApplicationMode appMode,
									@Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ReorderWidgetsFragment fragment = new ReorderWidgetsFragment();
			fragment.setTargetFragment(target, 0);
			fragment.setSelectedPanel(panel);
			fragment.setSelectedAppMode(appMode);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}