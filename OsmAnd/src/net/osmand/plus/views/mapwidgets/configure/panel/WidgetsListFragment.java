package net.osmand.plus.views.mapwidgets.configure.panel;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
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
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.WidgetsSettingsHelper;
import net.osmand.plus.views.mapwidgets.configure.dialogs.AddWidgetFragment;
import net.osmand.plus.views.mapwidgets.configure.settings.WidgetInfoBaseFragment;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
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
		nightMode = !settings.isLightContent();
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

		if (isEditMode()) {
			adapter.setItems(controller.getReorderList());
		} else {
			adapter.setData(originalWidgetsData);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		Fragment fragment = getParentFragment();
		if (fragment instanceof ConfigureWidgetsFragment) {
			((ConfigureWidgetsFragment) fragment).setSelectedFragment(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		Fragment fragment = getParentFragment();
		if (fragment instanceof ConfigureWidgetsFragment) {
			((ConfigureWidgetsFragment) fragment).setSelectedFragment(null);
		}
	}

	public void updateAdapter() {
		adapter.setData(originalWidgetsData);
	}

	public void updateContent() {
		if (adapter != null) {
			List<List<MapWidgetInfo>> newData = getPagedWidgets();
			originalWidgetsData = newData;
			adapter.setData(newData);
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
		if (isEditMode()) {
			controller.setReorderList(adapter.getItems());
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
		adapter.setData(widgetsOrder);
	}

	@Override
	public void onActionConfirmed(int actionId) {

	}
}