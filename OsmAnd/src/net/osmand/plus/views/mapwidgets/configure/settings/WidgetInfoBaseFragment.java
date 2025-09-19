package net.osmand.plus.views.mapwidgets.configure.settings;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.aidl.AidlMapWidgetWrapper;
import net.osmand.aidl.ConnectedApp;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.dialogs.DeleteWidgetConfirmationController;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.banner.WidgetPromoBanner;
import net.osmand.plus.views.mapwidgets.configure.panel.ConfigureWidgetsController;
import net.osmand.plus.views.mapwidgets.configure.panel.ConfigureWidgetsFragment;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsConfigurationChangeListener;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WidgetInfoBaseFragment extends BaseFullScreenFragment {

	public static final String KEY_APP_MODE = "app_mode";
	public static final String KEY_WIDGET_ID = "widget_id";
	public static final String KEY_ADD_MODE = "add_mode_key";
	public static final String KEY_SELECTED_PANEL = "selected_panel_key";

	protected ConfigureWidgetsController controller;
	protected ApplicationMode appMode;
	protected MapWidgetRegistry widgetRegistry;

	protected MapWidgetInfo widgetInfo;
	protected WidgetsPanel widgetPanel;
	protected String widgetId;

	private View promoBannerContainer;
	private ViewGroup promoBanner;
	private MenuProvider menuProvider;
	protected View view;

	private boolean addNewWidgetMode = false;
	protected boolean isVerticalPanel;

	@NonNull
	public WidgetType getWidget() {
		return widgetInfo.widget.getWidgetType();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		DialogManager dialogManager = app.getDialogManager();
		controller = (ConfigureWidgetsController) dialogManager.findController(ConfigureWidgetsController.PROCESS_ID);
		DeleteWidgetConfirmationController.askUpdateListener(app, this::dismiss);

		createMenuProvider();
	}

	private void createMenuProvider() {
		menuProvider = new MenuProvider() {
			@Override
			public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
				menu.clear();
				if (!addNewWidgetMode) {
					MenuItem deleteAction = menu.add(0, R.id.delete_button, 0, R.string.delete_widget);
					deleteAction.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined));
					deleteAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
					deleteAction.setOnMenuItemClickListener(item -> {
						showDeleteWidgetConfirmationDialog();
						return true;
					});

					MenuItem menuAction = menu.add(0, R.id.menu_button, 1, R.string.shared_string_menu);
					menuAction.setIcon(getContentIcon(R.drawable.ic_overflow_menu_white));
					menuAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
					menuAction.setOnMenuItemClickListener(item -> {
						View anchorView = view.findViewById(R.id.menu_button);
						List<PopUpMenuItem> items = new ArrayList<>();

						items.add(new PopUpMenuItem.Builder(app)
								.setTitle(app.getString(R.string.shared_string_duplicate))
								.setIcon(getContentIcon(R.drawable.ic_action_copy))
								.setOnClickListener(v -> {
									String duplicateId = createDuplicateWidget();
									if (duplicateId != null) {
										showDuplicateAddedSnackbar();
									}
								}).create());

						PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
						displayData.anchorView = anchorView;
						displayData.menuItems = items;
						displayData.nightMode = nightMode;
						PopUpMenu.show(displayData);
						return false;
					});
				}
			}

			@Override
			public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
				return false;
			}
		};
	}

	private void showDeleteWidgetConfirmationDialog() {
		callActivity(activity -> { if (widgetInfo != null) {
			DeleteWidgetConfirmationController.showDialog(activity, appMode, widgetInfo, isUsedOnMap(), this::dismiss);
		}});
	}

	private void showDuplicateAddedSnackbar() {
		Snackbar snackbar = Snackbar.make(view, R.string.duplacate_widget_added_snackbar, Snackbar.LENGTH_LONG);
		UiUtilities.setupSnackbar(snackbar, nightMode);
		snackbar.show();
	}

	@Nullable
	private String createDuplicateWidget() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return null;
		}

		int filter = ENABLED_MODE | MATCHING_PANELS_MODE;
		List<WidgetsPanel> panels = Collections.singletonList(widgetPanel);
		List<MapWidgetInfo> widgetInfos = new ArrayList<>(widgetRegistry.getWidgetsForPanel(mapActivity, appMode, filter, panels));

		int index = widgetInfos.indexOf(widgetInfo);
		if (index == -1) {
			return null;
		}

		WidgetType widgetType = getWidget();
		String duplicateId = WidgetType.getDuplicateWidgetId(widgetType);
		MapWidget duplicateWidget = new MapWidgetsFactory(mapActivity).createMapWidget(duplicateId, widgetType, widgetPanel);
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);
		MapWidgetInfo duplicateWidgetInfo = creator.askCreateWidgetInfo(duplicateId, duplicateWidget, widgetType, widgetPanel);
		if (duplicateWidgetInfo == null) {
			return null;
		}
		settings.CUSTOM_WIDGETS_KEYS.addModeValue(appMode, duplicateId);
		WidgetState widgetState = widgetInfo.getWidgetState();
		if (widgetState != null) {
			widgetState.copyPrefs(appMode, duplicateId);
		}
		duplicateWidgetInfo.enableDisableForMode(appMode, true);
		widgetInfo.widget.copySettings(appMode, duplicateId);

		Map<Integer, List<String>> pagedOrder = new LinkedHashMap<>();
		for (MapWidgetInfo widgetInfo : widgetInfos) {
			List<String> orderOfPage = pagedOrder.computeIfAbsent(widgetInfo.pageIndex, k -> new ArrayList<>());

			orderOfPage.add(widgetInfo.key);
			if (this.widgetInfo.key.equals(widgetInfo.key)) {
				orderOfPage.add(duplicateId);
			}
		}

		widgetPanel.setWidgetsOrder(appMode, new ArrayList<>(pagedOrder.values()), settings);

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateAllControls(mapActivity);
		}

		return duplicateId;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (savedInstanceState != null) {
			initParams(savedInstanceState);
		} else if (args != null) {
			initParams(args);
		}

		updateNightMode();
		view = inflate(R.layout.widget_settings_info_fragment, container, false);
		if (widgetInfo == null) {
			return view;
		}
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
			view.setFitsSystemWindows(true);
		}
		promoBannerContainer = view.findViewById(R.id.promo_banner_container);
		promoBanner = view.findViewById(R.id.promo_banner);

		setupToolbar();
		setupInfo();
		setupTopContent(view.findViewById(R.id.top_settings_container));
		setupMainContent(view.findViewById(R.id.main_settings_container));
		setupApplyButton();
		updateStatusBar();

		return view;
	}

	@Nullable
	@Override
	public List<Integer> getCollapsingAppBarLayoutId() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.appbar);
		return ids;
	}

	protected void initParams(@NonNull Bundle bundle) {
		widgetId = bundle.getString(KEY_WIDGET_ID);
		appMode = ApplicationMode.valueOfStringKey(bundle.getString(KEY_APP_MODE), settings.getApplicationMode());
		addNewWidgetMode = bundle.getBoolean(KEY_ADD_MODE, false);
		widgetPanel = WidgetsPanel.valueOf(bundle.getString(KEY_SELECTED_PANEL));
		isVerticalPanel = widgetPanel.isPanelVertical();

		if (addNewWidgetMode && controller != null) {
			MapWidgetInfo controllerAddedWidgetInfo = controller.getAddedWidget();
			if (controllerAddedWidgetInfo != null) {
				widgetInfo = controllerAddedWidgetInfo;
			}
		}
		if (widgetInfo == null) {
			widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		}

		if (widgetInfo == null) {
			dismiss();
		}
	}

	private void setupToolbar() {
		WidgetType widget = getWidget();

		String title = app.getString(widget.titleId);
		if (widgetInfo.isExternal() && !Algorithms.isEmpty(widgetInfo.getExternalProviderPackage())) {
			ConnectedApp connectedApp = app.getAidlApi().getConnectedApp(widgetInfo.getExternalProviderPackage());
			if (connectedApp != null) {
				AidlMapWidgetWrapper aidlMapWidgetWrapper = connectedApp.getWidgetData(widgetId);
				title = aidlMapWidgetWrapper.getMenuTitle();
			}
		}

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getContentIcon(addNewWidgetMode ? R.drawable.ic_action_close : AndroidUtils.getNavigationIconResId(app)));
		toolbar.setNavigationContentDescription(addNewWidgetMode ? R.string.shared_string_close : R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> {
			dismiss();
		});
		toolbar.addMenuProvider(menuProvider);
		toolbar.setTitle(title);
		toolbar.setTitleTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
	}

	protected void dismiss() {
		requireActivity().getSupportFragmentManager().popBackStack();
		if (controller != null) {
			controller.resetAddedWidget();
		}
	}

	private void setupInfo() {
		WidgetType widgetType = widgetInfo.getWidgetType();

		TextView tvDesc = view.findViewById(R.id.widget_description);
		String externalProviderPackage = widgetInfo.getExternalProviderPackage();
		if (widgetInfo.isExternal() && externalProviderPackage != null) {
			ConnectedApp connectedApp = app.getAidlApi().getConnectedApp(externalProviderPackage);
			if (connectedApp != null) {
				tvDesc.setText(connectedApp.getName());
			}
		} else {
			tvDesc.setText(widgetType.descId);
			MapActivity mapActivity = getMapActivity();
			if (!widgetType.isPurchased(app) && mapActivity != null) {
				AndroidUiHelper.updateVisibility(promoBannerContainer, true);
				WidgetPromoBanner banner = new WidgetPromoBanner(mapActivity, widgetType, false);
				promoBanner.addView(banner.build());
			}
		}
	}

	protected void setupTopContent(@NonNull ViewGroup container) {
	}

	protected void setupMainContent(@NonNull ViewGroup container) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.main_container), false);
	}

	private void setupApplyButton() {
		View buttonsContainer = view.findViewById(R.id.bottom_buttons_container);
		if (addNewWidgetMode) {
			buttonsContainer.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
			DialogButton applyButton = view.findViewById(R.id.dismiss_button);
			applyButton.setButtonType(DialogButtonType.PRIMARY);
			applyButton.setTitleId(addNewWidgetMode ? R.string.add_widget : R.string.shared_string_apply);

			applyButton.setOnClickListener(v -> {
				Fragment target = getTargetFragment();
				if (target instanceof WidgetsConfigurationChangeListener configurationChangeListener) {
					if (addNewWidgetMode && widgetInfo != null) {
						configurationChangeListener.onWidgetAdded(widgetInfo);
					}
				}
				FragmentManager fragmentManager = requireMyActivity().getSupportFragmentManager();
				int backStackCount = fragmentManager.getBackStackEntryCount();

				if (backStackCount == 0) return;
				Fragment fragment = fragmentManager.findFragmentByTag(ConfigureWidgetsFragment.TAG);

				if (fragment != null) {
					fragmentManager.popBackStack(ConfigureWidgetsFragment.TAG, 0);
				} else {
					dismiss();
				}
			});
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.apply_button_container), addNewWidgetMode);
	}

	protected void applySettings() {

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		applySettings();
		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateControls();
		}
		Fragment target = getTargetFragment();
		if (target instanceof WidgetsConfigurationChangeListener) {
			((WidgetsConfigurationChangeListener) target).onWidgetsConfigurationChanged();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_APP_MODE, appMode.getStringKey());
		outState.putString(KEY_WIDGET_ID, widgetId);
		outState.putBoolean(KEY_ADD_MODE, addNewWidgetMode);
		outState.putString(KEY_SELECTED_PANEL, widgetPanel.name());
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
	protected Drawable getPressedStateDrawable() {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		return UiUtilities.getColoredSelectableDrawable(app, activeColor);
	}

	private static void showInstance(@NonNull FragmentManager manager, @NonNull WidgetInfoBaseFragment fragment,
	                                 @Nullable Fragment target, @NonNull ApplicationMode appMode, @NonNull String widgetId, @NonNull WidgetsPanel widgetsPanel, boolean addNewWidgetMode) {
		String tag = fragment.getClass().getSimpleName();
		if (AndroidUtils.isFragmentCanBeAdded(manager, tag, true)) {
			Bundle args = new Bundle();
			args.putString(KEY_WIDGET_ID, widgetId);
			args.putString(KEY_APP_MODE, appMode.getStringKey());
			args.putBoolean(KEY_ADD_MODE, addNewWidgetMode);
			args.putString(KEY_SELECTED_PANEL, widgetsPanel.name());

			fragment.setArguments(args);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, tag)
					.addToBackStack(tag)
					.commitAllowingStateLoss();
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull WidgetInfoBaseFragment fragment,
	                                @Nullable Fragment target, @NonNull ApplicationMode appMode, @NonNull String widgetId, @NonNull WidgetsPanel widgetsPanel) {
		showInstance(manager, fragment, target, appMode, widgetId, widgetsPanel, false);
	}

	public static void showAddWidgetFragment(@NonNull FragmentManager manager, @NonNull WidgetInfoBaseFragment fragment,
	                                         @Nullable Fragment target, @NonNull ApplicationMode appMode, @NonNull String widgetId, @NonNull WidgetsPanel widgetsPanel) {
		showInstance(manager, fragment, target, appMode, widgetId, widgetsPanel, true);
	}


}