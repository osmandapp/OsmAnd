package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.views.mapwidgets.configure.dialogs.Map3DModeBottomSheet.Map3DModeUpdateListener;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

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
import net.osmand.plus.quickaction.QuickActionRegistry.QuickActionUpdatesListener;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet.ConfirmationDialogListener;
import net.osmand.plus.settings.enums.CompassVisibility;
import net.osmand.plus.settings.enums.Map3DModeVisibility;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.WidgetsRegistryListener;
import net.osmand.plus.views.mapwidgets.configure.WidgetsSettingsHelper;
import net.osmand.plus.views.mapwidgets.configure.dialogs.CompassVisibilityBottomSheet.CompassVisibilityUpdateListener;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureActionsCard;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureButtonsCard;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureOtherCard;
import net.osmand.plus.views.mapwidgets.configure.dialogs.cards.ConfigureWidgetsCard;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class ConfigureScreenFragment extends BaseOsmAndFragment implements QuickActionUpdatesListener,
		WidgetsRegistryListener, ConfirmationDialogListener, CopyAppModePrefsListener,
		CompassVisibilityUpdateListener, Map3DModeUpdateListener {

	public static final String TAG = ConfigureScreenFragment.class.getSimpleName();

	private MapWidgetRegistry widgetRegistry;
	private WidgetsSettingsHelper widgetsSettingsHelper;
	private ApplicationMode selectedAppMode;

	private MapActivity mapActivity;

	private AppBarLayout appBar;
	private HorizontalChipsView modesToggle;
	private ViewGroup cardsContainer;
	private NestedScrollView scrollView;

	private ConfigureWidgetsCard widgetsCard;
	private ConfigureButtonsCard buttonsCard;
	private ConfigureOtherCard otherCard;
	private ConfigureActionsCard actionsCard;

	private int currentScrollY;
	private int currentAppBarOffset;

	private StateChangedListener<Integer> displayPositionListener;
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
		modesToggle = view.findViewById(R.id.modes_toggle);
		cardsContainer = view.findViewById(R.id.cards_container);
		scrollView = view.findViewById(R.id.scroll_view);

		setupAppBar();
		setupToolbar(view);
		setupModesToggle();
		setupCards();

		if (currentScrollY > 0) {
			scrollView.scrollTo(0, currentScrollY);
		}

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateCard(widgetsCard);
		settings.POSITION_PLACEMENT_ON_MAP.addListener(getDisplayPositionListener());
		settings.SHOW_DISTANCE_RULER.addListener(getDistanceByTapListener());
	}

	@Override
	public void onPause() {
		super.onPause();
		settings.POSITION_PLACEMENT_ON_MAP.removeListener(getDisplayPositionListener());
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

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		ImageButton backButton = toolbar.findViewById(R.id.back_button);
		backButton.setOnClickListener(v -> {
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

			View cardView = actionsCard != null ? actionsCard.getView() : null;
			if (cardView != null) {
				scrollView.smoothScrollTo(0, (int) cardView.getY());
			}
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

	private void setupCards() {
		cardsContainer.removeAllViews();

		widgetsCard = new ConfigureWidgetsCard(mapActivity);
		cardsContainer.addView(widgetsCard.build(mapActivity));

		buttonsCard = new ConfigureButtonsCard(mapActivity, this);
		cardsContainer.addView(buttonsCard.build(mapActivity));

		otherCard = new ConfigureOtherCard(mapActivity);
		cardsContainer.addView(otherCard.build(mapActivity));

		actionsCard = new ConfigureActionsCard(mapActivity, this, R.string.map_widget_config);
		cardsContainer.addView(actionsCard.build(mapActivity));
	}

	private void updateCard(@Nullable BaseCard card) {
		if (card != null) {
			card.update();
		}
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
		updateCard(buttonsCard);
	}

	@Override
	public void onCompassVisibilityUpdated(@NonNull CompassVisibility visibility) {
		updateCard(buttonsCard);
	}

	@Override
	public void onWidgetRegistered(@NonNull MapWidgetInfo widgetInfo) {
		updateCard(widgetsCard);
	}

	@Override
	public void onWidgetVisibilityChanged(@NonNull MapWidgetInfo widgetInfo) {

	}

	@Override
	public void onWidgetsCleared() {

	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@NonNull
	private StateChangedListener<Integer> getDisplayPositionListener() {
		if (displayPositionListener == null) {
			displayPositionListener = value -> app.runInUIThread(() -> updateCard(otherCard));
		}
		return displayPositionListener;
	}

	@NonNull
	private StateChangedListener<Boolean> getDistanceByTapListener() {
		if (distanceByTapListener == null) {
			distanceByTapListener = change -> app.runInUIThread(() -> updateCard(otherCard));
		}
		return distanceByTapListener;
	}

	@Override
	public void onMap3DModeUpdated(@NonNull Map3DModeVisibility visibility) {
		updateCard(buttonsCard);
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
}
