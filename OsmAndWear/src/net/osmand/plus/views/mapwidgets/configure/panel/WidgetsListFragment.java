package net.osmand.plus.views.mapwidgets.configure.panel;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.DEFAULT_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;
import static net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.*;
import static net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment.KEY_APP_MODE;
import static net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment.KEY_WIDGET_ID;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.ImageView;
import android.widget.TextView;

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
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet.ConfirmationDialogListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.WidgetsSettingsHelper;
import net.osmand.plus.views.mapwidgets.configure.dialogs.AddWidgetFragment;
import net.osmand.plus.views.mapwidgets.configure.dialogs.WidgetInfoFragment;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureActionsCard;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsFragment;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AvailableItemViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AvailableItemViewHolder.AvailableWidgetUiInfo;
import net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class WidgetsListFragment extends Fragment implements OnScrollChangedListener,
		ConfirmationDialogListener, CopyAppModePrefsListener {

	private static final String SELECTED_GROUP_ATTR = "selected_group_key";

	private OsmandApplication app;
	private OsmandSettings settings;
	private MapWidgetRegistry widgetRegistry;

	private WidgetsPanel selectedPanel;
	private ApplicationMode selectedAppMode;
	private WidgetsSettingsHelper widgetsSettingsHelper;
	private WidgetIconsHelper iconsHelper;

	private View view;
	private NestedScrollView scrollView;
	private View changeOrderListButton;
	private View changeOrderFooterButton;
	private ViewGroup enabledWidgetsContainer;
	private ViewGroup availableWidgetsContainer;
	private ViewGroup actionsCardContainer;

	private final int enabledWidgetsFilter = AVAILABLE_MODE | ENABLED_MODE | MATCHING_PANELS_MODE;

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
		widgetsSettingsHelper = new WidgetsSettingsHelper(requireMapActivity(), selectedAppMode);
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

		boolean isRtl = AndroidUtils.isLayoutRtl(view.getContext());
		TextView title = view.findViewById(R.id.panel_title);
		title.setText(getString(isVerticalPanel() ? R.string.shared_string_rows : selectedPanel.getTitleId(isRtl)));

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
				FragmentManager manager = activity.getSupportFragmentManager();
				ReorderWidgetsFragment.showInstance(manager, selectedPanel, selectedAppMode, getParentFragment());
			}
		});
		setupListItemBackground(view);
	}

	public void scrollToActions() {
		scrollView.smoothScrollTo(0, (int) actionsCardContainer.getY());
	}

	public void scrollToAvailable() {
		scrollView.post(() -> {
			View availableWidgetsDivider = view.findViewById(R.id.available_widgets_divider);
			scrollView.scrollTo(0, availableWidgetsContainer.getTop() + enabledWidgetsContainer.getBottom() + (availableWidgetsDivider.getBottom() * 2));
		});
	}

	private void setupActionsCard() {
		int panelTitleId = selectedPanel.getTitleId(AndroidUtils.isLayoutRtl(app));
		View cardView = new ConfigureActionsCard(requireMapActivity(), this, panelTitleId)
				.build(view.getContext());
		actionsCardContainer.addView(cardView);
	}

	@Override
	public void onActionConfirmed(int actionId) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		widgetsSettingsHelper.resetWidgetsForPanel(selectedPanel);

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateAllControls(mapActivity);
		}
		updateContent();
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode fromAppMode) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		widgetsSettingsHelper.copyWidgetsForPanel(fromAppMode, selectedPanel);

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (settings.getApplicationMode().equals(selectedAppMode) && mapInfoLayer != null) {
			mapInfoLayer.recreateAllControls(mapActivity);
		}
		updateContent();
	}

	public void updateContent() {
		updateEnabledWidgets();
		updateAvailableWidgets();
	}

	public WidgetsPanel getSelectedPanel(){
		return selectedPanel;
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

		List<Set<MapWidgetInfo>> pagedWidgets = widgetRegistry.getPagedWidgetsForPanel(mapActivity, selectedAppMode, selectedPanel, enabledWidgetsFilter);
		for (int i = 0; i < pagedWidgets.size(); i++) {
			if (!isVerticalPanel()) {
				inflatePageItemView(i, inflater);
			}
			inflateWidgetItemsViews(pagedWidgets.get(i), inflater, i + 1, i == pagedWidgets.size() - 1);
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

	private void inflateWidgetItemsViews(@NonNull Set<MapWidgetInfo> widgetsInfo, @NonNull LayoutInflater inflater, int row, boolean lastRow) {
		List<MapWidgetInfo> widgets = new ArrayList<>(widgetsInfo);

		for (int i = 0; i < widgets.size(); i++) {
			MapWidgetInfo widgetInfo = widgets.get(i);

			View view = inflater.inflate(R.layout.configure_screen_widget_item, enabledWidgetsContainer, false);

			TextView title = view.findViewById(R.id.title);
			title.setText(widgetInfo.getTitle(app));

			WidgetType widgetType = widgetInfo.getWidgetType();
			WidgetGroup widgetGroup = widgetType == null ? null : widgetType.getGroup(selectedPanel);
			if (widgetGroup != null) {
				TextView description = view.findViewById(R.id.description);
				description.setText(widgetGroup.titleId);
				AndroidUiHelper.updateVisibility(description, true);
			}

			ImageView imageView = view.findViewById(R.id.icon);
			iconsHelper.updateWidgetIcon(imageView, widgetInfo);

			View settingsButton = view.findViewById(R.id.settings_button);
			WidgetSettingsBaseFragment fragment = widgetType != null ? widgetType.getSettingsFragment(app, widgetInfo) : null;
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
				UiUtilities.setupListItemBackground(app, settingsButton, selectedAppMode.getProfileColor(nightMode));
			}

			View infoButton = view.findViewById(R.id.info_button);
			infoButton.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					Fragment target = getParentFragment();
					WidgetInfoFragment.showInstance(fragmentManager, target, selectedAppMode, widgetInfo.key);
				}
			});
			view.setOnClickListener(v -> infoButton.callOnClick());

			AndroidUiHelper.updateVisibility(settingsButton, fragment != null);
			AndroidUiHelper.updateVisibility(infoButton, true);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), fragment != null);

			setupListItemBackground(view);

			View bottomDivider = view.findViewById(R.id.bottom_divider);
			boolean lastWidget = i + 1 == widgets.size();
			AndroidUiHelper.updateVisibility(bottomDivider, !lastWidget);

			if (isVerticalPanel()) {
				TextView rowId = view.findViewById(R.id.row_id);
				rowId.setText(String.valueOf(row));
				AndroidUiHelper.setVisibility(i == 0 ? View.VISIBLE : View.INVISIBLE, rowId);

				if (lastWidget && !lastRow) {
					ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomDivider.getLayoutParams();
					params.setMarginStart(0);
					AndroidUiHelper.updateVisibility(bottomDivider, true);
				}
			}
			enabledWidgetsContainer.addView(view);
		}
	}

	private void updateAvailableWidgets() {
		availableWidgetsContainer.removeAllViews();

		int filter = AVAILABLE_MODE | DEFAULT_MODE;

		MapActivity mapActivity = requireMapActivity();
		Set<MapWidgetInfo> availableWidgets = widgetRegistry.getWidgetsForPanel(mapActivity, selectedAppMode, filter, Collections.singletonList(selectedPanel));
		boolean hasAvailableWidgets = !Algorithms.isEmpty(availableWidgets);
		if (hasAvailableWidgets) {
			List<WidgetType> defaultWidgets = excludeGroupsDuplicated(listDefaultWidgets(availableWidgets));
			sortWidgetsItems(defaultWidgets, app, nightMode);

			List<MapWidgetInfo> externalWidgets = listExternalWidgets(availableWidgets);
			sortWidgetsItems(externalWidgets, app, nightMode);

			inflateAvailableDefaultWidgets(defaultWidgets, !Algorithms.isEmpty(externalWidgets));
			inflateAvailableExternalWidgets(externalWidgets);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.available_widgets_container), hasAvailableWidgets);
	}

	public static void sortWidgetsItems(List<?> widgets, @NonNull OsmandApplication app, boolean nightMode) {
		Collections.sort(widgets, (o1, o2) -> {
			String firstName = getListItemName(o1, app, nightMode);
			String secondName = getListItemName(o2, app, nightMode);
			if (firstName != null && secondName != null) {
				return firstName.compareTo(secondName);
			}
			return 0;
		});
	}

	@Nullable
	public static String getListItemName(Object item, @NonNull OsmandApplication app, boolean nightMode) {
		if (item instanceof WidgetType widgetType) {
			WidgetGroup widgetGroup = widgetType.getGroup(widgetType.getPanel(app.getSettings()));
			return widgetGroup != null
					? String.valueOf(AvailableItemViewHolder.getGroupTitle(widgetGroup, app, nightMode))
					: app.getString(widgetType.titleId);
		} else if (item instanceof MapWidgetInfo) {
			return ((MapWidgetInfo) item).getTitle(app);
		} else if (item instanceof ListItem) {
			Object value = ((ListItem) item).value;
			if (value instanceof AvailableWidgetUiInfo) {
				return ((AvailableWidgetUiInfo) value).title;
			} else if (value instanceof WidgetGroup) {
				return ((WidgetGroup) value).name();
			}
		}
		return null;
	}

	@NonNull
	private List<WidgetType> excludeGroupsDuplicated(List<WidgetType> widgets) {
		List<WidgetGroup> visitedGroups = new ArrayList<>();
		List<WidgetType> individualWidgets = new ArrayList<>();
		List<WidgetType> result = new ArrayList<>();
		for (WidgetType widget : widgets) {
			WidgetGroup group = widget.getGroup(selectedPanel);
			if (group != null && !visitedGroups.contains(group)) {
				visitedGroups.add(group);
				result.add(widget);
			} else if (group == null) {
				individualWidgets.add(widget);
			}
		}
		result.addAll(individualWidgets);
		return result;
	}

	private void inflateAvailableDefaultWidgets(@NonNull List<WidgetType> widgets, boolean hasExternalWidgets) {
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		for (int i = 0; i < widgets.size(); i++) {
			WidgetType widgetType = widgets.get(i);
			WidgetGroup widgetGroup = widgetType.getGroup(selectedPanel);

			View view = inflater.inflate(R.layout.configure_screen_widget_item, availableWidgetsContainer, false);

			ImageView icon = view.findViewById(R.id.icon);
			if (widgetGroup != null) {
				icon.setImageResource(widgetGroup.getIconId(nightMode));
			} else {
				icon.setImageResource(widgetType.getIconId(nightMode));
			}

			CharSequence title = widgetGroup != null
					? AvailableItemViewHolder.getGroupTitle(widgetGroup, app, nightMode)
					: getString(widgetType.titleId);
			((TextView) view.findViewById(R.id.title)).setText(title);

			view.setOnClickListener(v -> {
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

			View infoButton = view.findViewById(R.id.info_button);
			infoButton.setClickable(false);
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

			view.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				String externalProviderPackage = widgetInfo.getExternalProviderPackage();
				if (activity != null && !Algorithms.isEmpty(externalProviderPackage)) {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					AddWidgetFragment.showExternalWidgetDialog(fragmentManager, getParentFragment(),
							selectedAppMode, selectedPanel, widgetInfo.key, externalProviderPackage, null);
				}
			});
			View infoButton = view.findViewById(R.id.info_button);
			infoButton.setClickable(false);
			AndroidUiHelper.updateVisibility(infoButton, true);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.settings_button), false);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), false);

			boolean last = i + 1 == externalWidgets.size();
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), !last);
			setupListItemBackground(view);

			availableWidgetsContainer.addView(view);
		}
	}

	@NonNull
	private List<WidgetType> listDefaultWidgets(@NonNull Set<MapWidgetInfo> widgets) {
		Map<Integer, WidgetType> defaultWidgets = new TreeMap<>();
		for (MapWidgetInfo widgetInfo : widgets) {
			WidgetType widgetType = widgetInfo.getWidgetType();
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
			if (widgetInfo.isExternal()) {
				externalWidgets.add(widgetInfo);
			}
		}
		return externalWidgets;
	}

	private void setupListItemBackground(@NonNull View view) {
		View container = view.findViewById(R.id.container);
		UiUtilities.setupListItemBackground(app, container, selectedAppMode.getProfileColor(nightMode));
	}

	private boolean isVerticalPanel() {
		return selectedPanel.isPanelVertical();
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