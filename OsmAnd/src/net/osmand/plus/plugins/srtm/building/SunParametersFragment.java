package net.osmand.plus.plugins.srtm.building;

import static net.osmand.plus.dashboard.DashboardType.BUILDINGS_3D;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.DEFAULT_HILLSHADE_SUN_ANGLE;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.DEFAULT_HILLSHADE_SUN_AZIMUTH;

import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapOptionFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.RadiusRulerControlLayer;
import net.osmand.plus.widgets.ui.SegmentedList;
import net.osmand.plus.widgets.ui.SettingRow;
import net.osmand.plus.widgets.ui.SliderListItem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;

import kotlin.Unit;

/**
 * Light of the hillshade and the sun shadows: the real sun now (Live), the real sun at a chosen
 * date and time (Date), or azimuth and altitude set by hand (Manual).
 */
public class SunParametersFragment extends ConfigureMapOptionFragment {

	private static final String DATE_PICKER_TAG = "sun_date_picker";
	private static final long LIVE_REFRESH_INTERVAL_MS = 60_000L;
	private static final int MINUTES_PER_DAY = 24 * 60;

	private SunParametersController controller;

	private View content;
	private MaterialButtonToggleGroup modeToggle;
	private ViewGroup sunRows;
	private SettingRow azimuthRow;
	private SettingRow altitudeRow;
	private SettingRow dateRow;
	private SliderListItem timeSlider;
	private SliderListItem azimuthSlider;
	private SliderListItem altitudeSlider;
	private SettingRow shadowsRow;

	/* Live shows the sun of the current minute */
	private final Runnable liveRefresh = new Runnable() {
		@Override
		public void run() {
			if (content != null && isAdded() && controller.getMode() == SunLightMode.LIVE) {
				updateLiveRows();
				content.postDelayed(this, LIVE_REFRESH_INTERVAL_MS);
			}
		}
	};

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.astro_name_sun);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		controller = SunParametersController.getExistedInstance(app);
		if (controller == null) {
			dismiss();
			return;
		}
		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				activity.getSupportFragmentManager().popBackStack();
				activity.getDashboard().setDashboardVisibility(true, BUILDINGS_3D, false);
			}
		});
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		/* the frame of the screen is still Material 2 - the panel uses the Material 3 components */
		Context themedContext = new ContextThemeWrapper(requireContext(),
				nightMode ? R.style.OsmandMaterialDarkTheme : R.style.OsmandMaterialLightTheme);
		content = LayoutInflater.from(themedContext).inflate(R.layout.sun_parameters, container, false);

		modeToggle = content.findViewById(R.id.mode_toggle);
		sunRows = content.findViewById(R.id.sun_rows);
		int iconColor = AndroidUtils.getColorFromAttr(themedContext, R.attr.colorOnSurfaceVariant);

		azimuthRow = createInfoRow(R.id.azimuth_row, R.drawable.ic_action_compass, R.string.shared_string_azimuth, iconColor);
		altitudeRow = createInfoRow(R.id.altitude_row, R.drawable.ic_action_altitude, R.string.altitude, iconColor);

		dateRow = new SettingRow(content.findViewById(R.id.date_row));
		dateRow.setIcon(R.drawable.ic_action_calendar_month, iconColor);
		dateRow.setTitle(R.string.shared_string_date);
		dateRow.hideSwitch();
		dateRow.setOnClickListener(v -> showDatePicker());

		timeSlider = new SliderListItem(content.findViewById(R.id.time_row));
		timeSlider.setTitle(R.string.shared_string_time);
		azimuthSlider = new SliderListItem(content.findViewById(R.id.azimuth_slider_row));
		azimuthSlider.setTitle(R.string.shared_string_azimuth);
		altitudeSlider = new SliderListItem(content.findViewById(R.id.altitude_slider_row));
		altitudeSlider.setTitle(R.string.altitude);

		setupShadowsRow(iconColor);
		modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
			SunLightMode mode = getMode(checkedId);
			if (isChecked && mode != controller.getMode()) {
				controller.setMode(mode);
				updateContent();
				onChanged();
			}
		});
		restoreDatePicker();
		container.addView(content);
		updateContent();
		updateApplyButton(controller.hasChanges());
	}

	@NonNull
	private SettingRow createInfoRow(@IdRes int id, int iconId, int titleId, int iconColor) {
		SettingRow row = new SettingRow(content.findViewById(id));
		row.setIcon(iconId, iconColor);
		row.setTitle(titleId);
		row.hideSwitch();
		/* the row only shows a value, there is nothing to tap */
		row.getView().setClickable(false);
		row.getView().setFocusable(false);
		return row;
	}

	private void setupShadowsRow(int iconColor) {
		View rows = content.findViewById(R.id.shadows_rows);
		rows.setVisibility(app.useOpenGlRenderer() ? View.VISIBLE : View.GONE);
		shadowsRow = new SettingRow(content.findViewById(R.id.shadows_row));
		shadowsRow.setIcon(R.drawable.ic_action_3d_buildings, iconColor);
		shadowsRow.setTitle(R.string.cast_shadows);
		shadowsRow.setChecked(controller.isShadowsEnabled());
		shadowsRow.setOnClickListener(v -> {
			controller.setShadowsEnabled(!controller.isShadowsEnabled());
			shadowsRow.setChecked(controller.isShadowsEnabled());
			onChanged();
		});
		SegmentedList.apply((ViewGroup) rows);
	}

	/** Shows the rows of the selected mode and puts the values on them. */
	private void updateContent() {
		SunLightMode mode = controller.getMode();
		modeToggle.check(getButtonId(mode));

		boolean live = mode == SunLightMode.LIVE;
		boolean date = mode == SunLightMode.DATE;
		boolean manual = mode == SunLightMode.MANUAL;
		azimuthRow.getView().setVisibility(live ? View.VISIBLE : View.GONE);
		altitudeRow.getView().setVisibility(live ? View.VISIBLE : View.GONE);
		dateRow.getView().setVisibility(date ? View.VISIBLE : View.GONE);
		timeSlider.getView().setVisibility(date ? View.VISIBLE : View.GONE);
		azimuthSlider.getView().setVisibility(manual ? View.VISIBLE : View.GONE);
		altitudeSlider.getView().setVisibility(manual ? View.VISIBLE : View.GONE);
		SegmentedList.apply(sunRows);

		content.removeCallbacks(liveRefresh);
		if (live) {
			liveRefresh.run();
		} else if (date) {
			bindDate();
		} else {
			bindManual();
		}
	}

	private void updateLiveRows() {
		SunLight.Angles angles = controller.getSunAngles();
		azimuthRow.setSubtitle(formatAzimuth((int) Math.round(angles.getAzimuth()) % 360));
		altitudeRow.setSubtitle(formatAltitude((int) Math.round(angles.getAltitude())));
	}

	private void bindDate() {
		ZonedDateTime time = getZonedTime(controller.getTime());
		dateRow.setSubtitle(DateFormat.getLongDateFormat(requireContext()).format(Date.from(time.toInstant())));
		float minutes = time.getHour() * 60 + time.getMinute();
		timeSlider.setContinuous(0, MINUTES_PER_DAY - 1, 1, minutes, this::formatMinutes, value -> {
			ZonedDateTime current = getZonedTime(controller.getTime());
			ZonedDateTime changed = current.with(LocalTime.MIN).plusMinutes(Math.round(value));
			controller.setTime(changed.toInstant().toEpochMilli());
			onChanged();
			return Unit.INSTANCE;
		});
	}

	private void bindManual() {
		azimuthSlider.setContinuous(controller.getMinAzimuth(), controller.getMaxAzimuth(), 1,
				controller.getValidAzimuth(), value -> formatAzimuth(Math.round(value)), value -> {
					controller.setAzimuth(Math.round(value));
					onChanged();
					return Unit.INSTANCE;
				});
		altitudeSlider.setContinuous(controller.getMinAltitude(), controller.getMaxAltitude(), 1,
				controller.getValidAltitude(), value -> formatAltitude(Math.round(value)), value -> {
					controller.setAltitude(Math.round(value));
					onChanged();
					return Unit.INSTANCE;
				});
	}

	private void showDatePicker() {
		FragmentManager manager = getChildFragmentManager();
		if (!AndroidUtils.isFragmentCanBeAdded(manager, DATE_PICKER_TAG, true)) {
			return;
		}
		/* the picker works with midnight UTC of the selected day */
		LocalDate day = getZonedTime(controller.getTime()).toLocalDate();
		MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
				.setTheme(nightMode
						? R.style.ThemeOverlay_App_Material_DatePicker_Dark
						: R.style.ThemeOverlay_App_Material_DatePicker_Light)
				.setSelection(day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
				.build();
		addDateListener(picker);
		picker.show(manager, DATE_PICKER_TAG);
	}

	@SuppressWarnings("unchecked")
	private void restoreDatePicker() {
		Object picker = getChildFragmentManager().findFragmentByTag(DATE_PICKER_TAG);
		if (picker instanceof MaterialDatePicker) {
			addDateListener((MaterialDatePicker<Long>) picker);
		}
	}

	private void addDateListener(@NonNull MaterialDatePicker<Long> picker) {
		picker.addOnPositiveButtonClickListener(selection -> {
			LocalDate day = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
			ZonedDateTime current = getZonedTime(controller.getTime());
			ZonedDateTime changed = current.with(day);
			controller.setTime(changed.toInstant().toEpochMilli());
			bindDate();
			onChanged();
		});
	}

	private void onChanged() {
		updateApplyButton(controller.hasChanges());
		refreshMap();
	}

	@Override
	protected void applyChanges() {
		controller.onApplyChanges();
	}

	@Override
	protected void resetToDefault() {
		controller.setMode(SunLightMode.MANUAL);
		controller.setAzimuth(DEFAULT_HILLSHADE_SUN_AZIMUTH);
		controller.setAltitude(DEFAULT_HILLSHADE_SUN_ANGLE);
		controller.setShadowsEnabled(settings.CAST_SHADOWS.getDefaultValue());
		shadowsRow.setChecked(controller.isShadowsEnabled());
		updateContent();
		onChanged();
	}

	@Override
	public void onDestroyView() {
		if (content != null) {
			content.removeCallbacks(liveRefresh);
		}
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		if (controller != null) {
			controller.finishProcessIfNeeded(getActivity());
		}
		super.onDestroy();
	}

	@NonNull
	private SunLightMode getMode(@IdRes int buttonId) {
		if (buttonId == R.id.mode_live) {
			return SunLightMode.LIVE;
		} else if (buttonId == R.id.mode_date) {
			return SunLightMode.DATE;
		}
		return SunLightMode.MANUAL;
	}

	@IdRes
	private int getButtonId(@NonNull SunLightMode mode) {
		switch (mode) {
			case LIVE:
				return R.id.mode_live;
			case DATE:
				return R.id.mode_date;
			default:
				return R.id.mode_manual;
		}
	}

	@NonNull
	private static ZonedDateTime getZonedTime(long millis) {
		return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
	}

	@NonNull
	private String formatMinutes(float minutes) {
		int value = Math.round(minutes);
		ZonedDateTime time = ZonedDateTime.now().with(LocalTime.of(value / 60, value % 60));
		return DateFormat.getTimeFormat(requireContext()).format(Date.from(time.toInstant()));
	}

	@NonNull
	private String formatAltitude(int value) {
		return String.format(Locale.getDefault(), "%d°", value);
	}

	@NonNull
	private String formatAzimuth(int value) {
		String cardinalDirection = RadiusRulerControlLayer.getCardinalDirectionForDegrees(value);
		return String.format(Locale.getDefault(), "%d°, %s", value, cardinalDirection);
	}

	public static boolean showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new SunParametersFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}

	@Override
	public String getAnalyticsScreen() {
		return "sun_parameters";
	}
}
