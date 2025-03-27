package net.osmand.plus.configmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.helpers.DayNightHelper.MapThemeProvider;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.util.Algorithms;
import net.osmand.util.SunriseSunset;

import java.text.DateFormat;
import java.util.Optional;

public class MapModeController extends BaseDialogController implements MapThemeProvider {

	public static final String PROCESS_ID = "select_map_mode";

	private final CommonPreference<DayNightMode> preference;
	private DayNightMode selectedTheme;

	public MapModeController(@NonNull OsmandApplication app) {
		super(app);
		OsmandSettings settings = app.getSettings();
		preference = settings.DAYNIGHT_MODE;
		selectedTheme = preference.get();
	}

	public void registerDialog(@NonNull IDialog dialog) {
		dialogManager.register(PROCESS_ID, dialog);
	}

	public void finishProcessIfNeeded(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			dialogManager.unregister(PROCESS_ID);
		}
	}

	@NonNull
	public String getHeaderSummary() {
		return selectedTheme.toHumanString(app);
	}

	@NonNull
	public String getDescription() {
		return getString(selectedTheme.getSummaryRes());
	}

	@Nullable
	public String getSecondaryDescription() {
		if (selectedTheme == DayNightMode.AUTO) {
			SunriseSunset sunriseSunset = app.getDaynightHelper().getSunriseSunset();
			if (sunriseSunset != null && sunriseSunset.getSunrise() != null && sunriseSunset.getSunset() != null) {
				DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
				String sunriseTime = dateFormat.format(sunriseSunset.getSunrise());
				String sunsetTime = dateFormat.format(sunriseSunset.getSunset());
				String sunriseTitle = getString(R.string.shared_string_sunrise);
				String sunsetTitle = getString(R.string.shared_string_sunset);
				sunriseTime = getString(R.string.ltr_or_rtl_combine_via_dash, sunriseTitle, sunriseTime);
				sunsetTime = getString(R.string.ltr_or_rtl_combine_via_dash, sunsetTitle, sunsetTime);
				String description = getString(R.string.ltr_or_rtl_combine_via_comma, sunriseTime, sunsetTime);
				return Algorithms.capitalizeFirstLetterAndLowercase(description);
			}
		}
		return null;
	}

	public void onResetToDefault() {
		selectedTheme = preference.getDefaultValue();
	}

	public void onApplyChanges() {
		preference.set(selectedTheme);
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void onResume() {
		setMapThemeProvider(this);
	}

	public void onPause() {
		setMapThemeProvider(null);
	}

	private void setMapThemeProvider(@Nullable MapThemeProvider provider) {
		DayNightHelper helper = app.getDaynightHelper();
		helper.setExternalMapThemeProvider(provider);
	}

	public boolean askSelectMapTheme(@NonNull DayNightMode theme) {
		if (theme != selectedTheme) {
			selectedTheme = theme;
			return true;
		}
		return false;
	}

	@Override
	@NonNull
	public DayNightMode getMapTheme() {
		return selectedTheme;
	}

	public static void registerNewInstance(final OsmandApplication app) {
		app.getDialogManager().register(PROCESS_ID, new MapModeController(app));
	}

	public static Optional<MapModeController> findRegisteredInstance(final OsmandApplication app) {
		return Optional.ofNullable((MapModeController) app.getDialogManager().findController(PROCESS_ID));
	}
}
