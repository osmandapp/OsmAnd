package net.osmand.plus.plugins.weather.units;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;

public interface WeatherUnit {

	@NonNull
	String getUnit(@NonNull OsmandApplication app);

	@NonNull
	String getSymbol();

	String toHumanString(@NonNull Context ctx);
}