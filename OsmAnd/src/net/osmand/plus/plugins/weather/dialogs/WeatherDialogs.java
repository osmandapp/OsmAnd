package net.osmand.plus.plugins.weather.dialogs;

import android.content.Context;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.weather.OfflineForecastHelper;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

import java.util.List;

public class WeatherDialogs {

	public static void showChooseUnitDialog(@NonNull Context ctx, @NonNull WeatherBand band,
	                                        int selected, int profileColor, boolean nightMode,
	                                        @NonNull OnClickListener listener) {
		List<? extends WeatherUnit> bandUnits = band.getAvailableBandUnits();
		String[] entries = new String[bandUnits.size()];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = bandUnits.get(i).toHumanString(ctx);
		}
		AlertDialogData dialogData = new AlertDialogData(ctx, nightMode)
				.setTitle(band.getMeasurementName())
				.setControlsColor(profileColor);
		CustomAlert.showSingleSelection(dialogData, entries, selected, listener);
	}

	public static void showClearOnlineCacheDialog(@NonNull Context ctx, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		OfflineForecastHelper offlineForecastHelper = app.getOfflineForecastHelper();
		AlertDialogData dialogData = new AlertDialogData(ctx, nightMode)
				.setTitle(R.string.clear_online_cache)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_clear, (di, i) -> offlineForecastHelper.clearOnlineCacheAsync())
				.setPositiveButtonTextColor(ColorUtilities.getColor(ctx, R.color.color_osm_edit_delete));
		CustomAlert.showSimpleMessage(dialogData, R.string.clear_online_cache_description);
	}
}
