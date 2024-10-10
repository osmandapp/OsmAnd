package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.COMPASS_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.SMALL_SIZE_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;
import static net.osmand.plus.settings.enums.CompassVisibility.ALWAYS_HIDDEN;
import static net.osmand.plus.settings.enums.CompassVisibility.ALWAYS_VISIBLE;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.EnumStringPreference;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.settings.enums.CompassVisibility;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.controls.maphudbuttons.CompassDrawable;
import net.osmand.util.Algorithms;

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

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.key_event_action_change_map_orientation);
	}

	@Override
	public int getDefaultLayoutId() {
		return R.layout.map_compass_button;
	}

	@Override
	public boolean isEnabled() {
		return getVisibility() != ALWAYS_HIDDEN;
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
			preference = addPreference(new EnumStringPreference<>(settings, "compass_visibility", ALWAYS_VISIBLE, CompassVisibility.values()) {

				@Override
				public CompassVisibility getModeValue(ApplicationMode mode) {
					CompassVisibility customizationValue = CompassVisibility.getFromCustomization(app, mode);
					return isSetForMode(mode) || customizationValue == null ? super.getModeValue(mode) : customizationValue;
				}
			}).makeProfile().cache();
		}
		return preference;
	}

	@NonNull
	@Override
	public ButtonAppearanceParams createAppearanceParams() {
		ButtonAppearanceParams defaultParams = createDefaultAppearanceParams();

		String iconName = iconPref.get();
		int iconId = AndroidUtils.getDrawableId(app, iconPref.get());
		if (Algorithms.isEmpty(iconName) || CompassMode.isCompassIconId(iconId)) {
			iconName = defaultParams.getIconName();
		}
		int size = sizePref.get();
		if (size <= 0) {
			size = defaultParams.getSize();
		}
		float opacity = opacityPref.get();
		if (opacity < 0) {
			opacity = defaultParams.getOpacity();
		}
		int cornerRadius = cornerRadiusPref.get();
		if (cornerRadius < 0) {
			cornerRadius = defaultParams.getCornerRadius();
		}
		return new ButtonAppearanceParams(iconName, size, opacity, cornerRadius);
	}

	@NonNull
	@Override
	public ButtonAppearanceParams createDefaultAppearanceParams() {
		CompassMode compassMode = settings.getCompassMode();
		boolean nightMode = app.getDaynightHelper().isNightMode();
		String iconName = app.getResources().getResourceEntryName(compassMode.getIconId().getIconId(nightMode));
		return new ButtonAppearanceParams(iconName, SMALL_SIZE_DP, TRANSPARENT_ALPHA, ROUND_RADIUS_DP);
	}

	@Nullable
	@Override
	public Drawable getIcon(@DrawableRes int iconId, @ColorInt int color, boolean nightMode, boolean mapIcon) {
		Drawable drawable = super.getIcon(iconId, color, nightMode, mapIcon);
		if (mapIcon && drawable != null) {
			return new CompassDrawable(drawable);
		}
		return drawable;
	}
}
