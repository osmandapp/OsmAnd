package net.osmand.plus.plugins.weather.units;

import android.content.Context;

import androidx.annotation.NonNull;

public interface WeatherUnit {

	@NonNull
	String getSymbol();

	String toHumanString(@NonNull Context ctx);
}