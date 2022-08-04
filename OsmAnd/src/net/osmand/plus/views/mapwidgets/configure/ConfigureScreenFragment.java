package net.osmand.plus.views.mapwidgets.configure;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.AppBarLayout.Behavior;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.quickaction.QuickActionListFragment;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.quickaction.QuickActionRegistry.QuickActionUpdatesListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WidgetsRegistryListener;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.CompassVisibilityBottomSheetDialogFragment.CompassVisibility;
import net.osmand.plus.views.mapwidgets.configure.CompassVisibilityBottomSheetDialogFragment.CompassVisibilityUpdateListener;
import net.osmand.plus.views.mapwidgets.configure.ConfirmResetToDefaultBottomSheetDialog.ResetToDefaultListener;
import net.osmand.plus.views.mapwidgets.configure.panel.ConfigureWidgetsFragment;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigureScreenFragment extends BaseOsmAndFragment implements QuickActionUpdatesListener,
		WidgetsRegistryListener, ResetToDefaultListener, CopyAppModePrefsListener, CompassVisibilityUpdateListener {

	public static final String TAG = ConfigureScreenFragment.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;
	private MapWidgetRegistry widgetRegistry;
	private WidgetsSettingsHelper widgetsSettingsHelper;
	private ApplicationMode selectedAppMode;

	private MapActivity mapActivity;
	private LayoutInflater themedInflater;

	private AppBarLayout appBar;
	private Toolbar toolbar;
	private HorizontalChipsView modesToggle;
	private LinearLayout widgetsCard;
	private LinearLayout buttonsCard;
	private ViewGroup actionsCardContainer;
	private NestedScrollView scrollView;

	private boolean nightMode;
	private int currentScrollY;
	private int currentAppBarOffset;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		mapActivity = (MapActivity) requireMyActivity();
		selectedAppMode = settings.getApplicationMode();
		widgetRegistry = mapActivity.getMapLayers().getMapWidgetRegistry();
		widgetsSettingsHelper = new WidgetsSettingsHelper(mapActivity, selectedAppMode);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		nightMode = !settings.isLightContent();
		themedInflater = UiUtilities.getInflater(getContext(), nightMode);

		View view = themedInflater.inflate(R.layout.fragment_configure_screen, container, false);
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(app, view);
		}

		appBar = view.findViewById(R.id.appbar);
		toolbar = view.findViewById(R.id.toolbar);
		modesToggle = view.findViewById(R.id.modes_toggle);
		widgetsCard = view.findViewById(R.id.widgets_card);
		buttonsCard = view.findViewById(R.id.buttons_card);
		scrollView = view.findViewById(R.id.scroll_view);
		actionsCardContainer = view.findViewById(R.id.configure_screen_actions_container);

		setupAppBar();
		setupToolbar();
		setupModesToggle();
		setupWidgetsCard();
		setupButtonsCard();
		setupActionsCard();

		if (currentScrollY > 0) {
			scrollView.scrollTo(0, currentScrollY);
		}

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		setupWidgetsCard();
	}

	@Override
	public void onStart() {
		super.onStart();
		app.getQuickActionRegistry().addUpdatesListener(this);
		widgetRegistry.addWidgetsRegistryListener(this);
		mapActivity.disableDrawer();
	}

	@Override
	public void onStop() {
		super.onStop();
		app.getQuickActionRegistry().removeUpdatesListener(this);
		widgetRegistry.removeWidgetsRegistryListener(this);
		mapActivity.enableDrawer();
	}

	private void setupAppBar() {
		appBar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
			currentAppBarOffset = verticalOffset;
		});
		CoordinatorLayout.LayoutParams param = (CoordinatorLayout.LayoutParams) appBar.getLayoutParams();
		param.setBehavior(new AppBarLayout.Behavior());
		setAppBarOffset(currentAppBarOffset);
	}

	private void setAppBarOffset(int verticalOffset) {
		CoordinatorLayout.LayoutParams param = (CoordinatorLayout.LayoutParams) appBar.getLayoutParams();
		AppBarLayout.Behavior behavior = (Behavior) param.getBehavior();
		if (behavior != null) {
			behavior.setTopAndBottomOffset(verticalOffset);
		}
	}

	private void setupToolbar() {
		ImageButton backButton = toolbar.findViewById(R.id.back_button);
		backButton.setOnClickListener(view -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		backButton.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(app)));

		View infoButton = toolbar.findViewById(R.id.info_button);
		infoButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				AndroidUtils.openUrl(activity, R.string.docs_widget_configure_screen, nightMode);
			}
		});

		View actionsButton = toolbar.findViewById(R.id.actions_button);
		actionsButton.setOnClickListener(v -> {
			appBar.setExpanded(false);
			scrollView.smoothScrollTo(0, (int) actionsCardContainer.getY());
		});
	}

	private void setupModesToggle() {
		ChipItem selectedItem = null;
		List<ChipItem> items = new ArrayList<>();
		for (ApplicationMode mode : ApplicationMode.values(app)) {
			ChipItem item = new ChipItem(mode.getStringKey());
			int profileColor = mode.getProfileColor(nightMode);
			int bgSelectedColor = ColorUtilities.getColorWithAlpha(profileColor, 0.25f);
			// Do not use iconsCache to prevent same coloring for profiles with same icons
			item.icon = ContextCompat.getDrawable(app, mode.getIconRes());
			item.iconColor = profileColor;
			item.iconSelectedColor = profileColor;
			item.strokeSelectedColor = profileColor;
			item.strokeSelectedWidth = AndroidUtils.dpToPx(app, 2);
			item.rippleColor = profileColor;
			item.bgSelectedColor = bgSelectedColor;
			item.tag = mode;
			if (Algorithms.objectEquals(selectedAppMode, mode)) {
				selectedItem = item;
			}
			items.add(item);
		}
		modesToggle.setItems(items);
		modesToggle.setSelected(selectedItem);
		modesToggle.setOnSelectChipListener(chip -> {
			if (chip.tag instanceof ApplicationMode) {
				ApplicationMode mode = (ApplicationMode) chip.tag;
				if (!Algorithms.stringsEqual(mode.getStringKey(), selectedAppMode.getStringKey())) {
					selectedAppMode = mode;
					modesToggle.scrollTo(chip);
					settings.setApplicationMode(mode);
					updateFragment();
				}
			}
			return true;
		});
		if (selectedItem != null) {
			modesToggle.scrollTo(selectedItem);
		}
	}

	private void setupWidgetsCard() {
		widgetsCard.removeAllViews();
		widgetsCard.addView(createWidgetGroupView(WidgetsPanel.LEFT, false, false));
		widgetsCard.addView(createWidgetGroupView(WidgetsPanel.RIGHT, true, false));
		widgetsCard.addView(createWidgetGroupView(WidgetsPanel.TOP, false, false));
		widgetsCard.addView(createWidgetGroupView(WidgetsPanel.BOTTOM, false, true));

		widgetsCard.addView(createButtonWithSwitch(R.drawable.ic_action_appearance, getString(R.string.map_widget_transparent),
				settings.TRANSPARENT_MAP_THEME.getModeValue(selectedAppMode),
				false,
				v -> {
					boolean enabled = settings.TRANSPARENT_MAP_THEME.get();
					settings.TRANSPARENT_MAP_THEME.setModeValue(selectedAppMode, !enabled);
					mapActivity.updateApplicationModeSettings();
				}
		));
	}

	private void setupButtonsCard() {
		buttonsCard.removeAllViews();

		CompassVisibility compassVisibility = settings.COMPASS_VISIBILITY.getModeValue(selectedAppMode);
		buttonsCard.addView(createButtonWithDesc(
				compassVisibility.iconId,
				getString(R.string.map_widget_compass),
				compassVisibility.getTitle(app),
				true,
				v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager fragmentManager = activity.getSupportFragmentManager();
						CompassVisibilityBottomSheetDialogFragment.showInstance(fragmentManager, this, selectedAppMode);
					}
				}
		));

		buttonsCard.addView(createButtonWithSwitch(
				R.drawable.ic_action_ruler_line,
				getString(R.string.map_widget_distance_by_tap),
				settings.SHOW_DISTANCE_RULER.getModeValue(selectedAppMode),
				true,
				v -> {
					boolean enabled = settings.SHOW_DISTANCE_RULER.getModeValue(selectedAppMode);
					settings.SHOW_DISTANCE_RULER.setModeValue(selectedAppMode, !enabled);
					mapActivity.updateApplicationModeSettings();
				}
		));

		QuickActionRegistry registry = app.getQuickActionRegistry();
		int actionsCount = registry.getQuickActions().size();
		String actions = getString(R.string.shared_string_actions);
		String desc = getString(R.string.ltr_or_rtl_combine_via_colon, actions, String.valueOf(actionsCount));
		buttonsCard.addView(createButtonWithDesc(
				R.drawable.ic_quick_action,
				getString(R.string.configure_screen_quick_action),
				desc,
				settings.QUICK_ACTION.getModeValue(selectedAppMode),
				v -> QuickActionListFragment.showInstance(requireActivity(), false)
		));
	}

	private void setupActionsCard() {
		ConfigureScreenActionsCard card = new ConfigureScreenActionsCard(mapActivity, this,
				selectedAppMode, R.string.map_widget_config);
		actionsCardContainer.addView(card.build(actionsCardContainer.getContext()));
	}

	@Override
	public void onResetToDefaultConfirmed() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		widgetsSettingsHelper.setAppMode(selectedAppMode);
		widgetsSettingsHelper.resetConfigureScreenSettings();
		recreateControlsCompletely(mapActivity);
		updateFragment();
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode fromAppMode) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		widgetsSettingsHelper.setAppMode(selectedAppMode);
		widgetsSettingsHelper.copyConfigureScreenSettings(fromAppMode);
		recreateControlsCompletely(mapActivity);
		updateFragment();
	}

	private void recreateControlsCompletely(@NonNull MapActivity mapActivity) {
		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateAllControls(mapActivity);
		}
	}

	@Override
	public void onActionsUpdated() {
		setupButtonsCard();
	}

	@Override
	public void onCompassVisibilityUpdated(@NonNull CompassVisibility visibility) {
		setupButtonsCard();
	}

	@Override
	public void onWidgetRegistered(@NonNull MapWidgetInfo widgetInfo) {
		updateWidgetsCountForPanel(widgetInfo.widgetPanel);
	}

	private void updateWidgetsCountForPanel(@NonNull WidgetsPanel panel) {
		MapActivity mapActivity = getMapActivity();
		View panelContainer = widgetsCard.findViewWithTag(panel.name());
		if (mapActivity != null && panelContainer != null) {
			int count = getWidgetsCount(mapActivity, panel);
			updateWidgetsCount(panelContainer, count);
		}
	}

	@Override
	public void onWidgetVisibilityChanged(@NonNull MapWidgetInfo widgetInfo) {

	}

	@Override
	public void onWidgetsCleared() {

	}

	private View createWidgetGroupView(@NonNull WidgetsPanel panel, boolean showShortDivider, boolean showLongDivider) {
		boolean rtl = AndroidUtils.isLayoutRtl(app);
		int activeColor = selectedAppMode.getProfileColor(nightMode);
		int defColor = ColorUtilities.getDefaultIconColor(app, nightMode);

		View view = themedInflater.inflate(R.layout.configure_screen_list_item, null);
		view.setTag(panel.name());
		ImageView ivIcon = view.findViewById(R.id.icon);
		TextView tvTitle = view.findViewById(R.id.title);

		MapActivity mapActivity = requireMapActivity();
		int count = getWidgetsCount(mapActivity, panel);
		int iconColor = count > 0 ? activeColor : defColor;
		Drawable icon = getPaintedContentIcon(panel.getIconId(rtl), iconColor);
		ivIcon.setImageDrawable(icon);

		String title = getString(panel.getTitleId(rtl));
		tvTitle.setText(title);

		updateWidgetsCount(view, count);

		if (showShortDivider) {
			view.findViewById(R.id.short_divider).setVisibility(View.VISIBLE);
		}

		if (showLongDivider) {
			view.findViewById(R.id.long_divider).setVisibility(View.VISIBLE);
		}

		setupClickListener(view, v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				ConfigureWidgetsFragment.showInstance(activity, panel, selectedAppMode);
			}
		});
		setupListItemBackground(view);
		return view;
	}

	private int getWidgetsCount(@NonNull MapActivity mapActivity, @NonNull WidgetsPanel panel) {
		int filter = ENABLED_MODE | AVAILABLE_MODE;
		return widgetRegistry.getWidgetsForPanel(mapActivity, selectedAppMode, filter, Collections.singletonList(panel)).size();
	}

	private void updateWidgetsCount(@NonNull View container, int count) {
		TextView countContainer = container.findViewById(R.id.items_count_descr);
		countContainer.setText(String.valueOf(count));
		AndroidUiHelper.updateVisibility(countContainer, true);
	}

	private View createButtonWithSwitch(int iconId,
	                                    @NonNull String title,
	                                    boolean enabled,
	                                    boolean showShortDivider,
	                                    @Nullable OnClickListener listener) {
		int activeColor = selectedAppMode.getProfileColor(nightMode);
		int defColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = enabled ? activeColor : defColor;
		View view = themedInflater.inflate(R.layout.configure_screen_list_item, null);

		Drawable icon = getPaintedContentIcon(iconId, iconColor);
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageDrawable(icon);
		ivIcon.setColorFilter(enabled ? activeColor : defColor);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);

		CompoundButton cb = view.findViewById(R.id.compound_button);
		cb.setChecked(enabled);
		cb.setVisibility(View.VISIBLE);
		UiUtilities.setupCompoundButton(nightMode, activeColor, cb);

		if (showShortDivider) {
			view.findViewById(R.id.short_divider).setVisibility(View.VISIBLE);
		}

		cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
			ivIcon.setColorFilter(isChecked ? activeColor : defColor);
			if (listener != null) {
				listener.onClick(buttonView);
			}
		});
		setupClickListener(view, v -> {
			boolean newState = !cb.isChecked();
			cb.setChecked(newState);
		});
		setupListItemBackground(view);

		return view;
	}

	private View createButtonWithDesc(int iconId,
	                                  @NonNull String title,
	                                  @Nullable String desc,
	                                  boolean enabled,
	                                  OnClickListener listener) {
		int activeColor = selectedAppMode.getProfileColor(nightMode);
		int defColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = enabled ? activeColor : defColor;
		View view = themedInflater.inflate(R.layout.configure_screen_list_item, null);

		Drawable icon = getPaintedContentIcon(iconId, iconColor);
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageDrawable(icon);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);

		TextView tvDesc = view.findViewById(R.id.description);
		if (desc != null) {
			tvDesc.setText(desc);
			AndroidUiHelper.updateVisibility(tvDesc, true);
		}

		setupClickListener(view, listener);
		setupListItemBackground(view);
		return view;
	}

	private void setupListItemBackground(@NonNull View view) {
		View button = view.findViewById(R.id.button_container);
		int color = selectedAppMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(button, background);
	}

	private void setupClickListener(@NonNull View view, @Nullable OnClickListener listener) {
		View button = view.findViewById(R.id.button_container);
		button.setOnClickListener(listener);
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

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		return activity instanceof MapActivity ? ((MapActivity) activity) : null;
	}

	private void updateFragment() {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag(TAG);
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			currentScrollY = scrollView.getScrollY();
			fragmentManager.beginTransaction()
					.detach(fragment)
					.attach(fragment)
					.commitAllowingStateLoss();
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ConfigureScreenFragment fragment = new ConfigureScreenFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
