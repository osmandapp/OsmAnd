package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.routepreparationmenu.RouteOptionsBottomSheet;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.utils.AndroidUtils.createColorStateList;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.getCustomButtonView;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.updateCustomButtonView;
import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.RELIEF_SMOOTHNESS_FACTOR;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.getRoutingParameterTitle;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.isRoutingParameterSelected;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.updateSelectedParameters;
import static net.osmand.router.GeneralRouter.USE_HEIGHT_OBSTACLES;

public class ElevationDateBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ElevationDateBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(ElevationDateBottomSheet.class);

	private OsmandApplication app;
	private ApplicationMode appMode;
	private List<RoutingParameter> parameters;
	private CommonPreference<Boolean> useHeightPref;
	private LocalRoutingParameter heightObstacleParameter;

	private BottomSheetItemWithCompoundButton useHeightButton;
	private final List<BottomSheetItemWithCompoundButton> reliefFactorButtons = new ArrayList<>();

	private int selectedEntryIndex = -1;

	private String on;
	private String off;
	private int activeColor;
	private int checkedColor;
	private int uncheckedColor;
	private int disabledColor;
	@ColorInt
	private int appModeColor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		app = requiredMyApplication();
		if (savedInstanceState != null) {
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(APP_MODE_KEY), null);
		}
		super.onCreate(savedInstanceState);

		GeneralRouter router = app.getRouter(appMode);
		Map<String, RoutingParameter> routingParameterMap = RoutingHelperUtils.getParametersForDerivedProfile(appMode, router);
		RoutingParameter parameter = routingParameterMap.get(USE_HEIGHT_OBSTACLES);
		if (parameter != null) {
			useHeightPref = app.getSettings().getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
		} else {
			useHeightPref = app.getSettings().getCustomRoutingBooleanProperty(USE_HEIGHT_OBSTACLES, false);
		}
		parameters = getReliefParametersForMode(routingParameterMap);
		for (int i = 0; i < parameters.size(); i++) {
			if (isRoutingParameterSelected(app.getSettings(), appMode, parameters.get(i))) {
				selectedEntryIndex = i;
			}
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context themedCtx = UiUtilities.getThemedContext(requireContext(), nightMode);

		on = getString(R.string.shared_string_enabled);
		off = getString(R.string.shared_string_disabled);
		appModeColor = appMode.getProfileColor(nightMode);
		activeColor = AndroidUtils.resolveAttribute(themedCtx, R.attr.active_color_basic);
		disabledColor = AndroidUtils.resolveAttribute(themedCtx, android.R.attr.textColorSecondary);
		checkedColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		uncheckedColor = ColorUtilities.getSecondaryTextColor(app, nightMode);


		items.add(new TitleItem(getString(R.string.routing_attr_height_obstacles_name)));

		heightObstacleParameter = getHeightObstacleParameter();
		createUseHeightButton(themedCtx);

		int contentPaddingSmall = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		items.add(new DividerSpaceItem(app, contentPaddingSmall));
		items.add(new ShortDescriptionItem((getString(R.string.routing_attr_height_obstacles_description))));

		createReliefFactorButtons(themedCtx);
	}

	private LocalRoutingParameter getHeightObstacleParameter(){
		Fragment target = getTargetFragment();
		if (target instanceof RouteOptionsBottomSheet) {
			List<LocalRoutingParameter> list = ((RouteOptionsBottomSheet) target).getRoutingParameters(appMode);
			for (LocalRoutingParameter optionsItem : list) {
				if (USE_HEIGHT_OBSTACLES.equals(optionsItem.getKey())) {
					return optionsItem;
				}
			}
		}
		return null;
	}

	private void createUseHeightButton(Context context) {
		boolean checked = useHeightPref.getModeValue(appMode);
		useHeightButton = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setCompoundButtonColor(appModeColor)
				.setChecked(checked)
				.setTitle(checked ? on : off)
				.setTitleColorId(checked ? activeColor : disabledColor)
				.setCustomView(getCustomButtonView(app, appMode, checked, nightMode))
				.setOnClickListener(v -> {
					boolean newValue = !useHeightPref.getModeValue(appMode);
					Fragment target = getTargetFragment();
					if (target instanceof OnConfirmPreferenceChange) {
						OnConfirmPreferenceChange confirmInterface = (OnConfirmPreferenceChange) target;
						if (confirmInterface.onConfirmPreferenceChange(useHeightPref.getId(), newValue, ApplyQueryType.NONE)) {
							updateUseHeightButton(useHeightButton, newValue);

							if (target instanceof BaseSettingsFragment) {
								((BaseSettingsFragment) target).updateSetting(useHeightPref.getId());
							}
						}
					} else {
						applyRoutingParameter();

						useHeightPref.setModeValue(appMode, newValue);
						updateUseHeightButton(useHeightButton, newValue);
					}
				}).create();
		items.add(useHeightButton);
	}

	private void applyRoutingParameter() {
		if (heightObstacleParameter != null) {
			RoutingOptionsHelper routingOptionsHelper = app.getRoutingOptionsHelper();
			routingOptionsHelper.addNewRouteMenuParameter(appMode, heightObstacleParameter);
			boolean selected = !heightObstacleParameter.isSelected(app.getSettings());
			routingOptionsHelper.applyRoutingParameter(heightObstacleParameter, selected);
		}
	}

	private void updateUseHeightButton(BottomSheetItemWithCompoundButton button, boolean newValue) {
		enableDisableReliefButtons(newValue);
		button.setTitle(newValue ? on : off);
		button.setChecked(newValue);
		button.setTitleColorId(newValue ? activeColor : disabledColor);
		updateCustomButtonView(app, appMode, button.getView(), newValue, nightMode);
	}

	private void createReliefFactorButtons(Context context) {
		for (int i = 0; i < parameters.size(); i++) {
			RoutingParameter parameter = parameters.get(i);
			BottomSheetItemWithCompoundButton[] preferenceItem = new BottomSheetItemWithCompoundButton[1];
			preferenceItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(i == selectedEntryIndex)
					.setButtonTintList(createColorStateList(context, nightMode))
					.setTitle(getRoutingParameterTitle(app, parameter))
					.setLayoutId(R.layout.bottom_sheet_item_with_radio_btn_left)
					.setTag(i)
					.setOnClickListener(v -> {
						selectedEntryIndex = (int) preferenceItem[0].getTag();
						if (selectedEntryIndex >= 0) {
							RoutingParameter routingParameter = parameters.get(selectedEntryIndex);
							updateSelectedParameters(app, appMode, parameters, routingParameter.getId());
						}
						Fragment target = getTargetFragment();
						if (target instanceof BaseSettingsFragment) {
							((BaseSettingsFragment) target).updateSetting(useHeightPref.getId());
						}
						if (target instanceof RouteOptionsBottomSheet) {
							((RouteOptionsBottomSheet) target).updateMenuItems();
						}
						updateReliefButtons();
						app.runInUIThread(this::dismiss, 500);
					}).create();
			items.add(preferenceItem[0]);
			reliefFactorButtons.add(preferenceItem[0]);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(APP_MODE_KEY, appMode.getStringKey());
	}

	@Override
	public void onResume() {
		super.onResume();
		updateReliefButtons();
		enableDisableReliefButtons(useHeightButton.isChecked());
	}

	@Override
	public boolean isNightMode(@NonNull OsmandApplication app) {
		if (usedOnMap) {
			return app.getDaynightHelper().isNightModeForMapControlsForProfile(appMode);
		} else {
			return !app.getSettings().isLightContentForMode(appMode);
		}
	}

	private List<RoutingParameter> getReliefParametersForMode(Map<String, RoutingParameter> parameters) {
		List<RoutingParameter> reliefParameters = new ArrayList<>();
		for (RoutingParameter routingParameter : parameters.values()) {
			if (RELIEF_SMOOTHNESS_FACTOR.equals(routingParameter.getGroup())) {
				reliefParameters.add(routingParameter);
			}
		}
		return reliefParameters;
	}

	private void updateReliefButtons() {
		for (BottomSheetItemWithCompoundButton item : reliefFactorButtons) {
			item.setChecked(item.getTag().equals(selectedEntryIndex));
		}
	}

	private void enableDisableReliefButtons(boolean enable) {
		for (BaseBottomSheetItem item : reliefFactorButtons) {
			View view = item.getView();
			view.setEnabled(enable);
			view.findViewById(R.id.compound_button).setEnabled(enable);

			TextView titleField = view.findViewById(R.id.title);
			titleField.setTextColor(enable ? checkedColor : uncheckedColor);
		}
	}

	public static void showInstance(FragmentManager fm, ApplicationMode appMode, Fragment target, boolean usedOnMap) {
		try {
			if (!fm.isStateSaved() && fm.findFragmentByTag(TAG) == null) {
				ElevationDateBottomSheet fragment = new ElevationDateBottomSheet();
				fragment.appMode = appMode;
				fragment.setUsedOnMap(usedOnMap);
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}