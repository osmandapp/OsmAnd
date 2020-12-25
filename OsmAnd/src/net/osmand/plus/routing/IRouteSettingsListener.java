package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.ApplicationMode;

public interface IRouteSettingsListener {

	void onRouteSettingsChanged(@Nullable ApplicationMode mode);

}
