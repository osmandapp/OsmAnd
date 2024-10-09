package net.osmand.plus.views.mapwidgets.configure.buttons;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.FabMarginPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public abstract class MapButtonState {

	protected final OsmandApplication app;
	protected final OsmandSettings settings;
	protected final UiUtilities uiUtilities;

	protected final String id;
	protected final List<CommonPreference<?>> allPreferences;
	protected final CommonPreference<String> iconPref;
	protected final CommonPreference<Integer> sizePref;
	protected final CommonPreference<Float> opacityPref;
	protected final CommonPreference<Integer> cornerRadiusPref;

	public MapButtonState(@NonNull OsmandApplication app, @NonNull String id) {
		this.id = id;
		this.app = app;
		this.settings = app.getSettings();
		this.uiUtilities = app.getUIUtilities();
		this.allPreferences = new ArrayList<>();

		this.iconPref = addPreference(settings.registerStringPreference(id + "_icon", null)).makeProfile().cache();
		this.sizePref = addPreference(settings.registerIntPreference(id + "_size", -1)).makeProfile().cache();
		this.opacityPref = addPreference(settings.registerFloatPreference(id + "_opacity", -1)).makeProfile().cache();
		this.cornerRadiusPref = addPreference(settings.registerIntPreference(id + "_corner_radius", -1)).makeProfile().cache();
	}

	@NonNull
	public String getId() {
		return id;
	}

	@NonNull
	public abstract String getName();

	@NonNull
	public abstract String getDescription();

	public abstract boolean isEnabled();

	@LayoutRes
	public abstract int getDefaultLayoutId();

	@NonNull
	public abstract ButtonAppearanceParams createDefaultAppearanceParams();

	@NonNull
	public CommonPreference<String> getIconPref() {
		return iconPref;
	}

	@NonNull
	public CommonPreference<Integer> getSizePref() {
		return sizePref;
	}

	@NonNull
	public CommonPreference<Float> getOpacityPref() {
		return opacityPref;
	}

	@NonNull
	public CommonPreference<Integer> getCornerRadiusPref() {
		return cornerRadiusPref;
	}

	@NonNull
	public abstract CommonPreference getVisibilityPref();

	@Nullable
	public FabMarginPreference getFabMarginPref() {
		return null;
	}

	@NonNull
	public ButtonAppearanceParams createAppearanceParams() {
		ButtonAppearanceParams defaultParams = createDefaultAppearanceParams();

		String iconName = iconPref.get();
		if (Algorithms.isEmpty(iconName)) {
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

	@Nullable
	public Drawable getIcon(@ColorInt int color, boolean nightMode, boolean mapIcon) {
		String iconName = createAppearanceParams().getIconName();
		int iconId = AndroidUtils.getDrawableId(app, iconName);
		if (iconId == 0) {
			iconId = RenderingIcons.getBigIconResourceId(iconName);
		}
		return iconId != 0 ? getIcon(iconId, color, nightMode, mapIcon) : null;
	}

	@Nullable
	public Drawable getIcon(@DrawableRes int iconId, @ColorInt int color, boolean nightMode, boolean mapIcon) {
		return color != 0 ? uiUtilities.getPaintedIcon(iconId, color) : uiUtilities.getIcon(iconId);
	}

	public void resetToDefault(@NonNull ApplicationMode appMode) {
		iconPref.resetModeToDefault(appMode);
		sizePref.resetModeToDefault(appMode);
		opacityPref.resetModeToDefault(appMode);
		cornerRadiusPref.resetModeToDefault(appMode);
		getVisibilityPref().resetModeToDefault(appMode);
	}

	public void copyForMode(@NonNull ApplicationMode fromMode, @NonNull ApplicationMode toMode) {
		iconPref.setModeValue(toMode, iconPref.getModeValue(fromMode));
		sizePref.setModeValue(toMode, sizePref.getModeValue(fromMode));
		opacityPref.setModeValue(toMode, opacityPref.getModeValue(fromMode));
		cornerRadiusPref.setModeValue(toMode, cornerRadiusPref.getModeValue(fromMode));
		getVisibilityPref().setModeValue(toMode, getVisibilityPref().getModeValue(fromMode));
	}

	public void onButtonStateRemoved() {
		settings.removePreferences(allPreferences);
	}

	@NonNull
	protected <T> CommonPreference<T> addPreference(@NonNull CommonPreference<T> preference) {
		allPreferences.add(preference);
		return preference;
	}

	@NonNull
	protected FabMarginPreference addPreference(@NonNull FabMarginPreference fabMarginPreference) {
		allPreferences.addAll(fabMarginPreference.getInternalPrefs());
		return fabMarginPreference;
	}

	public boolean hasCustomAppearance() {
		return !Algorithms.objectEquals(createAppearanceParams(), createDefaultAppearanceParams());
	}

	@NonNull
	@Override
	public String toString() {
		return getId();
	}
}