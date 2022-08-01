package net.osmand.plus.views.mapwidgets.configure.panel;

import static net.osmand.plus.views.mapwidgets.MapWidgetInfo.DELIMITER;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.google.android.material.tabs.TabLayout.Tab;
import com.google.android.material.tabs.TabLayoutMediator;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.dialogs.AddWidgetFragment.AddWidgetListener;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ConfigureWidgetsFragment extends BaseOsmAndFragment implements WidgetsConfigurationChangeListener,
		OnOffsetChangedListener, AddWidgetListener {

	public static final String TAG = ConfigureWidgetsFragment.class.getSimpleName();

	private static final String APP_MODE_ATTR = "app_mode_key";
	private static final String SELECTED_GROUP_ATTR = "selected_group_key";

	private OsmandApplication app;
	private OsmandSettings settings;

	private MapLayers mapLayers;
	private MapWidgetRegistry widgetRegistry;
	private MapWidgetsFactory widgetsFactory;

	private WidgetsPanel selectedPanel;
	private ApplicationMode selectedAppMode;
	private WidgetsListFragment selectedFragment;

	private Toolbar toolbar;
	private TabLayout tabLayout;
	private ViewPager2 viewPager;
	private WidgetsTabAdapter widgetsTabAdapter;
	private View compensationView;

	private boolean nightMode;

	public void setSelectedPanel(@NonNull WidgetsPanel panel) {
		this.selectedPanel = panel;
	}

	public void setSelectedAppMode(@NonNull ApplicationMode appMode) {
		this.selectedAppMode = appMode;
	}

	public void setSelectedFragment(@Nullable WidgetsListFragment fragment) {
		this.selectedFragment = fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		mapLayers = app.getOsmandMap().getMapLayers();
		widgetRegistry = mapLayers.getMapWidgetRegistry();
		widgetsFactory = new MapWidgetsFactory((MapActivity) requireMyActivity());

		if (savedInstanceState != null) {
			String appModeKey = savedInstanceState.getString(APP_MODE_ATTR);
			selectedAppMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
			selectedPanel = WidgetsPanel.valueOf(savedInstanceState.getString(SELECTED_GROUP_ATTR));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		nightMode = !settings.isLightContent();
		inflater = UiUtilities.getInflater(requireContext(), nightMode);

		View view = inflater.inflate(R.layout.fragment_configure_widgets, container, false);
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(app, view);
		}
		AppBarLayout appBar = view.findViewById(R.id.appbar);
		appBar.addOnOffsetChangedListener(this);

		toolbar = view.findViewById(R.id.toolbar);
		tabLayout = view.findViewById(R.id.tab_layout);
		viewPager = view.findViewById(R.id.view_pager);
		compensationView = view.findViewById(R.id.compensation_view);

		setupToolbar();
		setupTabLayout();

		return view;
	}

	private void setupToolbar() {
		ImageButton backButton = toolbar.findViewById(R.id.back_button);
		backButton.setOnClickListener(view -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		UiUtilities.rotateImageByLayoutDirection(backButton);

		View infoButton = toolbar.findViewById(R.id.info_button);
		infoButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				AndroidUtils.openUrl(activity, R.string.docs_widgets, nightMode);
			}
		});

		View actionsButton = toolbar.findViewById(R.id.actions_button);
		actionsButton.setOnClickListener(v -> {
			if (selectedFragment != null) {
				selectedFragment.scrollToActions();
			}
		});

		TextView tvSubtitle = toolbar.findViewById(R.id.toolbar_subtitle);
		tvSubtitle.setText(selectedAppMode.toHumanString());
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(APP_MODE_ATTR, selectedAppMode.getStringKey());
		outState.putString(SELECTED_GROUP_ATTR, selectedPanel.name());
	}

	private void setupTabLayout() {
		widgetsTabAdapter = new WidgetsTabAdapter(this);
		viewPager.setAdapter(widgetsTabAdapter);
		viewPager.registerOnPageChangeCallback(new OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				selectedPanel = WidgetsPanel.values()[position];
			}
		});

		TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
		});
		mediator.attach();

		int profileColor = selectedAppMode.getProfileColor(nightMode);
		int defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		tabLayout.setSelectedTabIndicatorColor(profileColor);
		tabLayout.addOnTabSelectedListener(new OnTabSelectedListener() {

			@Override
			public void onTabSelected(Tab tab) {
				setupTabIconColor(tab, profileColor);
			}

			@Override
			public void onTabUnselected(Tab tab) {
				setupTabIconColor(tab, defaultIconColor);
			}

			@Override
			public void onTabReselected(Tab tab) {
			}

		});

		List<WidgetsPanel> panels = Arrays.asList(WidgetsPanel.values());
		for (int i = 0; i < tabLayout.getTabCount(); i++) {
			Tab tab = tabLayout.getTabAt(i);
			WidgetsPanel panel = panels.get(i);
			if (tab != null) {
				tab.setTag(panel);
				tab.setIcon(panel.getIconId(AndroidUtils.isLayoutRtl(app)));
			}
		}

		int position = panels.indexOf(selectedPanel);
		viewPager.setCurrentItem(position, false);

		if (position == 0) {
			Tab tab = tabLayout.getTabAt(position);
			setupTabIconColor(tab, profileColor);
		}
	}

	public void setupTabIconColor(@Nullable Tab tab, int color) {
		if (tab != null) {
			Drawable icon = tab.getIcon();
			if (icon != null) {
				icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
			}
		}
	}

	@Override
	public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
		int height = toolbar.getHeight() - Math.abs(verticalOffset);
		compensationView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height));
	}

	@Override
	public void onWidgetsConfigurationChanged() {
		if (selectedFragment != null) {
			selectedFragment.updateContent();
		}
	}

	@Override
	public void onWidgetsSelectedToAdd(@NonNull List<String> widgetsIds, @NonNull WidgetsPanel panel) {
		int filter = AVAILABLE_MODE | ENABLED_MODE;
		MapActivity mapActivity = requireMapActivity();
		for (String widgetId : widgetsIds) {
			MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
			Set<MapWidgetInfo> widgetInfos = widgetRegistry.getWidgetsForPanel(mapActivity, selectedAppMode,
					filter, Arrays.asList(WidgetsPanel.values()));
			if (panel.isDuplicatesAllowed() && (widgetInfo == null || widgetInfos.contains(widgetInfo))) {
				widgetInfo = createDuplicateWidget(widgetId, panel);
			}
			if (widgetInfo != null) {
				addWidgetToEnd(mapActivity, widgetInfo, panel);
				widgetRegistry.enableDisableWidgetForMode(selectedAppMode, widgetInfo, true, false);
			}
		}

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateControls();
		}
		onWidgetsConfigurationChanged();
	}

	private MapWidgetInfo createDuplicateWidget(@NonNull String widgetId, @NonNull WidgetsPanel panel) {
		WidgetType widgetType = WidgetType.getById(widgetId);
		if (widgetType != null) {
			String id = widgetId.contains(DELIMITER) ? widgetId : WidgetType.getDuplicateWidgetId(widgetId);
			MapWidget widget = widgetsFactory.createMapWidget(id, widgetType);
			if (widget != null) {
				settings.CUSTOM_WIDGETS_KEYS.addValue(id);
				return widgetRegistry.createCustomWidget(id, widget, widgetType, panel, selectedAppMode);
			}
		}
		return null;
	}

	private void addWidgetToEnd(@NonNull MapActivity mapActivity, @NonNull MapWidgetInfo targetWidget, @NonNull WidgetsPanel widgetsPanel) {
		Map<Integer, List<String>> pagedOrder = new TreeMap<>();
		Set<MapWidgetInfo> enabledWidgets = widgetRegistry.getWidgetsForPanel(mapActivity,
				selectedAppMode, ENABLED_MODE, Collections.singletonList(widgetsPanel));

		widgetRegistry.getWidgetsForPanel(targetWidget.widgetPanel).remove(targetWidget);
		targetWidget.widgetPanel = widgetsPanel;

		for (MapWidgetInfo widget : enabledWidgets) {
			int page = widget.pageIndex;
			List<String> orders = pagedOrder.get(page);
			if (orders == null) {
				orders = new ArrayList<>();
				pagedOrder.put(page, orders);
			}
			orders.add(widget.key);
		}

		if (Algorithms.isEmpty(pagedOrder)) {
			targetWidget.pageIndex = 0;
			targetWidget.priority = 0;
			widgetRegistry.getWidgetsForPanel(widgetsPanel).add(targetWidget);

			List<List<String>> flatOrder = new ArrayList<>();
			flatOrder.add(Collections.singletonList(targetWidget.key));
			widgetsPanel.setWidgetsOrder(selectedAppMode, flatOrder, settings);
		} else {
			List<Integer> pages = new ArrayList<>(pagedOrder.keySet());
			List<List<String>> orders = new ArrayList<>(pagedOrder.values());
			List<String> lastPageOrder = orders.get(orders.size() - 1);

			lastPageOrder.add(targetWidget.key);

			String previousLastWidgetId = lastPageOrder.get(lastPageOrder.size() - 2);
			MapWidgetInfo previousLastVisibleWidgetInfo = widgetRegistry.getWidgetInfoById(previousLastWidgetId);
			int lastPage;
			int lastOrder;
			if (previousLastVisibleWidgetInfo != null) {
				lastPage = previousLastVisibleWidgetInfo.pageIndex;
				lastOrder = previousLastVisibleWidgetInfo.priority + 1;
			} else {
				lastPage = pages.get(pages.size() - 1);
				lastOrder = lastPageOrder.size() - 1;
			}

			targetWidget.pageIndex = lastPage;
			targetWidget.priority = lastOrder;
			widgetRegistry.getWidgetsForPanel(widgetsPanel).add(targetWidget);

			widgetsPanel.setWidgetsOrder(selectedAppMode, orders, settings);
		}
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@NonNull
	public MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (!(activity instanceof MapActivity)) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return (MapActivity) activity;
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ConfigureWidgetsFragment fragment = new ConfigureWidgetsFragment();
			fragment.setSelectedPanel(panel);
			fragment.setSelectedAppMode(appMode);
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}