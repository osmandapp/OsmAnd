package net.osmand.plus.views.mapwidgets.configure.panel;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.DEFAULT_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.DISABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment.KEY_APP_MODE;
import static net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment.KEY_WIDGET_ID;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.ConfigureScreenActionsCard;
import net.osmand.plus.views.mapwidgets.configure.ConfirmResetToDefaultBottomSheetDialog.ResetToDefaultListener;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.add.AddWidgetFragment;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsFragment;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AvailableItemViewHolder;
import net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class WidgetsListFragment extends Fragment implements OnScrollChangedListener,
		ResetToDefaultListener, CopyAppModePrefsListener {

	private static final String SELECTED_GROUP_ATTR = "selected_group_key";

	private OsmandApplication app;
	private OsmandSettings settings;
	private MapWidgetRegistry widgetRegistry;

	private WidgetsPanel selectedPanel;
	private ApplicationMode selectedAppMode;
	private WidgetIconsHelper iconsHelper;

	private View view;
	private NestedScrollView scrollView;
	private View changeOrderListButton;
	private View changeOrderFooterButton;
	private ViewGroup enabledWidgetsContainer;
	private ViewGroup availableWidgetsContainer;
	private ViewGroup actionsCardContainer;

	private final int enabledWidgetsFilter = AVAILABLE_MODE | ENABLED_MODE;

	private boolean nightMode;

	public void setSelectedPanel(@NonNull WidgetsPanel panel) {
		this.selectedPanel = panel;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (OsmandApplication) requireContext().getApplicationContext();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();

		settings = app.getSettings();
		nightMode = !settings.isLightContent();
		selectedAppMode = settings.getApplicationMode();
		iconsHelper = new WidgetIconsHelper(app, selectedAppMode.getProfileColor(nightMode), nightMode);
		if (savedInstanceState != null) {
			selectedPanel = WidgetsPanel.valueOf(savedInstanceState.getString(SELECTED_GROUP_ATTR));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(requireContext(), nightMode);
		view = inflater.inflate(R.layout.fragment_widgets_list, container, false);

		enabledWidgetsContainer = view.findViewById(R.id.enabled_widgets_list);
		availableWidgetsContainer = view.findViewById(R.id.available_widgets_list);
		changeOrderListButton = view.findViewById(R.id.change_order_button_in_list);
		changeOrderFooterButton = view.findViewById(R.id.change_order_button_in_bottom);
		actionsCardContainer = view.findViewById(R.id.configure_screen_actions_container);

		scrollView = view.findViewById(R.id.scroll_view);
		scrollView.getViewTreeObserver().addOnScrollChangedListener(this);

		updateContent();

		TextView panelTitle = view.findViewById(R.id.panel_title);
		panelTitle.setText(getString(selectedPanel.getTitleId(AndroidUtils.isLayoutRtl(app))));

		setupReorderButton(changeOrderListButton);
		setupReorderButton(changeOrderFooterButton);
		setupActionsCard();

		return view;
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

	public void setupReorderButton(@NonNull View view) {
		view.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				ReorderWidgetsFragment.showInstance(activity.getSupportFragmentManager(), selectedPanel,
						selectedAppMode, getParentFragment());
			}
		});
		setupListItemBackground(view);
	}

	public void scrollToActions() {
		scrollView.smoothScrollTo(0, (int) actionsCardContainer.getY());
	}

	private void setupActionsCard() {
		int panelTitleId = selectedPanel.getTitleId(AndroidUtils.isLayoutRtl(app));
		View cardView = new ConfigureScreenActionsCard(requireMapActivity(), this, selectedAppMode, panelTitleId)
				.build(view.getContext());
		actionsCardContainer.addView(cardView);
	}

	@Override
	public void onResetToDefaultConfirmed() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		List<WidgetsPanel> panels = Collections.singletonList(selectedPanel);
		Set<MapWidgetInfo> widgetInfos = widgetRegistry
				.getWidgetsForPanel(mapActivity, selectedAppMode, 0, panels);
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			Boolean newEnableState = isOriginalWidgetOnAnotherPanel(widgetInfo)
					? false // Disable (not reset), because widget can be enabled by default
					: null;
			widgetRegistry.enableDisableWidgetForMode(selectedAppMode, widgetInfo, newEnableState, false);
		}
		selectedPanel.getOrderPreference(settings).resetModeToDefault(selectedAppMode);

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateAllControls(mapActivity);
		}
		updateContent();
	}

	private boolean isOriginalWidgetOnAnotherPanel(@NonNull MapWidgetInfo widgetInfo) {
		boolean original = WidgetType.isOriginalWidget(widgetInfo.key);
		WidgetType widgetType = widgetInfo.widget.getWidgetType();
		return original && widgetType != null && widgetType.defaultPanel != widgetInfo.widgetPanel;
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		MapWidgetsFactory widgetsFactory = new MapWidgetsFactory(mapActivity);

		List<WidgetsPanel> panels = Collections.singletonList(selectedPanel);
		List<MapWidgetInfo> defaultWidgetInfos = getDefaultWidgetInfos(mapActivity);
		Set<MapWidgetInfo> widgetInfosToCopy = widgetRegistry
				.getWidgetsForPanel(mapActivity, appMode, ENABLED_MODE | AVAILABLE_MODE, panels);
		List<List<String>> newPagedOrder = new ArrayList<>();
		int previousPage = -1;

		for (MapWidgetInfo widgetInfoToCopy : widgetInfosToCopy) {
			if (!selectedAppMode.isWidgetAvailable(widgetInfoToCopy.key)) {
				continue;
			}

			WidgetType widgetTypeToCopy = widgetInfoToCopy.widget.getWidgetType();
			boolean duplicateNotPossible = widgetTypeToCopy == null || !selectedPanel.isDuplicatesAllowed();
			String defaultWidgetId = WidgetType.getDefaultWidgetId(widgetInfoToCopy.key);
			MapWidgetInfo defaultWidgetInfo = getWidgetInfoById(defaultWidgetId, defaultWidgetInfos);

			if (defaultWidgetInfo != null) {
				String widgetIdToAdd;
				boolean disabled = !defaultWidgetInfo.isEnabledForAppMode(selectedAppMode);
				boolean inAnotherPanel = defaultWidgetInfo.widgetPanel != selectedPanel;
				if (duplicateNotPossible || (disabled && !inAnotherPanel)) {
					widgetRegistry.enableDisableWidgetForMode(selectedAppMode, defaultWidgetInfo, true, false);
					widgetIdToAdd = defaultWidgetInfo.key;
				} else {
					MapWidgetInfo duplicateWidgetInfo = createDuplicateWidgetInfo(widgetTypeToCopy, widgetsFactory);
					widgetIdToAdd = duplicateWidgetInfo != null ? duplicateWidgetInfo.key : null;
				}

				if (!Algorithms.isEmpty(widgetIdToAdd)) {
					if (previousPage != widgetInfoToCopy.pageIndex || newPagedOrder.size() == 0) {
						previousPage = widgetInfoToCopy.pageIndex;
						newPagedOrder.add(new ArrayList<>());
					}
					newPagedOrder.get(newPagedOrder.size() - 1).add(widgetIdToAdd);
				}
			}
		}

		selectedPanel.setWidgetsOrder(selectedAppMode, newPagedOrder, settings);

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (settings.getApplicationMode().equals(selectedAppMode) && mapInfoLayer != null) {
			mapInfoLayer.recreateAllControls(mapActivity);
		}
		updateContent();
	}

	@NonNull
	private List<MapWidgetInfo> getDefaultWidgetInfos(@NonNull MapActivity mapActivity) {
		Set<MapWidgetInfo> widgetInfos = widgetRegistry
				.getWidgetsForPanel(mapActivity, selectedAppMode, 0, selectedPanel.getMergedPanels());
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			if (widgetInfo.widgetPanel == selectedPanel) {
				Boolean visibility = WidgetType.isOriginalWidget(widgetInfo.key) ? false : null;
				widgetRegistry.enableDisableWidgetForMode(selectedAppMode, widgetInfo, visibility, false);
			}
		}
		selectedPanel.getOrderPreference(settings).resetModeToDefault(selectedAppMode);
		return new ArrayList<>(widgetInfos);
	}

	@Nullable
	private MapWidgetInfo createDuplicateWidgetInfo(@NonNull WidgetType widgetType,
	                                                @NonNull MapWidgetsFactory widgetsFactory) {
		String duplicateWidgetId = WidgetType.getDuplicateWidgetId(widgetType.id);
		MapWidget duplicateWidget = widgetsFactory.createMapWidget(duplicateWidgetId, widgetType);
		if (duplicateWidget != null) {
			settings.CUSTOM_WIDGETS_KEYS.addModeValue(selectedAppMode, duplicateWidgetId);
			MapWidgetInfo duplicateWidgetInfo = widgetRegistry.createCustomWidget(duplicateWidgetId,
					duplicateWidget, widgetType, selectedPanel, selectedAppMode);
			widgetRegistry.enableDisableWidgetForMode(selectedAppMode, duplicateWidgetInfo, true, false);
			return duplicateWidgetInfo;
		}
		return null;
	}

	@Nullable
	private MapWidgetInfo getWidgetInfoById(@NonNull String widgetId, @NonNull List<MapWidgetInfo> widgetInfos) {
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			if (widgetId.equals(widgetInfo.key)) {
				return widgetInfo;
			}
		}
		return null;
	}

	public void updateContent() {
		updateEnabledWidgets();
		updateAvailableWidgets();
	}

	private void updateEnabledWidgets() {
		enabledWidgetsContainer.removeAllViews();

		MapActivity mapActivity = requireMapActivity();
		Set<MapWidgetInfo> enabledWidgets = widgetRegistry.getWidgetsForPanel(mapActivity, selectedAppMode,
				enabledWidgetsFilter, Collections.singletonList(selectedPanel));
		boolean noEnabledWidgets = Algorithms.isEmpty(enabledWidgets);

		View noWidgetsContainer = view.findViewById(R.id.no_widgets_container);
		AndroidUiHelper.updateVisibility(noWidgetsContainer, noEnabledWidgets);
		if (noEnabledWidgets) {
			boolean rtl = AndroidUtils.isLayoutRtl(app);
			ImageView imageView = view.findViewById(R.id.no_widgets_image);
			imageView.setImageDrawable(app.getUIUtilities().getIcon(selectedPanel.getIconId(rtl), nightMode));
		} else {
			inflateEnabledWidgets();
		}
	}

	private void inflateEnabledWidgets() {
		MapActivity mapActivity = requireMapActivity();
		LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
		if (selectedPanel.isPagingAllowed()) {
			List<Set<MapWidgetInfo>> pagedWidgets = widgetRegistry.getPagedWidgetsForPanel(mapActivity, selectedAppMode, selectedPanel, enabledWidgetsFilter);
			for (int i = 0; i < pagedWidgets.size(); i++) {
				inflatePageItemView(i, inflater);
				inflateWidgetItemsViews(pagedWidgets.get(i), inflater);
			}
		} else {
			Set<MapWidgetInfo> widgets = widgetRegistry.getWidgetsForPanel(mapActivity, selectedAppMode, enabledWidgetsFilter, Collections.singletonList(selectedPanel));
			inflateWidgetItemsViews(widgets, inflater);
		}
	}

	private void inflatePageItemView(int index, @NonNull LayoutInflater inflater) {
		View view = inflater.inflate(R.layout.configure_screen_list_item_page, enabledWidgetsContainer, false);
		View topDivider = view.findViewById(R.id.top_divider);
		TextView pageText = view.findViewById(R.id.page);
		AndroidUiHelper.updateVisibility(topDivider, index > 0);
		pageText.setText(getString(R.string.page_number, String.valueOf(index + 1)));
		enabledWidgetsContainer.addView(view);
	}

	private void inflateWidgetItemsViews(@NonNull Set<MapWidgetInfo> widgetsInfo, @NonNull LayoutInflater inflater) {
		List<MapWidgetInfo> widgets = new ArrayList<>(widgetsInfo);

		for (int i = 0; i < widgets.size(); i++) {
			MapWidgetInfo widgetInfo = widgets.get(i);

			View view = inflater.inflate(R.layout.configure_screen_widget_item, enabledWidgetsContainer, false);

			TextView title = view.findViewById(R.id.title);
			title.setText(widgetInfo.getTitle(app));

			WidgetType widgetType = WidgetType.getById(widgetInfo.key);
			WidgetGroup widgetGroup = widgetType == null ? null : widgetType.group;
			if (widgetGroup != null) {
				TextView description = view.findViewById(R.id.description);
				description.setText(widgetGroup.titleId);
				AndroidUiHelper.updateVisibility(description, true);
			}

			ImageView imageView = view.findViewById(R.id.icon);
			iconsHelper.updateWidgetIcon(imageView, widgetInfo);

			View settingsButton = view.findViewById(R.id.settings_button);
			WidgetSettingsBaseFragment fragment = widgetType != null ? widgetType.getSettingsFragment() : null;
			if (fragment != null) {
				settingsButton.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						Bundle args = new Bundle();
						args.putString(KEY_WIDGET_ID, widgetInfo.key);
						args.putString(KEY_APP_MODE, selectedAppMode.getStringKey());

						Fragment target = getParentFragment();
						FragmentManager manager = activity.getSupportFragmentManager();
						WidgetSettingsBaseFragment.showFragment(manager, args, target, fragment);
					}
				});
				setupListItemBackground(settingsButton);
			}

			View infoButton = view.findViewById(R.id.info_button);
			infoButton.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					Fragment target = getParentFragment();
					if (widgetGroup != null) {
						AddWidgetFragment.showGroupDialog(fragmentManager, target,
								selectedAppMode, selectedPanel, widgetGroup, null);
					} else {
						AddWidgetFragment.showWidgetDialog(fragmentManager, target,
								selectedAppMode, selectedPanel, widgetType, null);
					}
				}
			});
			view.setOnClickListener(v -> infoButton.callOnClick());

			AndroidUiHelper.updateVisibility(settingsButton, fragment != null);
			AndroidUiHelper.updateVisibility(infoButton, true);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), fragment != null);

			setupListItemBackground(view);

			View bottomDivider = view.findViewById(R.id.bottom_divider);
			boolean last = i + 1 == widgets.size();
			AndroidUiHelper.updateVisibility(bottomDivider, !last);

			enabledWidgetsContainer.addView(view);
		}
	}

	private void updateAvailableWidgets() {
		availableWidgetsContainer.removeAllViews();

		int filter = AVAILABLE_MODE | DEFAULT_MODE;
		if (!selectedPanel.isDuplicatesAllowed()) {
			filter |= DISABLED_MODE;
		}

		MapActivity mapActivity = requireMapActivity();
		Set<MapWidgetInfo> availableWidgets = widgetRegistry.getWidgetsForPanel(mapActivity, selectedAppMode, filter, selectedPanel.getMergedPanels());
		boolean hasAvailableWidgets = !Algorithms.isEmpty(availableWidgets);
		if (hasAvailableWidgets) {
			List<WidgetType> disabledDefaultWidgets = listDefaultWidgets(availableWidgets);
			List<MapWidgetInfo> externalWidgets = listExternalWidgets(availableWidgets);

			inflateAvailableDefaultWidgets(excludeGroupsDuplicated(disabledDefaultWidgets), !Algorithms.isEmpty(externalWidgets));
			inflateAvailableExternalWidgets(externalWidgets);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.available_widgets_container), hasAvailableWidgets);
	}

	@NonNull
	private List<WidgetType> excludeGroupsDuplicated(List<WidgetType> widgets) {
		List<WidgetGroup> visitedGroups = new ArrayList<>();
		List<WidgetType> matchingWidgets = new ArrayList<>();
		for (WidgetType widget : widgets) {
			WidgetGroup group = widget.group;
			if (group != null && !visitedGroups.contains(group)) {
				visitedGroups.add(group);
				matchingWidgets.add(widget);
			} else if (group == null) {
				matchingWidgets.add(widget);
			}
		}
		return matchingWidgets;
	}

	private void inflateAvailableDefaultWidgets(@NonNull List<WidgetType> widgets, boolean hasExternalWidgets) {
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		for (int i = 0; i < widgets.size(); i++) {
			WidgetType widgetType = widgets.get(i);
			WidgetGroup widgetGroup = widgetType.group;

			View view = inflater.inflate(R.layout.configure_screen_widget_item, availableWidgetsContainer, false);

			ImageView icon = view.findViewById(R.id.icon);
			if (widgetGroup != null) {
				icon.setImageResource(widgetGroup.getIconId(nightMode));
			} else {
				iconsHelper.updateWidgetIcon(icon, widgetType);
			}

			CharSequence title = widgetGroup != null
					? AvailableItemViewHolder.getGroupTitle(widgetGroup, app, nightMode)
					: getString(widgetType.titleId);
			((TextView) view.findViewById(R.id.title)).setText(title);

			View infoButton = view.findViewById(R.id.info_button);
			infoButton.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					Fragment target = getParentFragment();
					if (widgetGroup != null) {
						AddWidgetFragment.showGroupDialog(fragmentManager, target,
								selectedAppMode, selectedPanel, widgetGroup, null);
					} else {
						AddWidgetFragment.showWidgetDialog(fragmentManager, target,
								selectedAppMode, selectedPanel, widgetType, null);
					}
				}
			});
			view.setOnClickListener(v -> infoButton.callOnClick());

			AndroidUiHelper.updateVisibility(infoButton, true);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.settings_button), false);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), false);

			boolean last = i + 1 == widgets.size();
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), !last || hasExternalWidgets);

			setupListItemBackground(view);

			availableWidgetsContainer.addView(view);
		}
	}

	private void inflateAvailableExternalWidgets(@NonNull List<MapWidgetInfo> externalWidgets) {
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		for (int i = 0; i < externalWidgets.size(); i++) {
			MapWidgetInfo widgetInfo = externalWidgets.get(i);

			View view = inflater.inflate(R.layout.configure_screen_widget_item, availableWidgetsContainer, false);

			ImageView icon = view.findViewById(R.id.icon);
			iconsHelper.updateWidgetIcon(icon, widgetInfo);

			TextView title = view.findViewById(R.id.title);
			title.setText(widgetInfo.getTitle(app));

			View infoButton = view.findViewById(R.id.info_button);
			infoButton.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				String externalProviderPackage = widgetInfo.getExternalProviderPackage();
				if (activity != null && !Algorithms.isEmpty(externalProviderPackage)) {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					AddWidgetFragment.showExternalWidgetDialog(fragmentManager, getParentFragment(),
							selectedAppMode, selectedPanel, widgetInfo.key, externalProviderPackage, null);
				}
			});
			AndroidUiHelper.updateVisibility(infoButton, true);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.settings_button), false);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), false);

			boolean last = i + 1 == externalWidgets.size();
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), !last);

			availableWidgetsContainer.addView(view);
		}
	}

	@NonNull
	private List<WidgetType> listDefaultWidgets(@NonNull Set<MapWidgetInfo> widgets) {
		Map<Integer, WidgetType> defaultWidgets = new TreeMap<>();
		for (MapWidgetInfo widgetInfo : widgets) {
			WidgetType widgetType = WidgetType.getById(widgetInfo.key);
			if (widgetType != null) {
				defaultWidgets.put(widgetType.ordinal(), widgetType);
			}
		}
		return new ArrayList<>(defaultWidgets.values());
	}

	@NonNull
	private List<MapWidgetInfo> listExternalWidgets(@NonNull Set<MapWidgetInfo> widgets) {
		List<MapWidgetInfo> externalWidgets = new ArrayList<>();
		for (MapWidgetInfo widgetInfo : widgets) {
			WidgetType widgetType = WidgetType.getById(widgetInfo.key);
			if (widgetType == null) {
				externalWidgets.add(widgetInfo);
			}
		}
		return externalWidgets;
	}

	private void setupListItemBackground(@NonNull View view) {
		setupListItemBackground(app, view, selectedAppMode.getProfileColor(nightMode));
	}

	public static void setupListItemBackground(@NonNull Context context,
	                                           @NonNull View view,
	                                           @ColorInt int profileColor) {
		View button = view.findViewById(R.id.container);
		Drawable background = UiUtilities.getColoredSelectableDrawable(context, profileColor, 0.3f);
		AndroidUtils.setBackground(button, background);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_GROUP_ATTR, selectedPanel.name());
	}

	@Override
	public void onScrollChanged() {
		int y1 = AndroidUtils.getViewOnScreenY(changeOrderListButton);
		int y2 = AndroidUtils.getViewOnScreenY(changeOrderFooterButton);
		changeOrderFooterButton.setVisibility(y1 <= y2 ? View.GONE : View.VISIBLE);
	}

	@NonNull
	public MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (!(activity instanceof MapActivity)) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return (MapActivity) activity;
	}

	@Nullable
	private MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		return activity instanceof MapActivity ? ((MapActivity) activity) : null;
	}
}