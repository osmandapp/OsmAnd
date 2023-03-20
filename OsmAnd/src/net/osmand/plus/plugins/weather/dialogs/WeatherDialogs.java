package net.osmand.plus.plugins.weather.dialogs;

import android.content.Context;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.weather.WeatherBand;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.utils.ColorUtilities;

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
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		DialogListItemAdapter adapter = DialogListItemAdapter.createSingleChoiceAdapter(entries,
				nightMode, selected, app, profileColor, themeRes, listener);

		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(band.getMeasurementName());
		builder.setAdapter(adapter, null);
		adapter.setDialog(builder.show());
	}

	public static void showClearOnlineCacheDialog(@NonNull Context ctx) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.clear_online_cache);
		builder.setMessage(R.string.clear_online_cache_description);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_clear, (di, i) -> {
			app.getOfflineForecastHelper().clearOnlineCacheAsync();
		});
		AlertDialog dialog = builder.show();
		Button btnClear = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
		btnClear.setTextColor(ColorUtilities.getColor(ctx, R.color.color_osm_edit_delete));
	}

}
