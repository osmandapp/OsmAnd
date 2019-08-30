package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiSelectPreferencesBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = MultiSelectPreferencesBottomSheet.class.getSimpleName();

	private static final String TITLE_KEY = "title_key";
	private static final String DESCRIPTION_KEY = "description_key";
	private static final String PREFERENCES_PARAMETERS_KEY = "preferences_parameters_key";

	private RoutingOptionsHelper routingOptionsHelper;

	private HashMap<String, Boolean> routingParametersMap;

	private String title = "";
	private String description = "";

	String[] vals = null;
	OsmandSettings.OsmandPreference[] bls = null;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		routingOptionsHelper = app.getRoutingOptionsHelper();
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(TITLE_KEY)) {
				title = savedInstanceState.getString(PREFERENCES_PARAMETERS_KEY);
			}
			if (savedInstanceState.containsKey(DESCRIPTION_KEY)) {
				description = savedInstanceState.getString(PREFERENCES_PARAMETERS_KEY);
			}
			if (savedInstanceState.containsKey(PREFERENCES_PARAMETERS_KEY)) {
				routingParametersMap = (HashMap<String, Boolean>) savedInstanceState.getSerializable(PREFERENCES_PARAMETERS_KEY);
			}
		}
		if (routingParametersMap == null) {
			routingParametersMap = getRoutingParametersMap(app);
		}

		items.add(new TitleItem(title));

		if (!Algorithms.isEmpty(description)) {
			items.add(new LongDescriptionItem(description));
		}

		populateImpassableRoadsTypes();
	}

	private void populateImpassableRoadsTypes() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		for (Map.Entry<String, Boolean> entry : routingParametersMap.entrySet()) {
			final String parameterId = entry.getKey();
			boolean selected = entry.getValue();
			GeneralRouter.RoutingParameter parameter = routingOptionsHelper.getRoutingPrefsForAppModeById(app.getRoutingHelper().getAppMode(), parameterId);
			String defValue = "";
			if (parameter != null) {
				defValue = parameter.getName();
			}
			String parameterName = SettingsBaseActivity.getRoutingStringPropertyName(app, parameterId, defValue);

			final BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(selected)
					.setTitle(parameterName)
					.setLayoutId(R.layout.bottom_sheet_item_with_switch_no_icon)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							item[0].setChecked(!item[0].isChecked());
							routingParametersMap.put(parameterId, item[0].isChecked());
						}
					})
					.setTag(parameterId)
					.create();
			items.add(item[0]);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				final String routingParameterId = (String) item.getTag();
				((BottomSheetItemWithCompoundButton) item).setChecked(routingParametersMap.get(routingParameterId));
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(PREFERENCES_PARAMETERS_KEY, routingParametersMap);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		final OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}

		for (Map.Entry<String, Boolean> entry : routingParametersMap.entrySet()) {
			String parameterId = entry.getKey();
			GeneralRouter.RoutingParameter parameter = routingOptionsHelper.getRoutingPrefsForAppModeById(app.getRoutingHelper().getAppMode(), parameterId);
			if (parameter != null) {
				boolean checked = entry.getValue();
				OsmandSettings.CommonPreference<Boolean> preference = app.getSettings().getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
				preference.setModeValue(app.getRoutingHelper().getAppMode(), checked);
			}
		}

		app.getRoutingHelper().recalculateRouteDueToSettingsChange();

		dismiss();
	}

	@NonNull
	private HashMap<String, Boolean> getRoutingParametersMap(OsmandApplication app) {
		HashMap<String, Boolean> res = new HashMap<>();
		List<GeneralRouter.RoutingParameter> avoidParameters = routingOptionsHelper.getAvoidRoutingPrefsForAppMode(app.getRoutingHelper().getAppMode());

		for (GeneralRouter.RoutingParameter parameter : avoidParameters) {
			OsmandSettings.CommonPreference<Boolean> preference = app.getSettings().getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
			res.put(parameter.getId(), preference.getModeValue(app.getRoutingHelper().getAppMode()));
		}

		return res;
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String title, String description, String[] vals, OsmandSettings.OsmandPreference[] bls, Fragment target) {
		try {
			Bundle args = new Bundle();

			MultiSelectPreferencesBottomSheet fragment = new MultiSelectPreferencesBottomSheet();
			fragment.title = title;
			fragment.description = description;
			fragment.vals = vals;
			fragment.bls = bls;
			fragment.setTargetFragment(target, 0);
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}