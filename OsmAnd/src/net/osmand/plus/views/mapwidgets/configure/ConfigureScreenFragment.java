package net.osmand.plus.views.mapwidgets.configure;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;
import static net.osmand.plus.views.mapwidgets.configure.Map3DModeBottomSheet.*;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
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

import net.osmand.StateChangedListener;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.quickaction.QuickActionListFragment;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.quickaction.QuickActionRegistry.QuickActionUpdatesListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.Map3DModeVisibility;
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
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet.ConfirmationDialogListener;
import net.osmand.plus.views.mapwidgets.configure.panel.ConfigureWidgetsFragment;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigureScreenFragment extends BaseOsmAndFragment implements QuickActionUpdatesListener,
		WidgetsRegistryListener, ConfirmationDialogListener, CopyAppModePrefsListener, CompassVisibilityUpdateListener, Map3DModeUpdateListener {

	public static final String TAG = ConfigureScreenFragment.class.getSimpleName();

	private MapWidgetRegistry widgetRegistry;
	private WidgetsSettingsHelper widgetsSettingsHelper;
	private ApplicationMode selectedAppMode;

	private MapActivity mapActivity;

	private AppBarLayout appBar;
	private Toolbar toolbar;
	private HorizontalChipsView modesToggle;
	private LinearLayout widgetsCard;
	private LinearLayout buttonsCard;
	private ViewGroup actionsCardContainer;
	private NestedScrollView scrollView;

	private int currentScrollY;
	private int currentAppBarOffset;

	private StateChangedListener<Boolean> distanceByTapListener;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mapActivity = (MapActivity) requireMyActivity();
		selectedAppMode = settings.getApplicationMode();
		widgetRegistry = mapActivity.getMapLayers().getMapWidgetRegistry();
		widgetsSettingsHelper = new WidgetsSettingsHelper(mapActivity, selectedAppMode);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_configure_screen, container, false);
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
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
		settings.SHOW_DISTANCE_RULER.addListener(getDistanceByTapListener());
	}

	@Override
	public void onPause() {
		super.onPause();
		settings.SHOW_DISTANCE_RULER.removeListener(getDistanceByTapListener());
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
		appBar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> currentAppBarOffset = verticalOffset);
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
			item.contentDescription = mode.toHumanString();
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

		widgetsCard.addView(new ButtonBuilder()
				.setTitle(getString(R.string.map_widget_transparent))
				.setIconId(R.drawable.ic_action_appearance)
				.setEnabled(settings.TRANSPARENT_MAP_THEME.getModeValue(selectedAppMode))
				.showSwitch(true, view -> {
					boolean enabled = settings.TRANSPARENT_MAP_THEME.get();
					settings.TRANSPARENT_MAP_THEME.setModeValue(selectedAppMode, !enabled);
					mapActivity.updateApplicationModeSettings();
				})
				.createButton());
	}

	public void setupButtonsCard() {
		buttonsCard.removeAllViews();

		CompassVisibility compassVisibility = settings.COMPASS_VISIBILITY.getModeValue(selectedAppMode);
		buttonsCard.addView(new ButtonBuilder()
				.setTitle(getString(R.string.map_widget_compass))
				.setDescription(compassVisibility.getTitle(app))
				.setIconId(compassVisibility.iconId)
				.setEnabled(true)
				.showShortDivider(true)
				.setClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager fragmentManager = activity.getSupportFragmentManager();
						CompassVisibilityBottomSheetDialogFragment.showInstance(fragmentManager, this, selectedAppMode);
					}
				})
				.createButton());

		if (app.useOpenGlRenderer()) {
			Map3DModeVisibility map3DModeVisibility = settings.MAP_3D_MODE_VISIBILITY.getModeValue(selectedAppMode);
			buttonsCard.addView(new ButtonBuilder()
					.setTitle(getString(R.string.map_3d_mode_action))
					.setDescription(map3DModeVisibility.getTitle(app))
					.setIconId(map3DModeVisibility.iconId)
					.setEnabled(true)
					.showShortDivider(true)
					.setClickListener(v -> {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							FragmentManager fragmentManager = activity.getSupportFragmentManager();
							Map3DModeBottomSheet.showInstance(fragmentManager, this, selectedAppMode);
						}
					})
					.createButton());
		}

		boolean distanceByTapEnabled = settings.SHOW_DISTANCE_RULER.getModeValue(selectedAppMode);
		buttonsCard.addView(new ButtonBuilder()
				.setTitle(getString(R.string.map_widget_distance_by_tap))
				.setIconId(R.drawable.ic_action_ruler_line)
				.setEnabled(distanceByTapEnabled)
				.showState(true, getString(distanceByTapEnabled ? R.string.shared_string_on : R.string.shared_string_off))
				.showShortDivider(true)
				.setClickListener(v -> DistanceByTapFragment.showInstance(requireActivity())).createButton());

		QuickActionRegistry registry = app.getQuickActionRegistry();
		int actionsCount = registry.getQuickActions().size();
		String actions = getString(R.string.shared_string_actions);
		String desc = getString(R.string.ltr_or_rtl_combine_via_colon, actions, String.valueOf(actionsCount));
		buttonsCard.addView(new ButtonBuilder()
				.setTitle(getString(R.string.configure_screen_quick_action))
				.setDescription(desc)
				.setIconId(R.drawable.ic_quick_action)
				.setEnabled(settings.QUICK_ACTION.getModeValue(selectedAppMode))
				.setClickListener(v -> QuickActionListFragment.showInstance(requireActivity(), false))
				.createButton());
	}

	private void setupActionsCard() {
		ConfigureScreenActionsCard card = new ConfigureScreenActionsCard(mapActivity, this,
				selectedAppMode, R.string.map_widget_config);
		actionsCardContainer.addView(card.build(actionsCardContainer.getContext()));
	}

	@Override
	public void onActionConfirmed(int actionId) {
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
		updateWidgetsCountForPanel(widgetInfo.getWidgetPanel());
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
		int filter = ENABLED_MODE | AVAILABLE_MODE | MATCHING_PANELS_MODE;
		return widgetRegistry.getWidgetsForPanel(mapActivity, selectedAppMode, filter, Collections.singletonList(panel)).size();
	}

	private void updateWidgetsCount(@NonNull View container, int count) {
		TextView countContainer = container.findViewById(R.id.items_count_descr);
		countContainer.setText(String.valueOf(count));
		AndroidUiHelper.updateVisibility(countContainer, true);
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

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		return activity instanceof MapActivity ? ((MapActivity) activity) : null;
	}

	private void updateFragment() {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		Fragment fragment = manager.findFragmentByTag(TAG);
		if (fragment != null && AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			currentScrollY = scrollView.getScrollY();
			manager.beginTransaction().detach(fragment).commitAllowingStateLoss();
			manager.beginTransaction().attach(fragment).commitAllowingStateLoss();
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

	@NonNull
	private StateChangedListener<Boolean> getDistanceByTapListener() {
		if (distanceByTapListener == null) {
			distanceByTapListener = change -> app.runInUIThread(() -> setupButtonsCard());
		}
		return distanceByTapListener;
	}

	@Override
	public void onMap3DModeUpdated() {
		setupButtonsCard();
	}

	private class ButtonBuilder {
		private int iconId;
		private String title;
		private String desc;
		private String stateText;
		private boolean enabled = false;
		private boolean showShortDivider = false;
		private OnClickListener listener;
		private OnClickListener onSwitchClickListener;
		private boolean showState = false;
		private boolean showSwitch = false;

		public ButtonBuilder setTitle(@NonNull String title) {
			this.title = title;
			return this;
		}

		public ButtonBuilder setIconId(@DrawableRes int iconId) {
			this.iconId = iconId;
			return this;
		}

		public ButtonBuilder setDescription(@Nullable String desc) {
			this.desc = desc;
			return this;
		}

		public ButtonBuilder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public ButtonBuilder setClickListener(@Nullable OnClickListener listener) {
			this.listener = listener;
			return this;
		}

		public ButtonBuilder showShortDivider(boolean showShortDivider) {
			this.showShortDivider = showShortDivider;
			return this;
		}

		public ButtonBuilder showState(boolean showState, @NonNull String stateText) {
			this.showState = showState;
			this.stateText = stateText;
			return this;
		}

		public ButtonBuilder showSwitch(boolean showSwitch, @NonNull OnClickListener onSwitchClickListener) {
			this.showSwitch = showSwitch;
			this.onSwitchClickListener = onSwitchClickListener;
			return this;
		}

		public View createButton() {
			View view = themedInflater.inflate(R.layout.configure_screen_list_item, null);
			TextView tvTitle = view.findViewById(R.id.title);
			ImageView ivIcon = view.findViewById(R.id.icon);
			TextView tvDesc = view.findViewById(R.id.description);
			View shortDivider = view.findViewById(R.id.short_divider);
			CompoundButton cb = view.findViewById(R.id.compound_button);
			TextView stateContainer = view.findViewById(R.id.items_count_descr);

			int activeColor = selectedAppMode.getProfileColor(nightMode);
			int defColor = ColorUtilities.getDefaultIconColor(app, nightMode);
			int iconColor = enabled ? activeColor : defColor;
			tvTitle.setText(title);
			Drawable icon = getPaintedContentIcon(iconId, iconColor);
			ivIcon.setImageDrawable(icon);

			if (desc != null) {
				tvDesc.setText(desc);
				AndroidUiHelper.updateVisibility(tvDesc, true);
			}
			if (showState) {
				stateContainer.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_sub_text_size));
				stateContainer.setText(stateText);
				AndroidUiHelper.updateVisibility(stateContainer, true);
			}
			if (showSwitch) {
				cb.setChecked(enabled);
				AndroidUiHelper.updateVisibility(cb, true);
				UiUtilities.setupCompoundButton(nightMode, activeColor, cb);

				cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
					ivIcon.setColorFilter(isChecked ? activeColor : defColor);
					if (onSwitchClickListener != null) {
						onSwitchClickListener.onClick(buttonView);
					}
				});
			}
			if (listener == null && showSwitch) {
				setupClickListener(view, v -> {
					boolean newState = !cb.isChecked();
					cb.setChecked(newState);
				});
			} else {
				setupClickListener(view, listener);
			}

			AndroidUiHelper.updateVisibility(shortDivider, showShortDivider);
			setupListItemBackground(view);
			return view;
		}
	}
}
