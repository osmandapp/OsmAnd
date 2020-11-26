package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.BooleanPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.router.GeneralRouter.RoutingParameter;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.getCustomButtonView;
import static net.osmand.plus.settings.bottomsheets.BooleanPreferenceBottomSheet.updateCustomButtonView;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.setRoutingParameterSelected;
import static net.osmand.router.GeneralRouter.USE_HEIGHT_OBSTACLES;

public class ElevationDateBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ElevationDateBottomSheet.class.getSimpleName();

	private static final Log LOG = PlatformUtil.getLog(ElevationDateBottomSheet.class);

	private OsmandApplication app;
	private ApplicationMode appMode;
	private List<RoutingParameter> reliefFactorParameters = new ArrayList<RoutingParameter>();
	private static final String SELECTED_ENTRY_INDEX_KEY = "selected_entry_index_key";

	private final List<BottomSheetItemWithCompoundButton> reliefFactorButtons = new ArrayList<>();
	private int selectedEntryIndex = -1;

	public void setAppMode(ApplicationMode appMode) {
		this.appMode = appMode;
	}

	public ApplicationMode getAppMode() {
		return appMode != null ? appMode : app.getSettings().getApplicationMode();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		Context ctx = requireContext();
		int contentPaddingSmall = getResources().getDimensionPixelSize(R.dimen.content_padding_small);

		final BooleanPreference pref = (BooleanPreference) app.getSettings().getCustomRoutingBooleanProperty(USE_HEIGHT_OBSTACLES, false);

		Context themedCtx = UiUtilities.getThemedContext(ctx, nightMode);

		final String on = getString(R.string.shared_string_enable);
		final String off = getString(R.string.shared_string_disable);
		final int activeColor = AndroidUtils.resolveAttribute(themedCtx, R.attr.active_color_basic);
		final int disabledColor = AndroidUtils.resolveAttribute(themedCtx, android.R.attr.textColorSecondary);
		if (savedInstanceState != null) {
			selectedEntryIndex = savedInstanceState.getInt(SELECTED_ENTRY_INDEX_KEY);
		}
		boolean checked = pref.getModeValue(getAppMode());
		final BottomSheetItemWithCompoundButton[] preferenceBtn = new BottomSheetItemWithCompoundButton[1];
		preferenceBtn[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(checked)
				.setTitle(checked ? on : off)
				.setTitleColorId(checked ? activeColor : disabledColor)
				.setCustomView(getCustomButtonView(app, getAppMode(), checked, nightMode))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean newValue = !pref.getModeValue(getAppMode());
						enableItems(newValue);
						Fragment targetFragment = getTargetFragment();
						pref.setModeValue(getAppMode(), newValue);

						preferenceBtn[0].setTitle(newValue ? on : off);
						preferenceBtn[0].setChecked(newValue);
						preferenceBtn[0].setTitleColorId(newValue ? activeColor : disabledColor);
						updateCustomButtonView(app, getAppMode(), v, newValue, nightMode);

						if (targetFragment instanceof OnPreferenceChanged) {
							((OnPreferenceChanged) targetFragment).onPreferenceChanged(pref.getId());
						}
						if (targetFragment instanceof BaseSettingsFragment) {
							((BaseSettingsFragment) targetFragment).updateSetting(pref.getId());
						}
					}
				})
				.create();
		preferenceBtn[0].setCompoundButtonColorId(getAppMode().getIconColorInfo().getColor(nightMode));
		items.add(new TitleItem(getString(R.string.routing_attr_height_obstacles_name)));
		items.add(preferenceBtn[0]);
		items.add(new DividerSpaceItem(getMyApplication(), contentPaddingSmall));
		items.add(new LongDescriptionItem(getString(R.string.elevation_data)));
		items.add(new DividerSpaceItem(getMyApplication(), contentPaddingSmall));

		for (int i = 0; i < reliefFactorParameters.size(); i++) {
			RoutingParameter parameter = reliefFactorParameters.get(i);
			final BottomSheetItemWithCompoundButton[] preferenceItem = new BottomSheetItemWithCompoundButton[1];
			preferenceItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(i == selectedEntryIndex)
					.setButtonTintList(AndroidUtils.createCheckedColorStateList(ctx, R.color.icon_color_default_light, getAppMode().getIconColorInfo().getColor(nightMode)))
					.setTitle(getRoutingParameterTitle(app, parameter))
					.setTag(i)
					.setLayoutId(R.layout.bottom_sheet_item_with_radio_btn_left)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							selectedEntryIndex = (int) preferenceItem[0].getTag();
							if (selectedEntryIndex >= 0) {
								RoutingParameter parameter = reliefFactorParameters.get(selectedEntryIndex);

								String selectedParameterId = parameter.getId();
								for (RoutingParameter p : reliefFactorParameters) {
									String parameterId = p.getId();
									setRoutingParameterSelected(app.getSettings(), appMode, parameterId, p.getDefaultBoolean(), parameterId.equals(selectedParameterId));
								}
								recalculateRoute();

								Fragment targetFragment = getTargetFragment();
								if (targetFragment instanceof OnPreferenceChanged) {
									((OnPreferenceChanged) targetFragment).onPreferenceChanged(pref.getId());
								}
							}
							updateItems();
						}
					})
					.create();
			reliefFactorButtons.add(preferenceItem[0]);
			items.add(preferenceItem[0]);
		}
	}

	private void recalculateRoute() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (getAppMode().equals(routingHelper.getAppMode())
				&& (routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated())) {
			routingHelper.recalculateRouteDueToSettingsChange();
		}
	}

	private String getRoutingParameterTitle(Context context, RoutingParameter parameter) {
		return AndroidUtils.getRoutingStringPropertyName(context, parameter.getId(), parameter.getName());
	}

	private void updateItems() {
		for (BaseBottomSheetItem item : reliefFactorButtons) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				boolean checked = item.getTag().equals(selectedEntryIndex);
				((BottomSheetItemWithCompoundButton) item).setChecked(checked);
			}
		}
	}

	private void enableItems(boolean enable) {
		for (BaseBottomSheetItem item : reliefFactorButtons) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				item.getView().setEnabled(enable);
			}
		}
	}

	public static void showInstance(FragmentManager fm, List<RoutingParameter> reliefFactorParameters,
									ApplicationMode appMode, Fragment target, boolean usedOnMap) {
		try {
			if (fm.findFragmentByTag(ElevationDateBottomSheet.TAG) == null) {
				ElevationDateBottomSheet fragment = new ElevationDateBottomSheet();
				fragment.setAppMode(appMode);
				fragment.setUsedOnMap(usedOnMap);
				fragment.reliefFactorParameters.addAll(reliefFactorParameters);
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, ScreenTimeoutBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}

