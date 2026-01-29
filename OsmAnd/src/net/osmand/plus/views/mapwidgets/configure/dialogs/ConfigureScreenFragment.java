package net.osmand.plus.views.mapwidgets.configure.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.google.android.material.tabs.TabLayout.Tab;

import net.osmand.StateChangedListener;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectAppModesBottomSheetDialogFragment;
import net.osmand.plus.profiles.SelectAppModesBottomSheetDialogFragment.AppModeChangedListener;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.quickaction.MapButtonsHelper.QuickActionUpdatesListener;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet.ConfirmationDialogListener;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.configure.WidgetsSettingsHelper;
import net.osmand.plus.views.mapwidgets.configure.buttons.CustomMapButtonsFragment;
import net.osmand.plus.views.mapwidgets.configure.buttons.DefaultMapButtonsFragment;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureActionsCard;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureButtonsCard;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureOtherCard;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureWidgetsCard;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.MapScreenLayoutCard;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.ArrayList;
import java.util.List;

public class ConfigureScreenFragment extends BaseFullScreenFragment implements QuickActionUpdatesListener,
		ConfirmationDialogListener, CopyAppModePrefsListener, AppModeChangedListener, CardListener {

	public static final String TAG = ConfigureScreenFragment.class.getSimpleName();

	public static final String SCREEN_LAYOUT_MODE = "screen_layout_mode";

	private MapActivity mapActivity;
	private MapWidgetRegistry widgetRegistry;
	private WidgetsSettingsHelper widgetsSettingsHelper;

	private final List<BaseCard> cards = new ArrayList<>();
	private final ScreenLayoutMode[] layoutMode = new ScreenLayoutMode[1];
	private TabLayout tabLayout;
	private ScrollView scrollView;

	private int currentScrollY;

	private StateChangedListener<Integer> displayPositionListener;
	private StateChangedListener<Boolean> distanceByTapListener;
	private StateChangedListener<Boolean> speedometerListener;
	private StateChangedListener<Boolean> separateLayoutsListener;

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.replace(InsetTarget.createCollapsingAppBar(R.id.appbar));
		return collection;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mapActivity = (MapActivity) requireMyActivity();
		widgetRegistry = mapActivity.getMapLayers().getMapWidgetRegistry();
		widgetsSettingsHelper = new WidgetsSettingsHelper(mapActivity, appMode);

		if (savedInstanceState != null) {
			setLayoutMode(AndroidUtils.getSerializable(savedInstanceState, SCREEN_LAYOUT_MODE, ScreenLayoutMode.class));
		} else {
			setLayoutMode(ScreenLayoutMode.getDefault(mapActivity));
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_configure_screen, container, false);
		if (!InsetsUtils.isEdgeToEdgeSupported()) {
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		}
		setupToolbar(view);
		setupTabLayout(view);
		setupCards(view);

		scrollView = view.findViewById(R.id.scroll_view);
		if (currentScrollY > 0) {
			scrollView.scrollTo(0, currentScrollY);
		}

		return view;
	}

	private void setLayoutMode(@Nullable ScreenLayoutMode layoutMode) {
		this.layoutMode[0] = layoutMode;
		widgetsSettingsHelper.setLayoutMode(layoutMode);
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		ImageButton backButton = toolbar.findViewById(R.id.back_button);
		backButton.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(app)));
		backButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});

		ImageButton profilesButton = toolbar.findViewById(R.id.profiles_button);
		profilesButton.setImageDrawable(getPaintedIcon(appMode.getIconRes(), appMode.getProfileColor(nightMode)));
		profilesButton.setOnClickListener(v -> {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				SelectAppModesBottomSheetDialogFragment.showInstance(manager,
						ConfigureScreenFragment.this, isUsedOnMap(), appMode, true);
			}
		});
		ImageButton actionsButton = toolbar.findViewById(R.id.actions_button);
		actionsButton.setOnClickListener(v -> openActionMenu(actionsButton));
	}

	private void openActionMenu(@NonNull ImageButton actionButton) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitle(getString(R.string.map_screen_layout))
				.setIcon(getContentIcon(R.drawable.ic_action_map_screen_layout_portrait))
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						MapScreenLayoutFragment.showInstance(activity);
					}
				}).create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitle(getString(R.string.copy_from_other_profile))
				.setIcon(getContentIcon(R.drawable.ic_action_copy))
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager manager = activity.getSupportFragmentManager();
						SelectCopyAppModeBottomSheet.showInstance(manager, this, appMode);
					}
				}).create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitle(getString(R.string.shared_string_help))
				.setIcon(getContentIcon(R.drawable.ic_action_help))
				.showTopDivider(true)
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						AndroidUtils.openUrl(activity, R.string.docs_widget_configure_screen, nightMode);
					}
				}).create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitle(getString(R.string.reset_to_default))
				.setIcon(getContentIcon(R.drawable.ic_action_reset))
				.showTopDivider(true)
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager manager = activity.getSupportFragmentManager();
						ConfirmationBottomSheet.showResetSettingsDialog(manager, this, R.string.map_widget_config);
					}
				}).create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = actionButton;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void setupCards(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.cards_container);
		container.removeAllViews();
		cards.clear();

		if (!settings.MAP_SCREEN_LAYOUT_CARD_DISMISSED.get() && !settings.USE_SEPARATE_LAYOUTS.get()) {
			inflate(R.layout.list_item_divider, container);
			addCard(container, new MapScreenLayoutCard(mapActivity));
		}

		inflate(R.layout.list_item_divider, container);
		addCard(container, new ConfigureWidgetsCard(mapActivity, layoutMode));
		inflate(R.layout.list_item_divider, container);

		addCard(container, new ConfigureButtonsCard(mapActivity));
		inflate(R.layout.list_item_divider, container);

		addCard(container, new ConfigureOtherCard(mapActivity));
		inflate(R.layout.list_item_divider, container);

		addCard(container, new ConfigureActionsCard(mapActivity));
		inflate(R.layout.card_bottom_divider, container);
	}

	private void addCard(@NonNull ViewGroup container, @NonNull BaseCard card) {
		cards.add(card);
		card.setListener(this);
		container.addView(card.build());
	}

	private void updateCards() {
		for (BaseCard card : cards) {
			card.update();
		}
	}

	private void setupTabLayout(@NonNull View view) {
		tabLayout = view.findViewById(R.id.layout_tab_layout);
		tabLayout.removeAllTabs();

		for (ScreenLayoutMode mode : ScreenLayoutMode.values()) {
			Tab tab = tabLayout.newTab();
			tab.setText(mode.toHumanString(app));
			tab.setTag(mode);
			tabLayout.addTab(tab);
		}
		tabLayout.addOnTabSelectedListener(new OnTabSelectedListener() {
			@Override
			public void onTabSelected(Tab tab) {
				if (tab.getTag() instanceof ScreenLayoutMode mode) {
					setLayoutMode(mode);
					updateCards();
				}
			}

			@Override
			public void onTabUnselected(Tab tab) {
			}

			@Override
			public void onTabReselected(Tab tab) {
			}
		});
		updateTabs();
	}

	private void updateTabs() {
		if (tabLayout == null) {
			return;
		}
		boolean separate = settings.USE_SEPARATE_LAYOUTS.get();
		AndroidUiHelper.updateVisibility(tabLayout, separate);

		if (separate && layoutMode[0] != null) {
			for (int i = 0; i < tabLayout.getTabCount(); i++) {
				Tab tab = tabLayout.getTabAt(i);
				if (tab != null && tab.getTag() == layoutMode[0] && !tab.isSelected()) {
					tab.select();
					break;
				}
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		updateTabs();
		updateCards();

		settings.SHOW_DISTANCE_RULER.addListener(getDistanceByTapListener());
		settings.POSITION_PLACEMENT_ON_MAP.addListener(getDisplayPositionListener());
		settings.SHOW_SPEEDOMETER.addListener(getSpeedometerListener());
		settings.USE_SEPARATE_LAYOUTS.addListener(getSeparateLayoutsListener());

		callMapActivity(MapActivity::disableDrawer);
	}

	@Override
	public void onPause() {
		super.onPause();
		settings.SHOW_DISTANCE_RULER.removeListener(getDistanceByTapListener());
		settings.POSITION_PLACEMENT_ON_MAP.removeListener(getDisplayPositionListener());
		settings.SHOW_SPEEDOMETER.removeListener(getSpeedometerListener());
		settings.USE_SEPARATE_LAYOUTS.removeListener(getSeparateLayoutsListener());

		callMapActivity(MapActivity::enableDrawer);
	}

	@Override
	public void onStart() {
		super.onStart();
		app.getMapButtonsHelper().addUpdatesListener(this);
		mapActivity.disableDrawer();
	}

	@Override
	public void onStop() {
		super.onStop();
		app.getMapButtonsHelper().removeUpdatesListener(this);
		mapActivity.enableDrawer();
	}


	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (layoutMode[0] != null) {
			outState.putSerializable(SCREEN_LAYOUT_MODE, layoutMode);
		}
	}

	@Override
	public void onActionConfirmed(int actionId) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		widgetsSettingsHelper.setAppMode(appMode);
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
		widgetsSettingsHelper.setAppMode(appMode);
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
	public void onAppModeChanged(ApplicationMode appMode) {
		setAppMode(appMode);
		updateFragment();
	}

	@Override
	public void onActionsUpdated() {
		updateCard(ConfigureButtonsCard.class);
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof MapScreenLayoutCard) {
			updateFragment();
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		if (card instanceof ConfigureActionsCard) {
			if (buttonIndex == ConfigureActionsCard.COPY_BUTTON_INDEX) {
				SelectCopyAppModeBottomSheet.showInstance(manager, ConfigureScreenFragment.this, appMode);
			} else if (buttonIndex == ConfigureActionsCard.RESET_BUTTON_INDEX) {
				ConfirmationBottomSheet.showResetSettingsDialog(manager, ConfigureScreenFragment.this, R.string.map_widget_config);
			}
		} else if (card instanceof ConfigureButtonsCard) {
			if (buttonIndex == ConfigureButtonsCard.CUSTOM_MAP_BUTTONS_INDEX) {
				CustomMapButtonsFragment.showInstance(manager, ConfigureScreenFragment.this);
			} else if (buttonIndex == ConfigureButtonsCard.DEFAULT_MAP_BUTTONS_INDEX) {
				DefaultMapButtonsFragment.showInstance(manager, ConfigureScreenFragment.this);
			}
		}
	}

	@Nullable
	private <T extends BaseCard> T getCard(Class<T> clazz) {
		for (BaseCard card : cards) {
			if (clazz.isInstance(card)) {
				return clazz.cast(card);
			}
		}
		return null;
	}


	private <T extends BaseCard> void updateCard(Class<T> clazz) {
		BaseCard card = getCard(clazz);
		if (card != null) {
			card.update();
		}
	}

	@NonNull
	private StateChangedListener<Integer> getDisplayPositionListener() {
		if (displayPositionListener == null) {
			displayPositionListener = change -> app.runInUIThread(() -> updateCard(ConfigureOtherCard.class));
		}
		return displayPositionListener;
	}

	@NonNull
	private StateChangedListener<Boolean> getDistanceByTapListener() {
		if (distanceByTapListener == null) {
			distanceByTapListener = change -> app.runInUIThread(() -> updateCard(ConfigureOtherCard.class));
		}
		return distanceByTapListener;
	}

	@NonNull
	private StateChangedListener<Boolean> getSpeedometerListener() {
		if (speedometerListener == null) {
			speedometerListener = change -> app.runInUIThread(() -> updateCard(ConfigureOtherCard.class));
		}
		return speedometerListener;
	}

	@NonNull
	private StateChangedListener<Boolean> getSeparateLayoutsListener() {
		if (separateLayoutsListener == null) {
			separateLayoutsListener = change -> app.runInUIThread(() -> {
				setLayoutMode(change ? ScreenLayoutMode.getDefault(mapActivity) : null);
				updateTabs();
				updateCards();
			});
		}
		return separateLayoutsListener;
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
}