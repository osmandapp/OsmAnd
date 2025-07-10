package net.osmand.plus.views.mapwidgets.configure.panel;

import static androidx.recyclerview.widget.DiffUtil.calculateDiff;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;
import static net.osmand.plus.views.mapwidgets.WidgetType.isComplexWidget;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.VIEW_TYPE_ADD_PAGE;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.VIEW_TYPE_DIVIDER;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.VIEW_TYPE_EMPTY_STATE;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.VIEW_TYPE_SPACE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil.Callback;
import androidx.recyclerview.widget.DiffUtil.DiffResult;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.WidgetsSettingsHelper;
import net.osmand.plus.views.mapwidgets.configure.dialogs.AddWidgetFragment;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.PageItem;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.WidgetItem;
import net.osmand.plus.views.mapwidgets.configure.settings.WidgetInfoBaseFragment;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class WidgetsListFragment extends Fragment implements ConfirmationBottomSheet.ConfirmationDialogListener,
		SelectCopyAppModeBottomSheet.CopyAppModePrefsListener, WidgetsListAdapter.WidgetsAdapterListener, AddWidgetFragment.AddWidgetListener {

	private static final String SELECTED_PANEL_KEY = "selected_panel_key";

	private OsmandApplication app;
	private OsmandSettings settings;
	private MapWidgetRegistry widgetRegistry;
	private ConfigureWidgetsController controller;

	private WidgetsPanel selectedPanel;
	private ApplicationMode selectedAppMode;
	private List<List<MapWidgetInfo>> originalWidgetsData;

	private RecyclerView recyclerView;
	private WidgetsListAdapter adapter;

	private boolean nightMode;

	public void setSelectedPanel(@NonNull WidgetsPanel panel) {
		this.selectedPanel = panel;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireContext().getApplicationContext();
		settings = app.getSettings();
		nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		selectedAppMode = settings.getApplicationMode();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();

		DialogManager dialogManager = app.getDialogManager();
		controller = (ConfigureWidgetsController) dialogManager.findController(ConfigureWidgetsController.PROCESS_ID);
		if (savedInstanceState != null) {
			selectedPanel = WidgetsPanel.valueOf(savedInstanceState.getString(SELECTED_PANEL_KEY));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = inflater.inflate(R.layout.fragment_widgets_list, container, false);

		recyclerView = view.findViewById(R.id.recycler_view);
		setupRecyclerView();

		return view;
	}

	private void setupRecyclerView() {
		originalWidgetsData = getPagedWidgets();

		adapter = new WidgetsListAdapter(requireMapActivity(), nightMode, this, selectedPanel.isPanelVertical(), selectedAppMode);

		recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
		recyclerView.setAdapter(adapter);
		adapter.attachToRecyclerView(recyclerView);

		boolean disableAnimation = app.getSettings().DO_NOT_USE_ANIMATIONS.getModeValue(selectedAppMode);
		if (disableAnimation) {
			recyclerView.setItemAnimator(null);
		}

		if (isEditMode()) {
			List<Object> savedList = controller.getReorderList();
			if (!Algorithms.isEmpty(savedList)) {
				updateAdapter(new ArrayList<>(savedList), false);
			}
		} else {
			updateWidgetItems(originalWidgetsData, false);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		Fragment fragment = getParentFragment();
		if (fragment instanceof ConfigureWidgetsFragment) {
			((ConfigureWidgetsFragment) fragment).setSelectedFragment(this);
		}
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onPause() {
		super.onPause();
		Fragment fragment = getParentFragment();
		if (fragment instanceof ConfigureWidgetsFragment) {
			((ConfigureWidgetsFragment) fragment).setSelectedFragment(null);
		}
	}

	@NonNull
	private List<List<MapWidgetInfo>> getPagedWidgets() {
		MapActivity mapActivity = requireMapActivity();

		List<List<MapWidgetInfo>> result = new ArrayList<>();
		int enabledWidgetsFilter = AVAILABLE_MODE | ENABLED_MODE | MATCHING_PANELS_MODE;
		for (Set<MapWidgetInfo> set : widgetRegistry.getPagedWidgetsForPanel(mapActivity, selectedAppMode, selectedPanel, enabledWidgetsFilter)) {
			result.add(new ArrayList<>(set));
		}
		return result;
	}

	public void onApplyChanges() {
		applyWidgetsConfiguration();

		Fragment fragment = getParentFragment();
		if (fragment instanceof WidgetsConfigurationChangeListener) {
			((WidgetsConfigurationChangeListener) fragment).onWidgetsConfigurationChanged();
		}
	}

	@NonNull
	private List<MapWidgetInfo> getFlatWidgetsList(@NonNull List<List<MapWidgetInfo>> widgetsData) {
		List<MapWidgetInfo> flatWidgetsList = new ArrayList<>();
		for (List<MapWidgetInfo> page : widgetsData) {
			if (!Algorithms.isEmpty(page)) {
				flatWidgetsList.addAll(page);
			}
		}
		return flatWidgetsList;
	}

	private void applyWidgetsConfiguration() {
		List<List<MapWidgetInfo>> widgetsData = adapter.getWidgetsData();
		List<String> enabledWidgetsIds = new ArrayList<>();
		List<MapWidgetInfo> newWidgetToCreate = new ArrayList<>();

		for (MapWidgetInfo widgetInfo : getFlatWidgetsList(widgetsData)) {
			String key = widgetInfo.key;
			MapWidgetInfo info = widgetRegistry.getWidgetInfoById(key);
			if (info == null) {
				newWidgetToCreate.add(widgetInfo);
			}
			enabledWidgetsIds.add(key);
		}

		applyWidgetsPanel(newWidgetToCreate);
		applyWidgetsVisibility(enabledWidgetsIds);

		List<List<String>> orderList = new ArrayList<>();
		for (List<MapWidgetInfo> page : widgetsData) {
			List<String> orderIds = new ArrayList<>();
			for (MapWidgetInfo widgetInfo : page) {
				orderIds.add(widgetInfo.key);
			}
			orderList.add(orderIds);
		}

		applyWidgetsOrder(orderList);

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateControls();
		}
	}

	private void applyWidgetsPanel(@NonNull List<MapWidgetInfo> enabledWidgetsIds) {
		Fragment fragment = getParentFragment();
		if (fragment instanceof ConfigureWidgetsFragment) {
			((ConfigureWidgetsFragment) fragment).createWidgets(enabledWidgetsIds);
		}
	}

	private void applyWidgetsVisibility(@NonNull List<String> enabledWidgetsIds) {
		WidgetsPanel panel = selectedPanel;
		for (MapWidgetInfo widget : widgetRegistry.getWidgetsForPanel(panel)) {
			boolean enabledFromApply = enabledWidgetsIds.contains(widget.key);
			if (widget.isEnabledForAppMode(selectedAppMode) != enabledFromApply) {
				widgetRegistry.enableDisableWidgetForMode(selectedAppMode, widget, enabledFromApply, false);
			}
		}
	}

	private void applyWidgetsOrder(@NonNull List<List<String>> pagedOrder) {
		selectedPanel.setWidgetsOrder(selectedAppMode, pagedOrder, settings);
		widgetRegistry.reorderWidgets();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_PANEL_KEY, selectedPanel.name());
		Fragment parent = getParentFragment();
		if (parent instanceof ConfigureWidgetsFragment configureWidgetsFragment) {
			if (isEditMode() && selectedPanel == configureWidgetsFragment.getSelectedPanel()) {
				controller.setReorderList(adapter.getItems());
			}
		}
	}

	@NonNull
	public MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (!(activity instanceof MapActivity)) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return (MapActivity) activity;
	}

	@Override
	public void onPageDeleted(int position, Object item) {
		if (adapter != null) {
			adapter.removeItem(position);
		}
	}

	@Override
	public void onWidgetDeleted(int position, Object item) {
		if (adapter != null) {
			adapter.removeItem(position);
		}
	}

	@Override
	public void onAddPageClicked() {
		if (adapter != null) {
			adapter.addPage();
		}
	}

	@Override
	public void onWidgetClick(@NonNull MapWidgetInfo widgetInfo) {
		WidgetType widgetType = widgetInfo.getWidgetType();

		WidgetInfoBaseFragment settingsBaseFragment = widgetType.getSettingsFragment(app, widgetInfo);
		if (settingsBaseFragment != null) {
			FragmentManager manager = requireMapActivity().getSupportFragmentManager();
			WidgetInfoBaseFragment.showFragment(manager, settingsBaseFragment, requireParentFragment(), selectedAppMode, widgetInfo.key, selectedPanel);
		}
	}

	@Override
	public boolean isEditMode() {
		Fragment fragment = getParentFragment();
		if (fragment instanceof ConfigureWidgetsFragment configureWidgetsFragment) {
			return configureWidgetsFragment.isEditMode;
		}
		return false;
	}

	@Override
	public void onMoveStarted() {
		adapter.actionStarted();
	}

	private void updatePageIndexes(@NonNull List<Object> items) {
		int pageNumber = 1;
		for (Object item : items) {
			if (item instanceof PageItem pageItem) {
				pageItem.pageNumber = pageNumber;
				pageNumber++;
			}
		}
	}

	@Override
	public void refreshAll() {
		updatePageIndexes(adapter.getItems());
		List<Object> updatedNewItems = duplicateItems(adapter.getItems());
		updateAdapter(updatedNewItems, true);
	}

	@Override
	public void restoreBackup() {
		List<Object> updatedNewItems = duplicateItems(adapter.getBackupItems());
		updateAdapter(updatedNewItems, true);
	}

	@NonNull
	private List<Object> duplicateItems(@NonNull List<Object> itemsToDuplicate) {
		List<Object> duplicatedItems = new ArrayList<>();
		for (Object object : itemsToDuplicate) {
			if (object instanceof PageItem pageItem) {
				PageItem newPageItem = new PageItem(pageItem.pageNumber);
				newPageItem.deleteMessage = pageItem.deleteMessage;
				newPageItem.movable = pageItem.movable;
				duplicatedItems.add(newPageItem);
			} else if (object instanceof WidgetItem widgetItem) {
				WidgetItem newWidgetItem = new WidgetItem(widgetItem.mapWidgetInfo);
				newWidgetItem.showBottomDivider = widgetItem.showBottomDivider;
				duplicatedItems.add(newWidgetItem);
			} else {
				duplicatedItems.add(object);
			}
		}
		return duplicatedItems;
	}

	@Override
	public void addNewWidget() {
		Fragment fragment = getParentFragment();
		if (fragment instanceof ConfigureWidgetsFragment) {
			((ConfigureWidgetsFragment) fragment).addNewWidget();
		}
	}

	public void addWidget(@NonNull MapWidgetInfo widgetInfo) {
		adapter.addWidget(widgetInfo);
	}

	public void reloadWidgets() {
		if (adapter != null) {
			List<List<MapWidgetInfo>> newData = getPagedWidgets();
			originalWidgetsData = newData;
			updateWidgetItems(newData, true);
		}
	}

	public void resetToOriginal() {
		updateWidgetItems(originalWidgetsData, true);
	}

	public void updateEditMode() {
		List<Object> newItems = getUpdatedItems(originalWidgetsData);
		updateAdapter(newItems, true);
	}

	@Override
	public void onWidgetSelectedToAdd(@NonNull String widgetId, @NonNull WidgetsPanel widgetsPanel, boolean recreateControls) {
		MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		if (widgetInfo != null) {
			adapter.addWidget(widgetInfo);
		}
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		int filter = ENABLED_MODE | AVAILABLE_MODE | MATCHING_PANELS_MODE;
		WidgetsSettingsHelper helper = new WidgetsSettingsHelper(requireMapActivity(), appMode);
		List<List<MapWidgetInfo>> widgetsOrder = helper.getWidgetInfoPagedOrder(appMode, selectedAppMode, selectedPanel, filter);
		updateWidgetItems(widgetsOrder, false);
	}

	@Override
	public void onActionConfirmed(int actionId) {

	}

	private void updateWidgetItems(@NonNull List<List<MapWidgetInfo>> pagedWidgets, boolean updatePosition) {
		List<Object> newItems = getUpdatedItems(pagedWidgets);
		updateAdapter(newItems, updatePosition);
	}

	private List<Object> getUpdatedItems(@NonNull List<List<MapWidgetInfo>> pagedWidgets) {
		List<Object> items = new ArrayList<>();
		boolean isEmpty = pagedWidgets.stream().allMatch(List::isEmpty);
		if (isEmpty) {
			items.add(VIEW_TYPE_DIVIDER);
			items.add(VIEW_TYPE_EMPTY_STATE);
			return items;
		}

		if (isEditMode()) {
			items.add(VIEW_TYPE_DIVIDER);

			int pageNumber = 1;
			for (List<MapWidgetInfo> widgetsOnPage : pagedWidgets) {
				PageItem pageItem = new PageItem(pageNumber);
				items.add(pageItem);
				for (MapWidgetInfo widgetInfo : widgetsOnPage) {
					items.add(new WidgetItem(widgetInfo));
				}

				pageNumber++;
			}

			items.add(VIEW_TYPE_ADD_PAGE);
			items.add(VIEW_TYPE_SPACE);
		} else {
			items.add(VIEW_TYPE_DIVIDER);

			int pageNumber = 1;
			for (List<MapWidgetInfo> widgetsOnPage : pagedWidgets) {
				PageItem pageItem = new PageItem(pageNumber);
				items.add(pageItem);
				for (MapWidgetInfo widgetInfo : widgetsOnPage) {
					items.add(new WidgetItem(widgetInfo));
				}

				pageNumber++;
			}

			items.add(VIEW_TYPE_SPACE);
		}
		return items;
	}

	private List<Object> updateItemsState(@NonNull List<Object> newItems) {
		int pageNumber = 1;

		for (int position = 0; position < newItems.size(); position++) {
			Object object = newItems.get(position);

			if (object instanceof PageItem pageItem) {

				boolean isPageMovable;
				String deleteMessage = null;
				if (isFirstPage(position, newItems)) {
					isPageMovable = false;
				} else if (selectedPanel.isPanelVertical()) {
					int previousRowPosition = getPreviousRowPosition(newItems, position);

					boolean rowHasComplexWidget = rowHasComplexWidget(position, newItems);
					boolean previousRowHasComplexWidget = rowHasComplexWidget(previousRowPosition, newItems);
					boolean isRowEmpty = getRowWidgetIds(position, newItems).isEmpty();
					boolean isPreviousRowEmpty = getRowWidgetIds(previousRowPosition, newItems).isEmpty();

					if (rowHasComplexWidget && !isPreviousRowEmpty) {
						deleteMessage = app.getString(R.string.remove_widget_first);
						isPageMovable = false;
					} else if (previousRowHasComplexWidget && !isRowEmpty) {
						deleteMessage = app.getString(R.string.previous_row_has_complex_widget);
						isPageMovable = false;
					} else {
						isPageMovable = true;
					}
				} else {
					isPageMovable = true;
				}
				pageItem.movable = isPageMovable;
				pageItem.deleteMessage = deleteMessage;
				pageItem.pageNumber = pageNumber;
				pageNumber++;

			} else if (object instanceof WidgetItem widgetItem) {
				boolean showDivider = false;
				boolean showBottomShadow = false;
				int nextItemIndex = position + 1;
				if (newItems.size() > nextItemIndex) {
					if (newItems.get(nextItemIndex) instanceof WidgetItem) {
						showDivider = true;
					} else if (newItems.get(nextItemIndex) instanceof Integer integer
							&& integer == VIEW_TYPE_SPACE) {
						showBottomShadow = true;
					}
				}
				widgetItem.showBottomDivider = showDivider;
				widgetItem.showBottomShadow = showBottomShadow;
			}
		}
		return newItems;
	}

	public static boolean isFirstPage(int position, @NonNull List<Object> searchItems) {
		for (int i = 0; i < position; i++) {
			if (searchItems.get(i) instanceof PageItem) {
				return false;
			}
		}
		return true;
	}

	@NonNull
	public static ArrayList<MapWidgetInfo> getRowWidgetIds(int rowPosition, @NonNull List<Object> searchItems) {
		ArrayList<MapWidgetInfo> rowWidgetIds = new ArrayList<>();
		Object item = searchItems.get(++rowPosition);

		while (rowPosition < searchItems.size() && item instanceof WidgetItem) {
			item = searchItems.get(rowPosition);
			if (item instanceof WidgetItem widgetItem) {
				rowWidgetIds.add(widgetItem.mapWidgetInfo);
			}
			rowPosition++;
		}
		return rowWidgetIds;
	}

	public static boolean rowHasComplexWidget(int rowPosition, @NonNull List<Object> searchItems) {
		ArrayList<MapWidgetInfo> rowWidgetIds = getRowWidgetIds(rowPosition, searchItems);
		for (MapWidgetInfo widgetUiInfo : rowWidgetIds) {
			if (isComplexWidget(widgetUiInfo.key)) {
				return true;
			}
		}
		return false;
	}

	public static int getPreviousRowPosition(@NonNull List<Object> newItems, int currentRowPosition) {
		currentRowPosition--;
		while (currentRowPosition >= 0) {
			if (newItems.get(currentRowPosition) instanceof PageItem) {
				return currentRowPosition;
			}
			currentRowPosition--;
		}
		return -1;
	}

	private void updateAdapter(@NonNull List<Object> items, boolean updatePosition) {
		boolean editMode = isEditMode();
		List<Object> newItems = updateItemsState(items);
		WidgetsListDiffCallback diffCallback = new WidgetsListDiffCallback(
				adapter.getItems(),
				newItems,
				adapter.isCurrentlyInEditMode(),
				editMode,
				updatePosition);
		DiffResult diffRes = calculateDiff(diffCallback);

		adapter.setEditMode(editMode);
		adapter.setItems(newItems);
		diffRes.dispatchUpdatesTo(adapter);
	}

	static class WidgetsListDiffCallback extends Callback {
		public static final int PAYLOAD_EDIT_MODE_CHANGED = 0;
		public static final int PAYLOAD_MOVE_STATE_CHANGED = 1;
		public static final int PAYLOAD_DIVIDER_STATE_CHANGED = 2;
		public static final int PAYLOAD_UPDATE_ALL = 3;
		public static final int PAYLOAD_UPDATE_POSITION = 4;
		private final List<Object> oldItems;
		private final List<Object> newItems;
		private final boolean oldEditMode;
		private final boolean newEditMode;

		private final boolean updatePosition;

		WidgetsListDiffCallback(@NonNull List<Object> oldItems, @NonNull List<Object> newItems,
		                        boolean oldEditMode, boolean newEditMode, boolean updatePosition) {
			this.oldItems = new ArrayList<>(oldItems);
			this.newItems = newItems;
			this.oldEditMode = oldEditMode;
			this.newEditMode = newEditMode;
			this.updatePosition = updatePosition;
		}

		@Override
		public int getOldListSize() {
			return oldItems.size();
		}

		@Override
		public int getNewListSize() {
			return newItems.size();
		}

		@Override
		public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
			Object oldItem = oldItems.get(oldItemPosition);
			Object newItem = newItems.get(newItemPosition);

			if (oldItem instanceof Integer && newItem instanceof Integer) {
				return oldItem.equals(newItem);
			}

			if (oldItem instanceof PageItem && newItem instanceof PageItem) {
				return ((PageItem) oldItem).pageNumber == ((PageItem) newItem).pageNumber;
			}

			if (oldItem instanceof WidgetItem && newItem instanceof WidgetItem) {
				return Objects.equals(((WidgetItem) oldItem).mapWidgetInfo.key, ((WidgetItem) newItem).mapWidgetInfo.key);
			}

			return false;
		}

		@Override
		public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
			Object oldItem = oldItems.get(oldItemPosition);
			Object newItem = newItems.get(newItemPosition);

			if (oldEditMode != newEditMode) {
				return false;
			}

			if (updatePosition) {
				return false;
			}

			if (oldItem instanceof Integer && newItem instanceof Integer) {
				return oldItem.equals(newItem);
			}

			if (oldItem instanceof PageItem oldPage && newItem instanceof PageItem newPage) {
				return oldPage.equals(newPage);
			}

			if (oldItem instanceof WidgetItem oldWidget && newItem instanceof WidgetItem newWidget) {
				return oldWidget.equals(newWidget);
			}

			return false;
		}

		@Nullable
		@Override
		public Object getChangePayload(int oldItemPosition, int newItemPosition) {
			Object oldItem = oldItems.get(oldItemPosition);
			Object newItem = newItems.get(newItemPosition);

			List<Integer> payloads = new ArrayList<>();

			if (oldEditMode != newEditMode) {
				payloads.add(PAYLOAD_EDIT_MODE_CHANGED);
			}

			if (updatePosition) {
				payloads.add(PAYLOAD_UPDATE_POSITION);
			}

			if (oldItem instanceof PageItem oldPage && newItem instanceof PageItem newPage) {
				if (oldPage.pageNumber != newPage.pageNumber) {
					payloads.add(PAYLOAD_UPDATE_ALL);
				}
				if ((oldPage.movable != newPage.movable) || (!Objects.equals(oldPage.deleteMessage, newPage.deleteMessage))) {
					payloads.add(PAYLOAD_MOVE_STATE_CHANGED);
				}
			}

			if (oldItem instanceof WidgetItem oldWidget && newItem instanceof WidgetItem newWidget) {
				if (!Objects.equals(oldWidget.mapWidgetInfo, newWidget.mapWidgetInfo)) {
					payloads.add(PAYLOAD_UPDATE_ALL);
				}
				if ((oldWidget.showBottomDivider != newWidget.showBottomDivider)
						|| (oldWidget.showBottomShadow != newWidget.showBottomShadow)) {
					payloads.add(PAYLOAD_DIVIDER_STATE_CHANGED);
				}
			}

			return payloads.isEmpty() ? null : payloads;
		}
	}

}