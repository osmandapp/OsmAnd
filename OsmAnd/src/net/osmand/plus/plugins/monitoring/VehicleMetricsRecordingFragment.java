package net.osmand.plus.plugins.monitoring;

import static net.osmand.plus.plugins.monitoring.VehicleMetricsRecordingFragment.VehicleMetricsRecordingCategory.ENGINE;
import static net.osmand.plus.plugins.monitoring.VehicleMetricsRecordingFragment.VehicleMetricsRecordingCategory.FUEL;
import static net.osmand.plus.plugins.monitoring.VehicleMetricsRecordingFragment.VehicleMetricsRecordingCategory.OTHER;
import static net.osmand.plus.plugins.monitoring.VehicleMetricsRecordingFragment.VehicleMetricsRecordingCategory.TEMPERATURE;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.odb.VehicleMetricsPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.dialogs.DistanceByTapFragment;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.shared.obd.OBDCommand;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class VehicleMetricsRecordingFragment extends BaseFullScreenFragment {

	public static final String TAG = DistanceByTapFragment.class.getSimpleName();
	public static final String SELECTED_COMMANDS_KEY = "selected_commands_key";

	private Toolbar toolbar;
	private ImageView navigationIcon;

	private RecyclerView recyclerView;
	private VehicleMetricsRecordingAdapter adapter;

	private Set<String> selectedCommands = new HashSet<>();
	private final List<Object> items = new ArrayList<>();
	private DialogButton selectAllButton;
	private DialogButton applyButton;
	private ListStringPreference preference;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.vehicle_metrics_recording_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		Window window = requireMapActivity().getWindow();
		if (window != null) {
			AndroidUiHelper.setStatusBarColor(window, ContextCompat.getColor(requireMapActivity(), getStatusBarColorId()));
			AndroidUiHelper.setStatusBarContentColor(window.getDecorView(), nightMode);
		}

		toolbar = view.findViewById(R.id.toolbar);
		AppBarLayout appBarLayout = view.findViewById(R.id.app_bar);
		ViewCompat.setElevation(appBarLayout, 5.0f);

		navigationIcon = toolbar.findViewById(R.id.close_button);
		recyclerView = view.findViewById(R.id.recycler_view);
		selectAllButton = view.findViewById(R.id.dismiss_button);
		applyButton = view.findViewById(R.id.right_bottom_button);
		AndroidUiHelper.updateVisibility(applyButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);

		selectedCommands = new HashSet<>();
		preference = PluginsHelper.requirePlugin(VehicleMetricsPlugin.class).getTRIP_RECORDING_VEHICLE_METRICS();

		if (savedInstanceState != null) {
			selectedCommands.addAll(Objects.requireNonNull(savedInstanceState.getStringArrayList(SELECTED_COMMANDS_KEY)));
		} else {
			List<String> commands = preference.getStringsListForProfile(appMode);
			if (commands != null) {
				selectedCommands.addAll(commands);
			}
		}

		updateSelectAllButton();
		setupButtons();
		setupToolbar();
		setupItems();

		return view;
	}

	@SuppressLint("NotifyDataSetChanged")
	private void setupButtons() {
		selectAllButton.setOnClickListener(v -> {
			if (areAllCommandsSelected()) {
				selectedCommands.clear();
			} else {
				for (VehicleMetricsItem item : VehicleMetricsItem.values()) {
					selectedCommands.add(item.command.name());
				}
			}
			updateSelectAllButton();
			adapter.notifyDataSetChanged();
		});

		applyButton.setTitleId(R.string.shared_string_apply);
		applyButton.setOnClickListener(v -> {
			applyPreferenceWithSnackBar(new ArrayList<>(selectedCommands));
			dismiss();
		});
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(SELECTED_COMMANDS_KEY, new ArrayList<>(selectedCommands));
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	private boolean areAllCommandsSelected() {
		return selectedCommands.containsAll(VehicleMetricsItem.getFlatOBDCommandsList());
	}

	private void setupToolbar() {
		TextView tvTitle = toolbar.findViewById(R.id.toolbar_title);
		tvTitle.setText(R.string.obd_widget_group);

		updateToolbarNavigationIcon();
		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false);
	}

	private void setupItems() {
		adapter = new VehicleMetricsRecordingAdapter(requireMapActivity(), new VehicleMetricsRecordingAdapter.VehicleMetricsRecordingListener() {
			@Override
			public void onCommandClick(@NonNull OBDCommand command) {
				boolean oldState = isCommandSelected(command);
				if (oldState) {
					selectedCommands.remove(command.name());
				} else {
					selectedCommands.add(command.name());
				}
				updateSelectAllButton();
			}

			@Override
			public boolean isCommandSelected(@NonNull OBDCommand command) {
				return selectedCommands.contains(command.name());
			}
		}, nightMode);

		items.clear();
		items.add(VehicleMetricsRecordingAdapter.DESCRIPTION_TYPE);
		for (VehicleMetricsRecordingCategory category : VehicleMetricsRecordingCategory.values()) {
			setupCategory(category);
		}

		recyclerView.setLayoutManager(new LinearLayoutManager(requireMapActivity()));
		recyclerView.setAdapter(adapter);
		adapter.setItems(items);
	}

	private void updateSelectAllButton() {
		selectAllButton.setTitleId(areAllCommandsSelected() ? R.string.shared_string_deselect_all : R.string.shared_string_select_all);
		applyButton.setEnabled(!areListsEqual(selectedCommands, preference.getStringsListForProfile(appMode)));
	}

	private boolean areListsEqual(@NonNull Set<String> set, @Nullable List<String> list) {
		if (list == null || set.size() != list.size()) {
			return false;
		}

		Set<String> set2 = new HashSet<>(list);
		return set.equals(set2);
	}

	private void setupCategory(@NonNull VehicleMetricsRecordingCategory category) {
		List<VehicleMetricsItem> sortedCommands = getSortedCommands(category);
		items.add(VehicleMetricsRecordingAdapter.DIVIDER_TYPE);
		items.add(category);
		items.addAll(sortedCommands);
	}

	@NonNull
	private List<VehicleMetricsItem> getSortedCommands(@NonNull VehicleMetricsRecordingCategory category) {
		List<VehicleMetricsItem> commandItems = VehicleMetricsItem.getCategoryCommands(category);
		Collator collator = OsmAndCollator.primaryCollator();
		commandItems.sort((indexItem, indexItem2) -> collator.compare(app.getString(indexItem.nameId), app.getString(indexItem2.nameId)));

		return commandItems;
	}

	private void updateToolbarNavigationIcon() {
		navigationIcon.setOnClickListener(view -> dismiss());
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
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	private void applyPreferenceWithSnackBar(@NonNull Serializable newValue) {
		Fragment fragment = getTargetFragment();
		String prefId = preference.getId();
		if (fragment instanceof MonitoringSettingsFragment monitoringSettingsFragment) {
			monitoringSettingsFragment.onConfirmPreferenceChange(prefId, newValue, ApplyQueryType.SNACK_BAR);
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity, @Nullable MonitoringSettingsFragment monitoringSettingsFragment, @NonNull ApplicationMode selectedAppMode) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			VehicleMetricsRecordingFragment fragment = new VehicleMetricsRecordingFragment();
			fragment.setAppMode(selectedAppMode);
			fragment.setTargetFragment(monitoringSettingsFragment, 0);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commit();
		}
	}

	enum VehicleMetricsItem {
		ENGINE_OIL_TEMP(OBDCommand.OBD_ENGINE_OIL_TEMPERATURE_COMMAND, R.string.obd_engine_oil_temperature, R.drawable.ic_action_obd_temperature_engine_oil, TEMPERATURE),
		ENGINE_COOLANT_TEMP(OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND, R.string.obd_engine_coolant_temp, R.drawable.ic_action_obd_temperature_coolant, TEMPERATURE),
		AIR_TEMP(OBDCommand.OBD_AMBIENT_AIR_TEMPERATURE_COMMAND, R.string.obd_ambient_air_temp, R.drawable.ic_action_obd_temperature_outside, TEMPERATURE),
		AIR_INTAKE_TEMP(OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND, R.string.obd_air_intake_temp, R.drawable.ic_action_obd_temperature_intake, TEMPERATURE),
		FUEL_LEVEL(OBDCommand.OBD_FUEL_LEVEL_COMMAND, R.string.remaining_fuel, R.drawable.ic_action_obd_fuel_remaining, FUEL),
		FUEL_PRESSURE(OBDCommand.OBD_FUEL_PRESSURE_COMMAND, R.string.obd_fuel_pressure, R.drawable.ic_action_obd_fuel_pressure, FUEL),
		FUEL_CONSUMPTION(OBDCommand.OBD_FUEL_CONSUMPTION_RATE_COMMAND, R.string.obd_fuel_consumption, R.drawable.ic_action_obd_fuel_consumption, FUEL),
		ENGINE_LOAD(OBDCommand.OBD_CALCULATED_ENGINE_LOAD_COMMAND, R.string.obd_calculated_engine_load, R.drawable.ic_action_car_info, ENGINE),
		ENGINE_RUNTIME(OBDCommand.OBD_ENGINE_RUNTIME_COMMAND, R.string.obd_engine_runtime, R.drawable.ic_action_car_running_time, ENGINE),
		ENGINE_RPM(OBDCommand.OBD_RPM_COMMAND, R.string.obd_widget_engine_speed, R.drawable.ic_action_obd_engine_speed, ENGINE),
		BATTERY_VOLTAGE(OBDCommand.OBD_BATTERY_VOLTAGE_COMMAND, R.string.obd_battery_voltage, R.drawable.ic_action_obd_battery_voltage, OTHER),
		THROTTLE_POSITION(OBDCommand.OBD_THROTTLE_POSITION_COMMAND, R.string.obd_throttle_position, R.drawable.ic_action_obd_throttle_position, OTHER),
		SPEED(OBDCommand.OBD_SPEED_COMMAND, R.string.obd_widget_vehicle_speed, R.drawable.ic_action_obd_speed, OTHER);

		public final OBDCommand command;
		public final int nameId;
		public final int iconId;
		public final VehicleMetricsRecordingCategory category;

		VehicleMetricsItem(@NonNull OBDCommand command, @StringRes int nameId, @DrawableRes int iconId, @NonNull VehicleMetricsRecordingCategory category) {
			this.nameId = nameId;
			this.iconId = iconId;
			this.command = command;
			this.category = category;
		}

		@NonNull
		public static List<String> getFlatOBDCommandsList() {
			List<String> commands = new ArrayList<>();
			for (VehicleMetricsItem metricsCommand : values()) {
				commands.add(metricsCommand.command.name());
			}
			return commands;
		}

		@NonNull
		public static List<VehicleMetricsItem> getCategoryCommands(VehicleMetricsRecordingCategory category) {
			List<VehicleMetricsItem> commands = new ArrayList<>();
			for (VehicleMetricsItem metricsCommand : values()) {
				if (metricsCommand.category == category) {
					commands.add(metricsCommand);
				}
			}
			return commands;
		}
	}

	enum VehicleMetricsRecordingCategory {
		TEMPERATURE(R.string.shared_string_temperature),
		FUEL(R.string.poi_filter_fuel),
		ENGINE(R.string.shared_string_engine),
		OTHER(R.string.shared_string_other);

		public final int titleId;

		VehicleMetricsRecordingCategory(@StringRes int titleId) {
			this.titleId = titleId;
		}
	}
}

