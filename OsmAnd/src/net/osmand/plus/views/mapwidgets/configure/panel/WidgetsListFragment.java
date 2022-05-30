package net.osmand.plus.views.mapwidgets.configure.panel;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.DEFAULT_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.DISABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment.KEY_APP_MODE;
import static net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment.KEY_WIDGET_ID;

import android.graphics.drawable.Drawable;
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
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetParams;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.add.AddWidgetFragment;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsFragment;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AvailableItemViewHolder;
import net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class WidgetsListFragment extends Fragment implements OnScrollChangedListener {

	private static final String SELECTED_GROUP_ATTR = "selected_group_key";

	private OsmandApplication app;
	private OsmandSettings settings;
	private MapWidgetRegistry widgetRegistry;

	private WidgetsPanel selectedPanel;
	private ApplicationMode selectedAppMode;
	private WidgetIconsHelper iconsHelper;

	private View view;
	private View changeOrderListButton;
	private View changeOrderFooterButton;
	private ViewGroup enabledWidgetsContainer;
	private ViewGroup availableWidgetsContainer;

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

		NestedScrollView scrollView = view.findViewById(R.id.scroll_view);
		scrollView.getViewTreeObserver().addOnScrollChangedListener(this);

		updateContent();

		TextView panelTitle = view.findViewById(R.id.panel_title);
		panelTitle.setText(getString(selectedPanel.getTitleId(AndroidUtils.isLayoutRtl(app))));

		setupReorderButton(changeOrderListButton);
		setupReorderButton(changeOrderFooterButton);

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

			WidgetParams params = WidgetParams.getById(widgetInfo.key);
			WidgetGroup widgetGroup = params == null ? null : params.group;
			if (widgetGroup != null) {
				TextView description = view.findViewById(R.id.description);
				description.setText(widgetGroup.titleId);
				AndroidUiHelper.updateVisibility(description, true);
			}

			ImageView imageView = view.findViewById(R.id.icon);
			iconsHelper.updateWidgetIcon(imageView, widgetInfo);

			ImageView settingsIcon = view.findViewById(R.id.settings_icon);
			WidgetSettingsBaseFragment fragment = params != null ? params.getSettingsFragment() : null;
			if (fragment != null) {
				settingsIcon.setOnClickListener(v -> {
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
			}
			view.setOnClickListener(v -> settingsIcon.callOnClick());

			AndroidUiHelper.updateVisibility(settingsIcon, fragment != null);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.info_button), false);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), false);

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
			List<WidgetParams> disabledDefaultWidgets = listDefaultWidgets(availableWidgets);
			List<MapWidgetInfo> externalWidgets = listExternalWidgets(availableWidgets);

			inflateAvailableDefaultWidgets(excludeGroupsDuplicated(disabledDefaultWidgets), !Algorithms.isEmpty(externalWidgets));
			inflateAvailableExternalWidgets(externalWidgets);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.available_widgets_container), hasAvailableWidgets);
	}

	@NonNull
	private List<WidgetParams> excludeGroupsDuplicated(List<WidgetParams> widgets) {
		List<WidgetGroup> visitedGroups = new ArrayList<>();
		List<WidgetParams> matchingWidgets = new ArrayList<>();
		for (WidgetParams widget : widgets) {
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

	private void inflateAvailableDefaultWidgets(@NonNull List<WidgetParams> widgets, boolean hasExternalWidgets) {
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		for (int i = 0; i < widgets.size(); i++) {
			WidgetParams params = widgets.get(i);
			WidgetGroup widgetGroup = params.group;

			View view = inflater.inflate(R.layout.configure_screen_widget_item, availableWidgetsContainer, false);

			ImageView icon = view.findViewById(R.id.icon);
			if (widgetGroup != null) {
				icon.setImageResource(widgetGroup.getIconId(nightMode));
			} else {
				iconsHelper.updateWidgetIcon(icon, params);
			}

			CharSequence title = widgetGroup != null
					? AvailableItemViewHolder.getGroupTitle(widgetGroup, app, nightMode)
					: getString(params.titleId);
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
								selectedAppMode, selectedPanel, params, null);
					}
				}
			});
			view.setOnClickListener(v -> infoButton.callOnClick());

			AndroidUiHelper.updateVisibility(infoButton, true);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.settings_icon), false);
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
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.settings_icon), false);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), false);

			boolean last = i + 1 == externalWidgets.size();
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), !last);

			availableWidgetsContainer.addView(view);
		}
	}

	@NonNull
	private List<WidgetParams> listDefaultWidgets(@NonNull Set<MapWidgetInfo> widgets) {
		Map<Integer, WidgetParams> defaultWidgets = new TreeMap<>();
		for (MapWidgetInfo widgetInfo : widgets) {
			WidgetParams params = WidgetParams.getById(widgetInfo.key);
			if (params != null) {
				defaultWidgets.put(params.ordinal(), params);
			}
		}
		return new ArrayList<>(defaultWidgets.values());
	}

	@NonNull
	private List<MapWidgetInfo> listExternalWidgets(@NonNull Set<MapWidgetInfo> widgets) {
		List<MapWidgetInfo> externalWidgets = new ArrayList<>();
		for (MapWidgetInfo widgetInfo : widgets) {
			WidgetParams params = WidgetParams.getById(widgetInfo.key);
			if (params == null) {
				externalWidgets.add(widgetInfo);
			}
		}
		return externalWidgets;
	}

	private void setupListItemBackground(@NonNull View view) {
		View button = view.findViewById(R.id.container);
		int profileColor = selectedAppMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, profileColor, 0.3f);
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
}