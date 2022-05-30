package net.osmand.plus.views.mapwidgets.configure.panel;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.DISABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;

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

	private View view;
	private View changeOrderListButton;
	private View changeOrderFooterButton;
	private ViewGroup enabledWidgetsContainer;
	private ViewGroup availableWidgetsContainer;

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
		selectedAppMode = settings.getApplicationMode();
		nightMode = !settings.isLightContent();
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

		int enabledAvailableFilter = ENABLED_MODE | AVAILABLE_MODE;
		Set<MapWidgetInfo> enabledWidgets = widgetRegistry.getWidgetsForPanel(selectedAppMode,
				enabledAvailableFilter, selectedPanel);
		boolean noEnabledWidgets = Algorithms.isEmpty(enabledWidgets);

		View noWidgetsContainer = view.findViewById(R.id.no_widgets_container);
		AndroidUiHelper.updateVisibility(noWidgetsContainer, noEnabledWidgets);
		if (noEnabledWidgets) {
			ImageView noWidgetsImage = view.findViewById(R.id.no_widgets_image);
			boolean rtl = AndroidUtils.isLayoutRtl(app);
			Drawable noWidgetsIcon = app.getUIUtilities().getIcon(selectedPanel.getIconId(rtl), nightMode);
			noWidgetsImage.setImageDrawable(noWidgetsIcon);
		} else {
			inflateEnabledWidgets();
		}
	}

	private void inflateEnabledWidgets() {
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		int enabledAvailableFilter = AVAILABLE_MODE | ENABLED_MODE;
		if (selectedPanel.isPagingAllowed()) {
			List<Set<MapWidgetInfo>> pagedWidgets =
					widgetRegistry.getPagedWidgetsForPanel(selectedAppMode, selectedPanel, enabledAvailableFilter);
			for (int i = 0; i < pagedWidgets.size(); i++) {
				inflatePageItemView(i, inflater);
				inflateWidgetItemsViews(pagedWidgets.get(i), inflater);
			}
		} else {
			Set<MapWidgetInfo> widgets = widgetRegistry
					.getWidgetsForPanel(selectedAppMode, enabledAvailableFilter, selectedPanel);
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

	private void inflateWidgetItemsViews(@NonNull Set<MapWidgetInfo> widgetsInfo,
	                                     @NonNull LayoutInflater inflater) {
		int profileColor = selectedAppMode.getProfileColor(nightMode);
		WidgetIconsHelper iconsHelper = new WidgetIconsHelper(app, profileColor, nightMode);
		List<MapWidgetInfo> widgets = new ArrayList<>(widgetsInfo);

		for (int i = 0; i < widgets.size(); i++) {
			MapWidgetInfo widgetInfo = widgets.get(i);

			View view = inflater.inflate(R.layout.configure_screen_widget_item, enabledWidgetsContainer, false);

			TextView title = view.findViewById(R.id.title);
			title.setText(widgetInfo.getTitle(app));

			WidgetParams widgetParams = WidgetParams.getById(widgetInfo.key);
			WidgetGroup widgetGroup = widgetParams == null ? null : widgetParams.group;
			if (widgetGroup != null) {
				TextView description = view.findViewById(R.id.description);
				description.setText(widgetGroup.titleId);
				AndroidUiHelper.updateVisibility(description, true);
			}

			ImageView imageView = view.findViewById(R.id.icon);
			iconsHelper.updateWidgetIcon(imageView, widgetInfo);

			ImageView settingsIcon = view.findViewById(R.id.settings_icon);
			WidgetSettingsBaseFragment settingsFragment = widgetParams != null
					? widgetParams.getSettingsFragment()
					: null;
			if (settingsFragment != null) {
				settingsIcon.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager fragmentManager = activity.getSupportFragmentManager();
						Fragment target = getParentFragment();
						WidgetSettingsBaseFragment.showFragment(fragmentManager, target, selectedAppMode, settingsFragment);
					}
				});
			}
			view.setOnClickListener(v -> settingsIcon.callOnClick());

			AndroidUiHelper.updateVisibility(settingsIcon, settingsFragment != null);
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

		int disabledAvailableFilter = AVAILABLE_MODE | DISABLED_MODE;
		Set<MapWidgetInfo> disabledWidgets = widgetRegistry
				.getWidgetsForPanel(selectedAppMode, disabledAvailableFilter, selectedPanel.getMergedPanels());
		boolean allWidgetsEnabled = Algorithms.isEmpty(disabledWidgets);

		View availableWidgetsCard = view.findViewById(R.id.available_widgets_container);
		AndroidUiHelper.updateVisibility(availableWidgetsCard, !allWidgetsEnabled);
		if (!allWidgetsEnabled) {
			List<WidgetParams> disabledDefaultWidgets = listDefaultWidgets(disabledWidgets);
			List<MapWidgetInfo> externalWidgets = listExternalWidgets(disabledWidgets);

			inflateAvailableDefaultWidgets(excludeGroupsDuplicated(disabledDefaultWidgets), !Algorithms.isEmpty(externalWidgets));
			inflateAvailableExternalWidgets(externalWidgets);
		}
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
		int profileColor = selectedAppMode.getProfileColor(nightMode);
		WidgetIconsHelper iconsHelper = new WidgetIconsHelper(app, profileColor, nightMode);


		for (int i = 0; i < widgets.size(); i++) {
			WidgetParams widget = widgets.get(i);
			WidgetGroup widgetGroup = widget.group;

			View view = inflater.inflate(R.layout.configure_screen_widget_item, availableWidgetsContainer, false);

			ImageView icon = view.findViewById(R.id.icon);
			if (widgetGroup != null) {
				icon.setImageResource(widgetGroup.getIconId(nightMode));
			} else {
				iconsHelper.updateWidgetIcon(icon, widget);
			}

			CharSequence title = widgetGroup != null
					? AvailableItemViewHolder.getGroupTitle(widgetGroup, app, nightMode)
					: getString(widget.titleId);
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
								selectedAppMode, selectedPanel, widget, null);
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
		int profileColor = selectedAppMode.getProfileColor(nightMode);
		WidgetIconsHelper iconsHelper = new WidgetIconsHelper(app, profileColor, nightMode);

		for (int i = 0; i < externalWidgets.size(); i++) {
			MapWidgetInfo widget = externalWidgets.get(i);

			View view = inflater.inflate(R.layout.configure_screen_widget_item, availableWidgetsContainer, false);

			ImageView icon = view.findViewById(R.id.icon);
			iconsHelper.updateWidgetIcon(icon, widget);

			TextView title = view.findViewById(R.id.title);
			title.setText(widget.getTitle(app));

			View infoButton = view.findViewById(R.id.info_button);
			infoButton.setOnClickListener(v -> {
				FragmentActivity activity = getActivity();
				String externalProviderPackage = widget.getExternalProviderPackage();
				if (activity != null && !Algorithms.isEmpty(externalProviderPackage)) {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					AddWidgetFragment.showExternalWidgetDialog(fragmentManager, getParentFragment(),
							selectedAppMode, selectedPanel, widget.key, externalProviderPackage, null);
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
}