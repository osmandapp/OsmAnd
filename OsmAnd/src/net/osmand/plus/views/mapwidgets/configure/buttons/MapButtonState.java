package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.plus.quickaction.ButtonAppearanceParams.BIG_SIZE_DP;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.maphudbuttons.ButtonPositionSize;
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
	protected final CommonPreference<Long> positionPref;
	protected final ButtonPositionSize positionSize;

	private final StateChangedListener<Integer> sizeListener;

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
		this.positionPref = addPreference(settings.registerLongPreference(id + "_position", -1)).makeProfile().cache();
		this.positionSize = createButtonPosition();

		sizeListener = change -> updatePositionSize(positionSize);
		sizePref.addListener(sizeListener);
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

	public int getDefaultSize() {
		return BIG_SIZE_DP;
	}

	@NonNull
	public abstract ButtonAppearanceParams createDefaultAppearanceParams();

	@Nullable
	public String getSavedIconName() {
		return iconPref.get();
	}

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

	@NonNull
	public CommonPreference<Long> getPositionPref() {
		return positionPref;
	}

	@NonNull
	public ButtonPositionSize getPositionSize() {
		return positionSize;
	}

	@NonNull
	public ButtonAppearanceParams createAppearanceParams() {
		ButtonAppearanceParams defaultParams = createDefaultAppearanceParams();

		String iconName = getSavedIconName();
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

	@NonNull
	protected ButtonPositionSize createButtonPosition() {
		ButtonPositionSize position = new ButtonPositionSize(getId());

		Long value = positionPref.get();
		if (value != null && value > 0) {
			position.fromLongValue(value);
		}
		updatePositionSize(position);

		return position;
	}

	protected void setupButtonPosition(boolean left, boolean top, boolean xMove, boolean yMove, boolean randomMove) {
		positionSize.left = left;
		positionSize.top = top;
		positionSize.xMove = xMove;
		positionSize.yMove = yMove;
		positionSize.randomMove = randomMove;
	}

	private void updatePositionSize(@NonNull ButtonPositionSize position) {
		int size = sizePref.get();
		if (size <= 0) {
			size = getDefaultSize();
		}
		size = (size / 8) + 1;
		position.setSize(size, size);
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
		positionPref.resetModeToDefault(appMode);
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

	public boolean hasCustomAppearance() {
		return !Algorithms.objectEquals(createAppearanceParams(), createDefaultAppearanceParams());
	}

	@NonNull
	@Override
	public String toString() {
		return getId();
	}
}