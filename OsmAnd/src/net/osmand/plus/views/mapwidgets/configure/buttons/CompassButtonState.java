package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.COMPASS_HUD_ID;
import static net.osmand.plus.settings.enums.CompassVisibility.ALWAYS_HIDDEN;
import static net.osmand.plus.settings.enums.CompassVisibility.VISIBLE_IF_MAP_ROTATED;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.settings.enums.CompassVisibility;

public class CompassButtonState extends MapButtonState {

	private final CommonPreference<CompassVisibility> visibilityPref;

	public CompassButtonState(@NonNull OsmandApplication app) {
		super(app, COMPASS_HUD_ID);
		visibilityPref = createVisibilityPref();
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.map_widget_compass);
	}

	@Override
	public boolean isEnabled() {
		return getVisibility() != ALWAYS_HIDDEN;
	}

	@Nullable
	@Override
	public Drawable getIcon(boolean nightMode, boolean mapIcon, @ColorInt int colorId) {
		return uiUtilities.getPaintedIcon(getVisibility().getIconId(), colorId);
	}

	@NonNull
	public CompassVisibility getVisibility() {
		return visibilityPref.get();
	}

	@NonNull
	public CompassVisibility getModeVisibility(@NonNull ApplicationMode mode) {
		return visibilityPref.getModeValue(mode);
	}

	public void setVisibility(@NonNull CompassVisibility visibility, @NonNull ApplicationMode mode) {
		visibilityPref.setModeValue(mode, visibility);
	}

	@NonNull
	public CommonPreference<CompassVisibility> getVisibilityPref() {
		return visibilityPref;
	}

	@NonNull
	private CommonPreference<CompassVisibility> createVisibilityPref() {
		CommonPreference<CompassVisibility> preference = (CommonPreference<CompassVisibility>) settings.getPreference("compass_visibility");
		if (preference == null) {
			preference = new EnumStringPreference<CompassVisibility>(settings, "compass_visibility", VISIBLE_IF_MAP_ROTATED, CompassVisibility.values()) {

				@Override
				public CompassVisibility getModeValue(ApplicationMode mode) {
					CompassVisibility customizationValue = CompassVisibility.getFromCustomization(app, mode);
					return isSetForMode(mode) || customizationValue == null ? super.getModeValue(mode) : customizationValue;
				}
			}.makeProfile().cache();
		}
		return preference;
	}
}
