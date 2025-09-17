package net.osmand.plus.views.mapwidgets.configure.panel;

import static net.osmand.plus.helpers.AndroidUiHelper.ANIMATION_DURATION;
import static net.osmand.plus.settings.bottomsheets.WidgetsResetConfirmationBottomSheet.showResetSettingsDialog;
import static net.osmand.plus.utils.WidgetUtils.createNewWidget;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.google.android.material.tabs.TabLayout.Tab;
import com.google.android.material.tabs.TabLayoutMediator;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet.ConfirmationDialogListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.WidgetsSettingsHelper;
import net.osmand.plus.views.mapwidgets.configure.dialogs.AddWidgetFragment.AddWidgetListener;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigureWidgetsFragment extends BaseFullScreenFragment implements WidgetsConfigurationChangeListener,
		InAppPurchaseListener, AddWidgetListener, CopyAppModePrefsListener, ConfirmationDialogListener {

	public static final String TAG = ConfigureWidgetsFragment.class.getSimpleName();

	private static final String APP_MODE_ATTR = "app_mode_key";
	private static final String SELECTED_GROUP_ATTR = "selected_group_key";
	private static final String CONTEXT_SELECTED_WIDGET = "context_widget_page";
	private static final String CONTEXT_SELECTED_PANEL = "context_selected_panel";
	private static final String ADD_TO_NEXT = "widget_order";
	private static final String EDIT_MODE_KEY = "edit_mode_key";

	private DialogManager dialogManager;
	private ConfigureWidgetsController controller;

	private WidgetsPanel selectedPanel;
	private ApplicationMode selectedAppMode;
	private OnBackPressedCallback onBackPressedCallback;
	private FragmentLifecycleCallbacks lifecycleCallbacks;

	private Toolbar toolbar;
	private AppBarLayout appBar;
	private CollapsingToolbarLayout collapsingToolbarLayout;
	private TabLayout tabLayout;
	private ViewPager2 viewPager;
	private View bottomButtons;
	private View shadowView;
	private View bottomButtonsShadow;
	private FloatingActionButton fabNewWidget;
	private TextView toolbarTitleView;
	private View view;

	public boolean isEditMode = false;

	@NonNull
	public WidgetsPanel getSelectedPanel() {
		return selectedPanel;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dialogManager = app.getDialogManager();
		setupController();
		if (savedInstanceState != null) {
			String appModeKey = savedInstanceState.getString(APP_MODE_ATTR);
			selectedAppMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
			selectedPanel = WidgetsPanel.valueOf(savedInstanceState.getString(SELECTED_GROUP_ATTR));
			isEditMode = savedInstanceState.getBoolean(EDIT_MODE_KEY, false);
		}

		Bundle args = getArguments();
		if (args != null && (args.containsKey(CONTEXT_SELECTED_WIDGET)
				|| args.containsKey(ADD_TO_NEXT))) {
			addNewWidget();
		}
		onBackPressedCallback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				closeFragment();
			}
		};
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		view = inflate(R.layout.fragment_configure_widgets, container, false);
		if (Build.VERSION.SDK_INT < 30) {
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
			view.setFitsSystemWindows(true);
		}
		appBar = view.findViewById(R.id.appbar);

		toolbar = view.findViewById(R.id.toolbar);
		tabLayout = view.findViewById(R.id.tab_layout);
		viewPager = view.findViewById(R.id.view_pager);
		collapsingToolbarLayout = view.findViewById(R.id.toolbar_layout);
		fabNewWidget = view.findViewById(R.id.fab);
		bottomButtonsShadow = view.findViewById(R.id.buttons_shadow);
		toolbarTitleView = toolbar.findViewById(R.id.toolbar_title);
		bottomButtons = view.findViewById(R.id.bottom_buttons_container);
		shadowView = view.findViewById(R.id.shadow_view);

		bottomButtons.setVisibility(View.GONE);
		bottomButtonsShadow.setVisibility(View.GONE);

		lifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
			@Override
			public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
				super.onFragmentDestroyed(fm, f);
				Fragment currentFragment = fm.findFragmentById(R.id.fragmentContainer);
				if (currentFragment instanceof ConfigureWidgetsFragment) {
					onBackPressedCallback.setEnabled(true);
				}
			}

			@Override
			public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
				super.onFragmentResumed(fm, f);
				onBackPressedCallback.setEnabled(false);
			}
		};
		getParentFragmentManager().registerFragmentLifecycleCallbacks(lifecycleCallbacks, false);

		setupToolbar();
		setupTabLayout();
		setupApplyButton();
		updateScreen();

		return view;
	}

	@Nullable
	@Override
	public List<Integer> getCollapsingAppBarLayoutId() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.appbar);
		return ids;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
	}

	private void closeFragment() {
		if (isEditMode) {
			toggleEditMode(false);

			WidgetsListFragment fragment = getSelectedFragment();
			if (fragment != null) {
				fragment.updateEditMode();
			}
		} else {
			requireActivity().getSupportFragmentManager().popBackStack();
		}
	}

	private void setupToolbar() {
		ImageButton backButton = toolbar.findViewById(R.id.back_button);
		backButton.setOnClickListener(view -> {
			closeFragment();
		});

		AppCompatImageButton infoButton = toolbar.findViewById(R.id.info_button);
		infoButton.setOnClickListener(v -> {
			WidgetsListFragment fragment = getSelectedFragment();
			if (!isEditMode) {
				toggleEditMode(true);
				if (fragment != null) {
					fragment.updateEditMode();
				}
			} else if (fragment != null) {
				fragment.resetToOriginal();
			}
		});

		AppCompatImageButton actionButton = view.findViewById(R.id.actions_button);
		actionButton.setOnClickListener(view -> {
			if (isEditMode) {
				copyFromProfile();
			} else {
				openActionMenu(actionButton);
			}
		});

		fabNewWidget.setOnClickListener(view -> {
			addNewWidget();
		});
	}

	private void openActionMenu(@NonNull AppCompatImageButton actionButton) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitle(getString(R.string.copy_from_other_profile))
				.setIcon(getContentIcon(R.drawable.ic_action_copy))
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager fragmentManager = activity.getSupportFragmentManager();
						ApplicationMode appMode = settings.getApplicationMode();
						SelectCopyAppModeBottomSheet.showInstance(fragmentManager, ConfigureWidgetsFragment.this, appMode);
					}
				}).create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitle(getString(R.string.reset_to_default))
				.setIcon(getContentIcon(R.drawable.ic_action_reset))
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager fragmentManager = activity.getSupportFragmentManager();
						int panelTitleId = selectedPanel.getTitleId(AndroidUtils.isLayoutRtl(app));
						showResetSettingsDialog(fragmentManager, ConfigureWidgetsFragment.this, panelTitleId);
					}
				}).create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitle(getString(R.string.shared_string_help))
				.setIcon(getContentIcon(R.drawable.ic_action_help))
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						AndroidUtils.openUrl(activity, R.string.docs_widgets, nightMode);
					}
				}).create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = actionButton;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void setupApplyButton() {
		bottomButtons.setBackgroundColor(ColorUtilities.getListBgColor(requireMapActivity(), nightMode));

		DialogButton applyButton = view.findViewById(R.id.dismiss_button);
		applyButton.setButtonType(DialogButtonType.PRIMARY);
		applyButton.setTitleId(R.string.shared_string_apply);
		applyButton.setOnClickListener(view -> {
			WidgetsListFragment fragment = getSelectedFragment();
			if (isEditMode && fragment != null) {
				fragment.onApplyChanges();
				toggleEditMode(false);
				fragment.reloadWidgets();
			}
		});
		AndroidUiHelper.updateVisibility(applyButton, true);
	}

	private void setupController() {
		controller = (ConfigureWidgetsController) dialogManager.findController(ConfigureWidgetsController.PROCESS_ID);
		if (controller == null) {
			dialogManager.register(ConfigureWidgetsController.PROCESS_ID, new ConfigureWidgetsController());
			controller = (ConfigureWidgetsController) dialogManager.findController(ConfigureWidgetsController.PROCESS_ID);
		}
	}

	private void updateToolbar() {
		ImageButton backButton = toolbar.findViewById(R.id.back_button);
		backButton.setImageDrawable(getContentIcon(isEditMode ? R.drawable.ic_action_close : AndroidUtils.getNavigationIconResId(app)));
		backButton.setContentDescription(app.getString(isEditMode ? R.string.shared_string_close : R.string.access_shared_string_navigate_up));
		AppCompatImageButton infoButton = toolbar.findViewById(R.id.info_button);
		infoButton.setImageDrawable(getContentIcon(isEditMode ? R.drawable.ic_action_reset : R.drawable.ic_action_edit_outlined));

		AppCompatImageButton actionButton = view.findViewById(R.id.actions_button);
		actionButton.setImageDrawable(getContentIcon(isEditMode ? R.drawable.ic_action_copy : R.drawable.ic_overflow_menu_white));
		actionButton.setContentDescription(getString(isEditMode ? R.string.copy_from_other_profile : R.string.shared_string_menu));

		updateToolbarName();
	}

	private void updateToolbarName() {
		boolean rtl = AndroidUtils.isLayoutRtl(app);
		toolbarTitleView.setText(selectedPanel.getTitleId(rtl));
	}

	private void copyFromProfile() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SelectCopyAppModeBottomSheet.showInstance(activity.getSupportFragmentManager(), this, selectedAppMode);
		}
	}

	public void addNewWidget() {
		SearchWidgetsFragment.showInstance(requireMapActivity(), selectedPanel, ConfigureWidgetsFragment.this);
	}

	private void updateScreen() {
		AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) collapsingToolbarLayout.getLayoutParams();
		params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL);
		if (isEditMode) {
			bottomButtons.setVisibility(View.VISIBLE);
			bottomButtonsShadow.setVisibility(View.VISIBLE);

			params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL);
			appBar.setExpanded(true, true);

			tabLayout.setClickable(false);
			tabLayout.setFocusable(false);
		} else {
			tabLayout.setVisibility(View.VISIBLE);

			params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
			tabLayout.setClickable(true);
			tabLayout.setFocusable(true);
		}

		updateFabPosition(isEditMode);
		viewPager.setUserInputEnabled(!isEditMode);
		updateToolbar();
	}

	private void animateOnUpdateEditMode() {
		if (isEditMode) {
			if (isDisableAnimations()) {
				tabLayout.setVisibility(View.GONE);
				shadowView.setVisibility(View.GONE);
			} else {
				bottomButtons.setTranslationY(bottomButtons.getHeight());
				bottomButtonsShadow.setTranslationY(bottomButtons.getHeight() - bottomButtonsShadow.getHeight());
				appBar.setElevation(0f);

				animateView(tabLayout, -tabLayout.getHeight(), false, () -> tabLayout.setVisibility(View.INVISIBLE));
				animateView(viewPager, -tabLayout.getHeight(), null, null);
				animateView(shadowView, -tabLayout.getHeight(), null, null);
				animateView(bottomButtons, 0, true, () -> bottomButtons.setVisibility(View.VISIBLE));
				animateView(bottomButtonsShadow, 0, true, () -> bottomButtonsShadow.setVisibility(View.VISIBLE));
			}
		} else {
			if (isDisableAnimations()) {
				shadowView.setVisibility(View.VISIBLE);
				bottomButtons.setVisibility(View.GONE);
				bottomButtonsShadow.setVisibility(View.GONE);
			} else {
				animateView(tabLayout, 0, true, () -> appBar.setElevation(view.getResources().getDimension(R.dimen.abp__shadow_height)));
				animateView(viewPager, 0, null, null);
				animateView(shadowView, 0, null, null);
				animateView(bottomButtons, bottomButtons.getHeight(), false, () -> bottomButtons.setVisibility(View.INVISIBLE));
				animateView(bottomButtonsShadow, bottomButtons.getHeight() - bottomButtonsShadow.getHeight(), false, () -> bottomButtonsShadow.setVisibility(View.INVISIBLE));
			}
		}
	}

	private void toggleEditMode(boolean editMode) {
		isEditMode = editMode;
		updateScreen();
		animateOnUpdateEditMode();
	}

	private void animateView(View view, int translationY, @Nullable Boolean show, @Nullable Runnable endAction) {
		ViewPropertyAnimator propertyAnimator = view.animate();
		propertyAnimator.translationY(translationY)
				.setDuration(ANIMATION_DURATION);

		if (show != null) {
			propertyAnimator.alpha(show ? 1f : 0f);
		}
		if (endAction != null) {
			propertyAnimator.withEndAction(endAction);
		}
		propertyAnimator.start();
	}

	private void updateFabPosition(boolean isEditing) {
		int translationY = isEditing ? -dpToPx(60) : 0;
		if (isDisableAnimations()) {
			fabNewWidget.setTranslationY(translationY);
		} else {
			fabNewWidget.animate()
					.translationY(translationY)
					.setDuration(ANIMATION_DURATION)
					.setInterpolator(new DecelerateInterpolator())
					.start();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(APP_MODE_ATTR, selectedAppMode.getStringKey());
		outState.putString(SELECTED_GROUP_ATTR, selectedPanel.name());
		outState.putBoolean(EDIT_MODE_KEY, isEditMode);
	}

	private void setupTabLayout() {
		WidgetsTabAdapter widgetsTabAdapter = new WidgetsTabAdapter(this);
		viewPager.setAdapter(widgetsTabAdapter);
		viewPager.registerOnPageChangeCallback(new OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				selectedPanel = WidgetsPanel.values()[position];
				updateToolbarName();
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
	public void onWidgetsConfigurationChanged() {
		WidgetsListFragment fragment = getSelectedFragment();
		if (fragment != null && !isEditMode) {
			fragment.reloadWidgets();
		}
	}

	@Override
	public void onWidgetAdded(@NonNull MapWidgetInfo widgetInfo) {
		WidgetsListFragment fragment = getSelectedFragment();
		if (isEditMode && fragment != null) {
			fragment.addWidget(widgetInfo);
		} else {
			createWidgets(Collections.singletonList(widgetInfo));
		}
	}

	@Override
	public void onWidgetSelectedToAdd(@NonNull String widgetsId, @NonNull WidgetsPanel panel, boolean recreateControls) {
		controller.openAddNewWidgetScreen(requireMapActivity(), selectedPanel, widgetsId, selectedAppMode, this);
	}

	public void createWidgets(@NonNull List<MapWidgetInfo> newWidgetInfos) {
		for (MapWidgetInfo widgetInfo : newWidgetInfos) {
			Bundle args = getArguments();
			if (args != null) {
				String selectedWidget = args.getString(CONTEXT_SELECTED_WIDGET);
				boolean addToNext = args.getBoolean(ADD_TO_NEXT);
				if (selectedWidget != null) {
					createNewWidget(requireMapActivity(), widgetInfo, selectedPanel, selectedAppMode, true, selectedWidget, addToNext);
					onWidgetsConfigurationChanged();
					return;
				}
			}
			createNewWidget(requireMapActivity(), widgetInfo, selectedPanel, selectedAppMode, true);
			onWidgetsConfigurationChanged();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		setupController();
		updateStatusBar();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			dialogManager.unregister(ConfigureWidgetsController.PROCESS_ID);
		}
		getParentFragmentManager().unregisterFragmentLifecycleCallbacks(lifecycleCallbacks);
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		onWidgetsConfigurationChanged();
	}

	private boolean isDisableAnimations() {
		return app.getSettings().DO_NOT_USE_ANIMATIONS.getModeValue(selectedAppMode);
	}

	@Nullable
	private WidgetsListFragment getSelectedFragment() {
		FragmentManager manager = getChildFragmentManager();
		for (Fragment fragment : manager.getFragments()) {
			if (fragment instanceof WidgetsListFragment widgetsFragment
					&& Algorithms.objectEquals(widgetsFragment.getSelectedPanel(), selectedPanel)) {
				return widgetsFragment;
			}
		}
		return null;
	}

	@Override
	public void onActionConfirmed(int actionId) {
		WidgetsSettingsHelper helper = new WidgetsSettingsHelper(requireMapActivity(), selectedAppMode);
		helper.resetWidgetsForPanel(selectedPanel);

		WidgetsListFragment fragment = getSelectedFragment();
		if (fragment != null) {
			fragment.reloadWidgets();
		}
		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateAllControls(requireMapActivity());
		}
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		WidgetsListFragment fragment = getSelectedFragment();
		if (isEditMode && fragment != null) {
			fragment.copyAppModePrefs(appMode);
		} else {
			WidgetsSettingsHelper helper = new WidgetsSettingsHelper(requireMapActivity(), selectedAppMode);

			helper.copyWidgetsForPanel(appMode, selectedPanel);

			MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
			if (settings.getApplicationMode().equals(selectedAppMode) && mapInfoLayer != null) {
				mapInfoLayer.recreateAllControls(requireMapActivity());
			}
			if (fragment != null) {
				fragment.reloadWidgets();
			}
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode, @Nullable Bundle args) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ConfigureWidgetsFragment fragment = new ConfigureWidgetsFragment();
			fragment.selectedPanel = panel;
			fragment.selectedAppMode = appMode;
			if (args != null) {
				fragment.setArguments(args);
			}
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode, @NonNull String selectedWidget, boolean addNext) {
		Bundle args = new Bundle();
		args.putString(CONTEXT_SELECTED_WIDGET, selectedWidget);
		args.putInt(CONTEXT_SELECTED_PANEL, panel.ordinal());
		args.putBoolean(ADD_TO_NEXT, addNext);

		ConfigureWidgetsFragment.showInstance(activity, panel, appMode, args);
	}
}