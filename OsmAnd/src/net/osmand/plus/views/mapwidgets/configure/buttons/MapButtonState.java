package net.osmand.plus.views.mapwidgets.configure.buttons;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.UiUtilities;

public abstract class MapButtonState {

	protected final OsmandApplication app;
	protected final OsmandSettings settings;
	protected final UiUtilities uiUtilities;

	protected final String id;

	public MapButtonState(@NonNull OsmandApplication app, @NonNull String id) {
		this.id = id;
		this.app = app;
		this.settings = app.getSettings();
		this.uiUtilities = app.getUIUtilities();
	}

	@NonNull
	public String getId() {
		return id;
	}

	@NonNull
	public abstract String getName();

	public boolean isEnabled() {
		return true;
	}

	@Nullable
	public Drawable getIcon(boolean nightMode, boolean mapIcon, @ColorInt int colorId) {
		return uiUtilities.getPaintedIcon(R.drawable.ic_quick_action, colorId);
	}
}