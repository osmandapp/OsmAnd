package net.osmand.plus.plugins.srtm.building;

import static net.osmand.plus.plugins.srtm.SRTMPlugin.DEFAULT_HILLSHADE_SUN_ANGLE;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.DEFAULT_HILLSHADE_SUN_AZIMUTH;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapOptionFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Light of the hillshade and the sun shadows: the real sun now (Live), the real sun at a chosen
 * date and time (Date), or azimuth and altitude set by hand (Manual).
 * <p>
 * A panel like Terrain - Visibility: the map shows the light while it is changed, Apply keeps it,
 * leaving the panel without Apply brings the previous light back.
 */
public class SunParametersFragment extends ConfigureMapOptionFragment {

	private static final String DATE_PICKER_TAG = "sun_date_picker";
	private static final long LIVE_REFRESH_INTERVAL_MS = 60_000L;
	private static final int MINUTES_PER_DAY = 24 * 60;

	private SunParametersController controller;

	private View content;
	private TextToggleButton modeToggle;
	private final Map<SunLightMode, TextRadioItem> modeItems = new EnumMap<>(SunLightMode.class);

	private View liveRows;
	private TextView liveAzimuthValue;
	private TextView liveAltitudeValue;
	private TextView liveTimeValue;

	private View dateRows;
	private TextView dateValue;
	private TextView timeValue;
	private Slider timeSlider;
	private TextView dateAnglesValue;

	private View manualRows;
	private TextView azimuthValue;
	private Slider azimuthSlider;
	private TextView altitudeValue;
	private Slider altitudeSlider;

	/* Live shows the sun and the date and time of the current minute; the next refresh comes
	   when the minute changes, so the time row follows the clock */
	private final Runnable liveRefresh = new Runnable() {
		@Override
		public void run() {
			if (content != null && isAdded() && controller.getMode() == SunLightMode.LIVE) {
				updateLiveRows();
				content.postDelayed(this, LIVE_REFRESH_INTERVAL_MS - System.currentTimeMillis() % LIVE_REFRESH_INTERVAL_MS);
			}
		}
	};

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.map_widget_sun_position);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		controller = SunParametersController.getExistedInstance(app);
		if (controller == null) {
			closeWithoutController(savedInstanceState);
			return;
		}
		saveOriginalValues(savedInstanceState, controller.getPreviewPreferences());
		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				activity.getSupportFragmentManager().popBackStack();
				activity.getDashboard().setDashboardVisibility(true, controller.getReturnTo(), false);
			}
		});
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		if (controller == null) {
			return;
		}
		content = inflate(R.layout.sun_parameters, container, false);

		liveRows = content.findViewById(R.id.live_rows);
		liveAzimuthValue = content.findViewById(R.id.live_azimuth_value);
		liveAltitudeValue = content.findViewById(R.id.live_altitude_value);
		liveTimeValue = content.findViewById(R.id.live_time_value);

		dateRows = content.findViewById(R.id.date_rows);
		dateValue = content.findViewById(R.id.date_value);
		timeValue = content.findViewById(R.id.time_value);
		timeSlider = content.findViewById(R.id.time_slider);
		dateAnglesValue = content.findViewById(R.id.date_angles_value);
		/* a landscape phone has no height for the time row and the angles caption, the panel stays as without them */
		if (!AndroidUiHelper.isPortrait(requireActivity()) && !AndroidUiHelper.isTablet(requireActivity())) {
			AndroidUiHelper.updateVisibility(content.findViewById(R.id.live_time_row), false);
			AndroidUiHelper.updateVisibility(dateAnglesValue, false);
		}

		manualRows = content.findViewById(R.id.manual_rows);
		azimuthValue = content.findViewById(R.id.azimuth_value_tv);
		azimuthSlider = content.findViewById(R.id.azimuth_slider);
		altitudeValue = content.findViewById(R.id.altitude_value_tv);
		altitudeSlider = content.findViewById(R.id.altitude_slider);

		setupModeToggle();
		setupSliders();
		content.findViewById(R.id.date_row).setOnClickListener(v -> showDatePicker());
		restoreDatePicker();

		container.addView(content);
		updateContent();
		updateApplyButton(controller.hasChanges());
	}

	private void setupModeToggle() {
		modeToggle = new TextToggleButton(app, content.findViewById(R.id.custom_radio_buttons), nightMode);
		List<TextRadioItem> items = new ArrayList<>();
		for (SunLightMode mode : SunLightMode.values()) {
			TextRadioItem item = new TextRadioItem(getString(mode.getTitleId()));
			item.setOnClickListener((radioItem, view) -> {
				if (mode != controller.getMode()) {
					controller.setMode(mode);
					if (mode == SunLightMode.DATE) {
						/* keep the shown moment, an unset time would follow the clock like Live */
						controller.setTime(controller.getTime());
					}
					updateContent();
					onChanged();
				}
				return true;
			});
			modeItems.put(mode, item);
			items.add(item);
		}
		modeToggle.setItems(items);
	}

	private void setupSliders() {
		int profileColor = getAppModeColor(nightMode);

		UiUtilities.setupSlider(timeSlider, nightMode, profileColor);
		timeSlider.setValueFrom(0);
		timeSlider.setValueTo(MINUTES_PER_DAY - 1);
		timeSlider.addOnChangeListener((slider, value, fromUser) -> {
			if (fromUser) {
				ZonedDateTime current = getZonedTime(controller.getTime());
				int minutes = Math.round(value);
				ZonedDateTime changed = current.with(LocalTime.of(minutes / 60, minutes % 60));
				controller.setTime(changed.toInstant().toEpochMilli());
				timeValue.setText(formatMinutes(value));
				updateDateAngles();
				onChanged();
			}
		});

		UiUtilities.setupSlider(azimuthSlider, nightMode, profileColor);
		azimuthSlider.setValueFrom(controller.getMinAzimuth());
		azimuthSlider.setValueTo(controller.getMaxAzimuth());
		azimuthSlider.addOnChangeListener((slider, value, fromUser) -> {
			if (fromUser) {
				int azimuth = Math.round(value);
				controller.setAzimuth(azimuth);
				azimuthValue.setText(formatAzimuth(azimuth));
				onChanged();
			}
		});

		UiUtilities.setupSlider(altitudeSlider, nightMode, profileColor);
		altitudeSlider.setValueFrom(controller.getMinAltitude());
		altitudeSlider.setValueTo(controller.getMaxAltitude());
		altitudeSlider.addOnChangeListener((slider, value, fromUser) -> {
			if (fromUser) {
				int altitude = Math.round(value);
				controller.setAltitude(altitude);
				altitudeValue.setText(formatAltitude(altitude));
				onChanged();
			}
		});
	}

	/** Shows the rows of the selected mode and puts the values on them. */
	private void updateContent() {
		SunLightMode mode = controller.getMode();
		modeToggle.setSelectedItem(modeItems.get(mode));
		showRows(mode);

		content.removeCallbacks(liveRefresh);
		if (mode == SunLightMode.LIVE) {
			liveRefresh.run();
		} else if (mode == SunLightMode.DATE) {
			bindDate();
		} else {
			bindManual();
		}
	}

	/* the rows of the other modes stay laid out, so the panel and the toggle do not move */
	private void showRows(@NonNull SunLightMode mode) {
		liveRows.setVisibility(mode == SunLightMode.LIVE ? View.VISIBLE : View.INVISIBLE);
		dateRows.setVisibility(mode == SunLightMode.DATE ? View.VISIBLE : View.INVISIBLE);
		manualRows.setVisibility(mode == SunLightMode.MANUAL ? View.VISIBLE : View.INVISIBLE);
	}

	private void updateLiveRows() {
		SunLight.Angles angles = controller.getSunAngles();
		liveAzimuthValue.setText(formatSunAzimuth(angles));
		liveAltitudeValue.setText(formatSunAltitude(angles));
		Date now = new Date();
		liveTimeValue.setText(getString(R.string.ltr_or_rtl_combine_via_comma,
				OsmAndFormatter.getDateFormat(requireContext()).format(now), DateFormat.getTimeFormat(requireContext()).format(now)));
	}

	private void bindDate() {
		ZonedDateTime time = getZonedTime(controller.getTime());
		dateValue.setText(DateFormat.getLongDateFormat(requireContext()).format(Date.from(time.toInstant())));
		int minutes = time.getHour() * 60 + time.getMinute();
		timeSlider.setValue(minutes);
		timeValue.setText(formatMinutes(minutes));
		updateDateAngles();
	}

	/* one line under the time slider: the sun at the chosen date and time */
	private void updateDateAngles() {
		SunLight.Angles angles = controller.getSunAngles();
		String azimuth = getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.shared_string_azimuth), formatSunAzimuth(angles));
		String altitude = getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.altitude), formatSunAltitude(angles));
		dateAnglesValue.setText(getString(R.string.ltr_or_rtl_combine_via_bold_point, azimuth, altitude));
	}

	private void bindManual() {
		int azimuth = controller.getValidAzimuth();
		azimuthSlider.setValue(azimuth);
		azimuthValue.setText(formatAzimuth(azimuth));
		int altitude = controller.getValidAltitude();
		altitudeSlider.setValue(altitude);
		altitudeValue.setText(formatAltitude(altitude));
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
	private String formatSunAzimuth(@NonNull SunLight.Angles angles) {
		return formatAzimuth((int) Math.round(angles.getAzimuth()) % 360);
	}

	@NonNull
	private String formatSunAltitude(@NonNull SunLight.Angles angles) {
		return formatAltitude((int) Math.round(angles.getAltitude()));
	}

	@NonNull
	private String formatAltitude(int value) {
		return String.format(Locale.getDefault(), "%d°", value);
	}

	@NonNull
	private String formatAzimuth(int value) {
		String cardinalDirection = OsmAndFormatter.getLocalizedCardinalDirection(requireContext(), value);
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
