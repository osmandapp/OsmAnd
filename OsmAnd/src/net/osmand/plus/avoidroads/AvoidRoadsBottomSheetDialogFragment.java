package net.osmand.plus.avoidroads;

import static net.osmand.IndexConstants.AVOID_ROADS_FILE_EXT;
import static net.osmand.util.Algorithms.capitalizeFirstLetter;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.QuadRect;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvoidRoadsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = AvoidRoadsBottomSheetDialogFragment.class.getSimpleName();

	public static final int REQUEST_CODE = 0;
	public static final int OPEN_AVOID_ROADS_DIALOG_REQUEST_CODE = 1;

	private static final String ENABLED_FILES_IDS = "enabled_files_ids";
	private static final String AVOID_ROADS_TYPES_KEY = "avoid_roads_types";
	private static final String HIDE_IMPASSABLE_ROADS_KEY = "hide_impassable_roads";
	private static final String AVOID_ROADS_OBJECTS_KEY = "avoid_roads_objects";
	private static final String AVOID_ROADS_APP_MODE_KEY = "avoid_roads_app_mode";

	private DirectionPointsHelper pointsHelper;
	private RoutingOptionsHelper routingOptionsHelper;

	private Map<String, Boolean> routingParametersMap;
	private List<AvoidRoadInfo> removedImpassableRoads;
	private final List<String> enabledFiles = new ArrayList<>();
	private LinearLayout stylesContainer;

	private boolean hideImpassableRoads;
	@ColorInt
	private Integer compoundButtonColor;

	public void setHideImpassableRoads(boolean hideImpassableRoads) {
		this.hideImpassableRoads = hideImpassableRoads;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		pointsHelper = app.getAvoidSpecificRoads().getPointsHelper();
		routingOptionsHelper = app.getRoutingOptionsHelper();

		compoundButtonColor = appMode.getProfileColor(nightMode);
		if (savedInstanceState != null) {
			hideImpassableRoads = savedInstanceState.getBoolean(HIDE_IMPASSABLE_ROADS_KEY);
			if (savedInstanceState.containsKey(AVOID_ROADS_TYPES_KEY)) {
				routingParametersMap = (HashMap<String, Boolean>) AndroidUtils.getSerializable(savedInstanceState, AVOID_ROADS_TYPES_KEY, HashMap.class);
			}
			if (savedInstanceState.containsKey(AVOID_ROADS_OBJECTS_KEY)) {
				removedImpassableRoads = (List<AvoidRoadInfo>) AndroidUtils.getSerializable(savedInstanceState, AVOID_ROADS_OBJECTS_KEY, ArrayList.class);
			}
		}
		List<String> selectedFileNames;
		if (savedInstanceState != null) {
			selectedFileNames = savedInstanceState.getStringArrayList(ENABLED_FILES_IDS);
		} else {
			selectedFileNames = pointsHelper.getSelectedFilesForMode(appMode);
		}
		if (!Algorithms.isEmpty(selectedFileNames)) {
			enabledFiles.addAll(selectedFileNames);
		}
		if (routingParametersMap == null) {
			routingParametersMap = getRoutingParametersMap();
		}
		if (removedImpassableRoads == null) {
			removedImpassableRoads = new ArrayList<>();
		}

		View titleView = inflate(R.layout.bottom_sheet_item_toolbar_title);
		TextView textView = titleView.findViewById(R.id.title);
		textView.setText(!hideImpassableRoads ? R.string.impassable_road : R.string.avoid_pt_types);

		Toolbar toolbar = titleView.findViewById(R.id.toolbar);
		int icBackResId = AndroidUtils.getNavigationIconResId(app);
		toolbar.setNavigationIcon(getContentIcon(icBackResId));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismiss());

		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create();
		items.add(titleItem);

		SimpleBottomSheetItem descriptionItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setTitle(!hideImpassableRoads ? getString(R.string.avoid_roads_descr) : getString(R.string.avoid_pt_types_descr))
				.setLayoutId(R.layout.bottom_sheet_item_title_long)
				.create();
		items.add(descriptionItem);

		stylesContainer = new LinearLayout(app);
		stylesContainer.setLayoutParams((new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)));
		stylesContainer.setOrientation(LinearLayout.VERTICAL);
		stylesContainer.setPadding(0, getDimensionPixelSize(R.dimen.bottom_sheet_content_padding_small), 0, 0);

		items.add(new BaseBottomSheetItem.Builder().setCustomView(stylesContainer).create());

		if (!hideImpassableRoads) {
			for (AvoidRoadInfo roadInfo : app.getAvoidSpecificRoads().getImpassableRoads()) {
				if (removedImpassableRoads.contains(roadInfo)) {
					continue;
				}
				inflate(R.layout.bottom_sheet_item_simple_right_icon, stylesContainer, true);
			}
			populateImpassableRoadsObjects();

			View buttonView = inflate(R.layout.bottom_sheet_item_btn);
			TextView buttonDescription = buttonView.findViewById(R.id.button_descr);
			buttonDescription.setText(R.string.shared_string_select_on_map);
			buttonDescription.setTextColor(ColorUtilities.getActiveColor(app, nightMode));

			FrameLayout buttonContainer = buttonView.findViewById(R.id.button_container);
			AndroidUtils.setBackground(app, buttonContainer, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
			AndroidUtils.setBackground(app, buttonDescription, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);

			buttonContainer.setOnClickListener(v -> {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					mapActivity.getMapRouteInfoMenu().hide();
					app.getAvoidSpecificRoads().selectFromMap(mapActivity, appMode);
					Fragment fragment = getTargetFragment();
					if (fragment != null) {
						fragment.onActivityResult(getTargetRequestCode(), OPEN_AVOID_ROADS_DIALOG_REQUEST_CODE, null);
					}
				}
				dismiss();
			});
			SimpleBottomSheetItem buttonItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
					.setCustomView(buttonView)
					.create();
			items.add(buttonItem);
		}

		items.add(new SubtitleDividerItem(app));

		populateImpassableRoadsTypes();
		populateImpassableRoadsFiles();
	}

	private void populateImpassableRoadsObjects() {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		AvoidRoadsHelper avoidRoadsHelper = app.getAvoidSpecificRoads();

		int counter = 0;
		for (AvoidRoadInfo roadInfo : avoidRoadsHelper.getImpassableRoads()) {
			if (removedImpassableRoads.contains(roadInfo)) {
				continue;
			}

			View view = stylesContainer.getChildAt(counter);
			view.setOnClickListener(v -> {
				removedImpassableRoads.add(roadInfo);
				stylesContainer.removeView(v);
			});

			TextView titleTv = view.findViewById(R.id.title);
			titleTv.setText(roadInfo.getName(app));
			titleTv.setTextColor(activeColor);

			ImageView icon = view.findViewById(R.id.icon);
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));

			counter++;
		}
	}

	private void populateImpassableRoadsTypes() {
		for (Map.Entry<String, Boolean> entry : routingParametersMap.entrySet()) {
			String parameterId = entry.getKey();
			boolean selected = entry.getValue();
			GeneralRouter.RoutingParameter parameter = routingOptionsHelper.getRoutingPrefsForAppModeById(app.getRoutingHelper().getAppMode(), parameterId);
			String defValue = "";
			if (parameter != null) {
				defValue = parameter.getName();
			}
			String parameterName = AndroidUtils.getRoutingStringPropertyName(app, parameterId, defValue);

			BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setCompoundButtonColor(compoundButtonColor)
					.setChecked(selected)
					.setTitle(parameterName)
					.setLayoutId(R.layout.bottom_sheet_item_with_switch_no_icon)
					.setOnClickListener(v -> {
						item[0].setChecked(!item[0].isChecked());
						routingParametersMap.put(parameterId, item[0].isChecked());
					})
					.setTag(parameterId)
					.create();
			items.add(item[0]);
		}
	}

	private void populateImpassableRoadsFiles() {
		List<File> avoidRoadsFiles = pointsHelper.collectAvoidRoadsFiles();
		if (!Algorithms.isEmpty(avoidRoadsFiles)) {
			items.add(new SubtitleDividerItem(app));
			items.add(new TitleItem(getString(R.string.files_with_route_restrictions)));

			for (File file : avoidRoadsFiles) {
				String fileName = file.getName();
				String name = capitalizeFirstLetter(fileName.replace(AVOID_ROADS_FILE_EXT, ""));
				boolean enabled = enabledFiles.contains(fileName);

				View itemView = inflate(R.layout.bottom_sheet_item_with_switch_and_dialog);
				AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.divider), false);

				BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
				item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
						.setChecked(enabled)
						.setTitle(name)
						.setIcon(getActiveIcon(R.drawable.ic_action_file_report))
						.setCustomView(itemView)
						.setOnClickListener(v -> {
							boolean checked = !item[0].isChecked();
							if (checked) {
								enabledFiles.add(fileName);
							} else {
								enabledFiles.remove(fileName);
							}
							item[0].setChecked(checked);
						})
						.create();

				pointsHelper.getDirectionPointsForFileAsync(file, result -> {
					int size = result.queryInBox(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE), new ArrayList<>()).size();

					String roads = getString(R.string.roads);
					String used = getString(enabled ? R.string.shared_string_used : R.string.shared_string_not_used);
					String roadsCount = getString(R.string.ltr_or_rtl_combine_via_colon, roads.toLowerCase(), String.valueOf(size));
					String description = getString(R.string.ltr_or_rtl_combine_via_bold_point, used, roadsCount);
					item[0].setDescription(description);
					return true;
				});
				items.add(item[0]);
			}
		}
	}

	public void setCompoundButtonColor(@ColorInt int compoundButtonColor) {
		this.compoundButtonColor = compoundButtonColor;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(AVOID_ROADS_TYPES_KEY, (Serializable) routingParametersMap);
		outState.putSerializable(AVOID_ROADS_OBJECTS_KEY, (Serializable) removedImpassableRoads);
		outState.putStringArrayList(ENABLED_FILES_IDS, (ArrayList<String>) enabledFiles);
		outState.putBoolean(HIDE_IMPASSABLE_ROADS_KEY, hideImpassableRoads);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		ApplicationMode mode = app.getRoutingHelper().getAppMode();
		for (Map.Entry<String, Boolean> entry : routingParametersMap.entrySet()) {
			String parameterId = entry.getKey();
			GeneralRouter.RoutingParameter parameter = routingOptionsHelper.getRoutingPrefsForAppModeById(mode, parameterId);
			if (parameter != null) {
				boolean checked = entry.getValue();
				CommonPreference<Boolean> preference = settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
				preference.setModeValue(mode, checked);
			}
		}

		AvoidRoadsHelper avoidRoadsHelper = app.getAvoidSpecificRoads();
		for (AvoidRoadInfo avoidRoadInfo : removedImpassableRoads) {
			avoidRoadsHelper.removeImpassableRoad(avoidRoadInfo);
		}

		app.getRoutingHelper().onSettingsChanged(true);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapRouteInfoMenu().updateMenu();
		}
		pointsHelper.setSelectedFilesForMode(mode, enabledFiles);

		dismiss();
	}

	@NonNull
	private HashMap<String, Boolean> getRoutingParametersMap() {
		HashMap<String, Boolean> map = new HashMap<>();
		Map<RoutingParameter, Boolean> parameters = routingOptionsHelper.getAvoidParametersWithStates(app);
		for (Map.Entry<RoutingParameter, Boolean> entry : parameters.entrySet()) {
			map.put(entry.getKey().getId(), entry.getValue());
		}
		return map;
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		showInstance(activity, null, null,null, null);
	}

	public static void showInstance(@NonNull FragmentActivity activity, @Nullable Fragment targetFragment,
	                                @Nullable ApplicationMode appMode, @Nullable Boolean hideImpassableRoads,
	                                @Nullable @ColorInt Integer accentColor) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			AvoidRoadsBottomSheetDialogFragment avoidRoadsFragment = new AvoidRoadsBottomSheetDialogFragment();
			if (appMode != null) {
				avoidRoadsFragment.setAppMode(appMode);
			}
			if (hideImpassableRoads != null) {
				avoidRoadsFragment.setHideImpassableRoads(hideImpassableRoads);
			}
			if (targetFragment != null) {
				avoidRoadsFragment.setTargetFragment(targetFragment, REQUEST_CODE);
			}
			if (accentColor != null) {
				avoidRoadsFragment.setCompoundButtonColor(accentColor);
			}
			avoidRoadsFragment.show(fragmentManager, AvoidRoadsBottomSheetDialogFragment.TAG);
		}
	}
}