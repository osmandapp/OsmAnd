package net.osmand.plus.plugins.monitoring;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.dialogs.DistanceByTapFragment;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.shared.obd.OBDCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VehicleMetricsRecordingFragment extends BaseOsmAndFragment {

	public static final String TAG = DistanceByTapFragment.class.getSimpleName();

	private Toolbar toolbar;
	private ImageView navigationIcon;

	private RecyclerView recyclerView;
	private VehicleMetricsRecordingAdapter adapter;

	private final Set<String> selectedCommands = new HashSet<>();
	private final List<Object> items = new ArrayList<>();
	private DialogButton selectAllButton;
	private boolean allCommandsSelected = false;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.vehicle_metrics_recording_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		toolbar = view.findViewById(R.id.toolbar);
		AppBarLayout appBarLayout = view.findViewById(R.id.app_bar);
		ViewCompat.setElevation(appBarLayout, 5.0f);

		navigationIcon = toolbar.findViewById(R.id.close_button);
		recyclerView = view.findViewById(R.id.recycler_view);
		selectAllButton = view.findViewById(R.id.select_all);

		ListStringPreference preference = settings.TRIP_RECORDING_VEHICLE_METRICS;
		List<String> commands = preference.getStringsList();
		if (commands != null) {
			selectedCommands.addAll(preference.getStringsList());
		}

		updateAllCommandsSelected();
		setupButtons(view);
		setupToolbar();
		setupItems();

		return view;
	}

	@SuppressLint("NotifyDataSetChanged")
	private void setupButtons(@NonNull View view) {
		selectAllButton.setOnClickListener(v -> {
			if (allCommandsSelected) {
				selectedCommands.clear();
			} else {
				for (OBDCommand command : VehicleMetricsRecordingCategory.getFlatListAvailableCommands()) {
					selectedCommands.add(command.name());
				}
			}
			updateAllCommandsSelected();
			adapter.notifyDataSetChanged();
		});

		DialogButton applyButton = view.findViewById(R.id.apply_button);
		applyButton.setOnClickListener(v -> {
			settings.TRIP_RECORDING_VEHICLE_METRICS.setStringsList(new ArrayList<>(selectedCommands));
			Fragment fragment = getTargetFragment();
			if (fragment instanceof MonitoringSettingsFragment monitoringSettingsFragment) {
				monitoringSettingsFragment.setupObdRecordingPref();
			}
			dismiss();
		});
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	private void updateAllCommandsSelected() {
		allCommandsSelected = true;
		for (VehicleMetricsRecordingCategory category : VehicleMetricsRecordingCategory.values()) {
			for (OBDCommand command : category.categoryCommands) {
				if (!selectedCommands.contains(command.name())) {
					allCommandsSelected = false;
					break;
				}
			}
		}
		updateSelectAllButton();
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
				updateAllCommandsSelected();
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
		selectAllButton.setTitleId(allCommandsSelected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all);
	}

	private void setupCategory(@NonNull VehicleMetricsRecordingCategory category) {
		List<CommandItem> sortedCommands = getSortedCommands(category);
		items.add(VehicleMetricsRecordingAdapter.DIVIDER_TYPE);
		items.add(category);
		items.addAll(sortedCommands);
	}

	@NonNull
	private List<CommandItem> getSortedCommands(@NonNull VehicleMetricsRecordingCategory category) {
		List<CommandItem> commandItems = new ArrayList<>();
		for (OBDCommand command : category.categoryCommands) {
			commandItems.add(new CommandItem(getItemIcon(command), getItemTitle(command), command));
		}

		return commandItems.stream()
				.sorted(Comparator.comparing(CommandItem::name))
				.collect(Collectors.toList());
	}

	public int getDimen(@DimenRes int id) {
		return getResources().getDimensionPixelSize(id);
	}

	@NonNull
	private String getItemTitle(@NonNull OBDCommand obdCommand) {
		int titleId;
		titleId = switch (obdCommand) {
			case OBD_CALCULATED_ENGINE_LOAD_COMMAND -> R.string.obd_calculated_engine_load;
			case OBD_THROTTLE_POSITION_COMMAND -> R.string.obd_throttle_position;
			case OBD_ENGINE_OIL_TEMPERATURE_COMMAND -> R.string.obd_engine_oil_temperature;
			case OBD_FUEL_PRESSURE_COMMAND -> R.string.obd_fuel_pressure;
			case OBD_BATTERY_VOLTAGE_COMMAND -> R.string.obd_battery_voltage;
			case OBD_AMBIENT_AIR_TEMPERATURE_COMMAND -> R.string.obd_ambient_air_temp;
			case OBD_RPM_COMMAND -> R.string.obd_widget_engine_speed;
			case OBD_ENGINE_RUNTIME_COMMAND -> R.string.obd_engine_runtime;
			case OBD_SPEED_COMMAND -> R.string.obd_widget_vehicle_speed;
			case OBD_AIR_INTAKE_TEMP_COMMAND -> R.string.obd_air_intake_temp;
			case OBD_ENGINE_COOLANT_TEMP_COMMAND -> R.string.obd_engine_coolant_temp;
			case OBD_FUEL_CONSUMPTION_RATE_COMMAND -> R.string.obd_fuel_consumption;
			case OBD_FUEL_LEVEL_COMMAND -> R.string.remaining_fuel;
			default -> 0;
		};

		if (titleId == 0) {
			return obdCommand.name();
		} else {
			return app.getString(titleId);
		}
	}

	@DrawableRes
	private int getItemIcon(@NonNull OBDCommand obdCommand) {
		int iconId;
		iconId = switch (obdCommand) {
			case OBD_CALCULATED_ENGINE_LOAD_COMMAND -> R.drawable.ic_action_car_info;
			case OBD_THROTTLE_POSITION_COMMAND -> R.drawable.ic_action_obd_throttle_position;
			case OBD_ENGINE_OIL_TEMPERATURE_COMMAND ->
					R.drawable.ic_action_obd_temperature_engine_oil;
			case OBD_FUEL_PRESSURE_COMMAND -> R.drawable.ic_action_obd_fuel_pressure;
			case OBD_BATTERY_VOLTAGE_COMMAND -> R.drawable.ic_action_obd_battery_voltage;
			case OBD_AMBIENT_AIR_TEMPERATURE_COMMAND ->
					R.drawable.ic_action_obd_temperature_outside;
			case OBD_RPM_COMMAND -> R.drawable.ic_action_obd_engine_speed;
			case OBD_ENGINE_RUNTIME_COMMAND -> R.drawable.ic_action_car_running_time;
			case OBD_SPEED_COMMAND -> R.drawable.ic_action_obd_speed;
			case OBD_AIR_INTAKE_TEMP_COMMAND -> R.drawable.ic_action_obd_temperature_intake;
			case OBD_ENGINE_COOLANT_TEMP_COMMAND -> R.drawable.ic_action_obd_temperature_coolant;
			case OBD_FUEL_CONSUMPTION_RATE_COMMAND -> R.drawable.ic_action_obd_fuel_consumption;
			case OBD_FUEL_LEVEL_COMMAND -> R.drawable.ic_action_obd_fuel_remaining;
			default -> 0;
		};
		return iconId;
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
		requireMapActivity().disableDrawer();
	}

	@Override
	public void onPause() {
		super.onPause();
		requireMapActivity().enableDrawer();
	}

	@NonNull
	private MapActivity requireMapActivity() {
		MapActivity mapActivity = (MapActivity) getMyActivity();
		if (mapActivity == null) {
			throw new IllegalStateException(this + " not attached to MapActivity.");
		}
		return mapActivity;
	}

	public static void showInstance(@NonNull FragmentActivity activity, @Nullable MonitoringSettingsFragment monitoringSettingsFragment) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			VehicleMetricsRecordingFragment fragment = new VehicleMetricsRecordingFragment();
			fragment.setTargetFragment(monitoringSettingsFragment, 0);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commit();
		}
	}

	record CommandItem(@DrawableRes int iconId, String name, OBDCommand command) {
		CommandItem(@DrawableRes int iconId, @NonNull String name, @NonNull OBDCommand command) {
			this.iconId = iconId;
			this.name = name;
			this.command = command;
		}
	}

	enum VehicleMetricsRecordingCategory {
		TEMPERATURE(R.string.shared_string_temperature, Arrays.asList(OBDCommand.OBD_ENGINE_OIL_TEMPERATURE_COMMAND,
				OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND,
				OBDCommand.OBD_AMBIENT_AIR_TEMPERATURE_COMMAND,
				OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND)),
		FUEL(R.string.poi_filter_fuel, Arrays.asList(OBDCommand.OBD_FUEL_LEVEL_COMMAND,
				OBDCommand.OBD_FUEL_PRESSURE_COMMAND,
				OBDCommand.OBD_FUEL_CONSUMPTION_RATE_COMMAND)),
		ENGINE(R.string.shared_string_engine, Arrays.asList(OBDCommand.OBD_CALCULATED_ENGINE_LOAD_COMMAND,
				OBDCommand.OBD_ENGINE_RUNTIME_COMMAND,
				OBDCommand.OBD_RPM_COMMAND)),
		OTHER(R.string.shared_string_other, Arrays.asList(OBDCommand.OBD_BATTERY_VOLTAGE_COMMAND,
				OBDCommand.OBD_THROTTLE_POSITION_COMMAND,
				OBDCommand.OBD_SPEED_COMMAND));

		public final List<OBDCommand> categoryCommands;
		public final int titleId;

		VehicleMetricsRecordingCategory(@StringRes int titleId, @NonNull List<OBDCommand> categoryCommands) {
			this.categoryCommands = categoryCommands;
			this.titleId = titleId;
		}

		@NonNull
		static public List<OBDCommand> getFlatListAvailableCommands() {
			List<OBDCommand> availableCommands = new ArrayList<>();
			for (VehicleMetricsRecordingCategory category : VehicleMetricsRecordingCategory.values()) {
				availableCommands.addAll(category.categoryCommands);
			}
			return availableCommands;
		}
	}
}

