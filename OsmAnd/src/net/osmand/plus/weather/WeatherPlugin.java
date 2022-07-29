package net.osmand.plus.weather;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_WEATHER;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.plugins.OsmandPlugin;

public class WeatherPlugin extends OsmandPlugin {

	public WeatherPlugin(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
	public String getId() {
		return PLUGIN_WEATHER;
	}

	@Override
	public String getName() {
		return app.getString(R.string.shared_string_weather);
	}

	@Override
	public CharSequence getDescription() {
		return null;
	}

	@Override
	public boolean isPaid() {
		return true;
	}

	@Override
	public boolean isLocked() {
		return !Version.isPaidVersion(app);
	}
}
