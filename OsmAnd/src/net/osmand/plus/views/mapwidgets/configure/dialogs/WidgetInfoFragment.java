package net.osmand.plus.views.mapwidgets.configure.dialogs;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.*;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.aidl.ConnectedApp;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetsFactory;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.banner.WidgetPromoBanner;
import net.osmand.plus.views.mapwidgets.banner.WidgetPromoBanner.WidgetData;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsConfigurationChangeListener;
import net.osmand.plus.views.mapwidgets.configure.settings.WidgetSettingsBaseFragment;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WidgetInfoFragment extends BaseWidgetFragment implements WidgetsConfigurationChangeListener {

	public static final String TAG = WidgetInfoFragment.class.getSimpleName();

	private static final String WIDGET_ID_KEY = "widget_id";

	private MapWidgetRegistry widgetRegistry;
	private WidgetIconsHelper iconsHelper;

	private MapWidgetInfo widgetInfo;
	private WidgetType widgetType;
	private WidgetGroup widgetGroup;
	private WidgetsPanel panel;

	private View view;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// Using here, because in onCreate() not all required data is initialized after rotation
		Bundle args = getArguments();
		if (savedInstanceState != null) {
			initFromBundle(savedInstanceState);
		} else if (args != null) {
			initFromBundle(args);
		}

		updateNightMode();
		iconsHelper = new WidgetIconsHelper(app, appMode.getProfileColor(nightMode), nightMode);
		view = themedInflater.inflate(R.layout.base_widget_fragment_layout, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		ViewGroup mainContent = view.findViewById(R.id.main_content);
		themedInflater.inflate(R.layout.widget_info_fragment_content, mainContent);

		setupToolbar();
		setupContent();
		hideBottomButton();

		return view;
	}

	private void initFromBundle(@NonNull Bundle bundle) {
		String appModeKey = bundle.getString(APP_MODE_KEY);
		String widgetId = bundle.getString(WIDGET_ID_KEY);

		appMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
		widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		if (widgetInfo == null) {
			throw new IllegalStateException("Failed to find widget by id: " + widgetId);
		}
		widgetType = widgetInfo.getWidgetType();
		panel = widgetInfo.getWidgetPanel();
		widgetGroup = widgetType == null ? null : widgetType.getGroup(panel);
	}

	private void setupToolbar() {
		View closeButton = view.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> dismiss());

		View helpButton = view.findViewById(R.id.help_button);
		AndroidUiHelper.updateVisibility(helpButton, false);

		TextView tvTitle = view.findViewById(R.id.title);
		TextView tvSubTitle = view.findViewById(R.id.sub_title);
		ImageView ivIcon = view.findViewById(R.id.icon);

		String subTitle;
		if (widgetGroup != null) {
			String widgetStr = getString(R.string.widget_with_dot);
			subTitle = getString(R.string.ltr_or_rtl_combine_via_space, widgetStr, getString(widgetGroup.titleId));
		} else {
			subTitle = getString(R.string.shared_string_widget);
		}

		tvTitle.setText(widgetInfo.getStateIndependentTitle(app));
		tvSubTitle.setText(subTitle);
		setWidgetIcon(ivIcon);
	}

	private void setupContent() {
		setupWidgetDescription();
		setupWidgetItem();
		setupSecondaryDescription();
		setupActions();
	}

	private void setupWidgetDescription() {
		TextView tvDesc = view.findViewById(R.id.desc);
		String externalProviderPackage = widgetInfo.getExternalProviderPackage();
		if (widgetType != null) {
			tvDesc.setText(widgetType.descId);
			return;
		} else if (externalProviderPackage != null) {
			ConnectedApp connectedApp = app.getAidlApi().getConnectedApp(externalProviderPackage);
			if (connectedApp != null) {
				tvDesc.setText(connectedApp.getName());
				return;
			}
		}

		AndroidUiHelper.updateVisibility(tvDesc, false);
	}

	private void setupWidgetItem() {
		if (widgetType != null && !widgetType.isPurchased(app)) {
			setupPromoBanner();
		} else {
			setupSelectableItem();
		}
	}

	private void setupSelectableItem() {
		View container = view.findViewById(R.id.widget_item);
		ImageView ivIcon = container.findViewById(R.id.icon);
		TextView ivName = container.findViewById(R.id.title);
		CompoundButton compoundButton = container.findViewById(R.id.compound_button);

		container.setSelected(true);
		container.setOnClickListener(v -> {}); // Empty listener to have pressed state
		setWidgetIcon(ivIcon);
		container.setVisibility(View.VISIBLE);
		ivName.setText(widgetInfo.getTitle(app));

		View bannerContainer = view.findViewById(R.id.promo_banner_container);
		AndroidUiHelper.setVisibility(View.GONE, compoundButton, bannerContainer);
	}

	private void setupPromoBanner() {
		MapActivity activity = getMapActivity();
		ViewGroup bannerContainer = view.findViewById(R.id.promo_banner_container);
		View widgetContainer = view.findViewById(R.id.widget_item);
		bannerContainer.setVisibility(View.VISIBLE);
		widgetContainer.setVisibility(View.GONE);
		bannerContainer.removeAllViews();

		WidgetData widgetData = new WidgetData(widgetType.titleId, widgetType.dayIconId, widgetType.nightIconId);
		WidgetPromoBanner banner = new WidgetPromoBanner(requireMapActivity(), widgetData, false);
		bannerContainer.addView(banner.build(activity));
	}

	private void setupSecondaryDescription() {
		String desc = widgetType == null ? null : widgetType.getSecondaryDescription(app);
		int iconId = widgetType == null ? 0 : widgetType.getSecondaryIconId();
		if (!Algorithms.isEmpty(desc) && iconId != 0) {
			TextView tvDesc = view.findViewById(R.id.secondary_desc);
			ImageView ivIcon = view.findViewById(R.id.secondary_icon);
			tvDesc.setText(desc);
			ivIcon.setImageDrawable(getIcon(iconId));
		} else {
			View container = view.findViewById(R.id.secondary_desc_container);
			AndroidUiHelper.updateVisibility(container, false);
		}
	}

	private void setupActions() {
		for (Action action : Action.values()) {
			View container = view.findViewById(action.containerId);
			if (action.isAvailable(widgetInfo, app)) {
				View pressableContainer = container.findViewById(R.id.pressable_container);
				ImageView ivIcon = container.findViewById(R.id.icon);
				TextView tvTitle = container.findViewById(R.id.title);
				View bottomDivider = container.findViewById(R.id.bottom_divider);

				int activeColor = ColorUtilities.getActiveColor(app, nightMode);
				pressableContainer.setBackground(UiUtilities.getColoredSelectableDrawable(app, activeColor));

				ivIcon.setImageDrawable(getIcon(action.iconId));
				tvTitle.setText(action.titleId);
				AndroidUiHelper.updateVisibility(bottomDivider, action.showBottomDivider());

				container.setOnClickListener(getActionClickListener(action));
			} else {
				AndroidUiHelper.updateVisibility(container, false);
			}
		}
	}

	@NonNull
	private OnClickListener getActionClickListener(@NonNull Action action) {
		return v -> {
			if (action == Action.SETTINGS) {
				openSettingsFragment();
			} else if (action == Action.DUPLICATE) {
				String duplicateId = createDuplicateWidget();
				if (duplicateId != null) {
					notifyTarget();
					showDuplicateAddedSnackbar(duplicateId);
				}
			} else if (action == Action.REMOVE) {
				widgetRegistry.enableDisableWidgetForMode(appMode, widgetInfo, false, true);
				notifyTarget();
				dismiss();
			} else {
				throw new IllegalStateException("Unsupported action");
			}
		};
	}

	private void openSettingsFragment() {
		WidgetSettingsBaseFragment settingsFragment = widgetType == null ? null : widgetType.getSettingsFragment(app, widgetInfo);
		if (settingsFragment == null) {
			throw new IllegalStateException("Widget has no available settings");
		}

		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			Bundle args = new Bundle();
			args.putString(APP_MODE_KEY, appMode.getStringKey());
			args.putString(WIDGET_ID_KEY, widgetInfo.key);

			WidgetSettingsBaseFragment.showFragment(fragmentManager, args, this, settingsFragment);
		}
	}

	@Nullable
	private String createDuplicateWidget() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return null;
		}

		int filter = ENABLED_MODE | MATCHING_PANELS_MODE;
		List<WidgetsPanel> panels = Collections.singletonList(panel);
		List<MapWidgetInfo> widgetInfos = new ArrayList<>(widgetRegistry.getWidgetsForPanel(mapActivity, appMode, filter, panels));

		int index = widgetInfos.indexOf(widgetInfo);
		if (index == -1) {
			return null;
		}

		String duplicateId = WidgetType.getDuplicateWidgetId(widgetType);
		MapWidget duplicateWidget = new MapWidgetsFactory(mapActivity).createMapWidget(duplicateId, widgetType, panel);
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);
		MapWidgetInfo duplicateWidgetInfo = creator.askCreateWidgetInfo(duplicateId, duplicateWidget, widgetType, panel);
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
			List<String> orderOfPage = pagedOrder.get(widgetInfo.pageIndex);
			if (orderOfPage == null) {
				orderOfPage = new ArrayList<>();
				pagedOrder.put(widgetInfo.pageIndex, orderOfPage);
			}

			orderOfPage.add(widgetInfo.key);
			if (this.widgetInfo.key.equals(widgetInfo.key)) {
				orderOfPage.add(duplicateId);
			}
		}

		panel.setWidgetsOrder(appMode, new ArrayList<>(pagedOrder.values()), settings);

		MapInfoLayer mapInfoLayer = app.getOsmandMap().getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			mapInfoLayer.recreateAllControls(mapActivity);
		}

		return duplicateId;
	}

	private void showDuplicateAddedSnackbar(String duplicateId) {
		Snackbar snackbar = Snackbar.make(view, R.string.duplacate_widget_added_snackbar, Snackbar.LENGTH_LONG);
		UiUtilities.setupSnackbar(snackbar, nightMode);
		snackbar.setAction(R.string.info_button, v -> updateWidgetInfo(duplicateId));
		snackbar.show();
	}

	private void updateWidgetInfo(@NonNull String duplicateId) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			List<MapWidgetInfo> widgetInfos = new ArrayList<>(widgetRegistry.getWidgetsForPanel(mapActivity,
					appMode, MATCHING_PANELS_MODE, Collections.singletonList(panel)));
			for (MapWidgetInfo widgetInfo : widgetInfos) {
				if (widgetInfo.key.equals(duplicateId)) {
					this.widgetInfo = widgetInfo;
				}
			}
		}
	}

	private void hideBottomButton() {
		View buttonsShadow = view.findViewById(R.id.buttons_shadow);
		View buttonsContainer = view.findViewById(R.id.buttons_container);
		AndroidUiHelper.setVisibility(View.GONE, buttonsShadow, buttonsContainer);
	}

	private void setWidgetIcon(@NonNull ImageView ivIcon) {
		if (widgetType != null && widgetInfo.getWidgetState() == null) {
			ivIcon.setImageResource(widgetType.getIconId(nightMode));
		} else {
			iconsHelper.updateWidgetIcon(ivIcon, widgetInfo);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(APP_MODE_KEY, appMode.getStringKey());
		outState.putString(WIDGET_ID_KEY, widgetInfo.key);
	}

	@Override
	public void onWidgetsConfigurationChanged() {
		setupWidgetItem();
		notifyTarget();
	}

	private void notifyTarget() {
		Fragment target = getTargetFragment();
		if (target instanceof WidgetsConfigurationChangeListener) {
			((WidgetsConfigurationChangeListener) target).onWidgetsConfigurationChanged();
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		recreateFragment();
	}

	@Override
	protected String getFragmentTag() {
		return TAG;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @Nullable Fragment target,
	                                @NonNull ApplicationMode appMode,
	                                @NonNull String widgetId) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			WidgetInfoFragment fragment = new WidgetInfoFragment();
			fragment.setTargetFragment(target, 0);
			Bundle args = new Bundle();
			args.putString(APP_MODE_KEY, appMode.getStringKey());
			args.putString(WIDGET_ID_KEY, widgetId);
			fragment.setArguments(args);
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	private enum Action {

		SETTINGS(R.string.shared_string_settings, R.drawable.ic_action_settings, R.id.settings_action),
		DUPLICATE(R.string.shared_string_duplicate, R.drawable.ic_action_copy, R.id.duplicate_action),
		REMOVE(R.string.shared_string_remove, R.drawable.ic_action_clear_all_fields, R.id.remove_action);

		@StringRes
		public int titleId;
		@DrawableRes
		private final int iconId;
		@IdRes
		private final int containerId;

		Action(@StringRes int titleId, @DrawableRes int iconId, @IdRes int containerId) {
			this.titleId = titleId;
			this.iconId = iconId;
			this.containerId = containerId;
		}

		public boolean isAvailable(@NonNull MapWidgetInfo widgetInfo, @NonNull Context ctx) {
			WidgetType widgetType = widgetInfo.widget.getWidgetType();
			switch (this) {
				case SETTINGS:
					return widgetType != null && widgetType.getSettingsFragment(ctx, widgetInfo) != null;
				case DUPLICATE:
					return widgetType != null && widgetType.isPurchased(ctx);
				case REMOVE:
					return true;
				default:
					throw new IllegalStateException("Unsupported action");
			}
		}

		public boolean showBottomDivider() {
			return this == SETTINGS;
		}
	}
}