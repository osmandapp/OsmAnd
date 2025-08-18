package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.plus.quickaction.ButtonAppearanceParams.BIG_SIZE_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ORIGINAL_VALUE;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiContext;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.grid.ButtonPositionSize;
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
	protected final CommonPreference<Long> portraitPositionPref;
	protected final CommonPreference<Long> landscapePositionPref;
	protected final ButtonPositionSize positionSize;
	protected final ButtonPositionSize defaultPositionSize;

	private final StateChangedListener<Integer> sizeListener;

	private boolean portrait;

	public MapButtonState(@NonNull OsmandApplication app, @NonNull String id) {
		this.id = id;
		this.app = app;
		this.settings = app.getSettings();
		this.uiUtilities = app.getUIUtilities();
		this.allPreferences = new ArrayList<>();

		this.iconPref = addPreference(settings.registerStringPreference(id + "_icon", null)).makeProfile().cache();
		this.sizePref = addPreference(settings.registerIntPreference(id + "_size", ORIGINAL_VALUE)).makeProfile().cache();
		this.opacityPref = addPreference(settings.registerFloatPreference(id + "_opacity", ORIGINAL_VALUE)).makeProfile().cache();
		this.cornerRadiusPref = addPreference(settings.registerIntPreference(id + "_corner_radius", ORIGINAL_VALUE)).makeProfile().cache();
		this.portraitPositionPref = addPreference(settings.registerLongPreference(id + "_position_portrait", ORIGINAL_VALUE)).makeProfile().cache();
		this.landscapePositionPref = addPreference(settings.registerLongPreference(id + "_position_landscape", ORIGINAL_VALUE)).makeProfile().cache();
		this.positionSize = setupButtonPosition(new ButtonPositionSize(getId()));
		this.defaultPositionSize = setupButtonPosition(new ButtonPositionSize(getId()));

		sizeListener = change -> {
			updatePosition(positionSize);
			updatePosition(defaultPositionSize);
		};
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

	@NonNull
	public ButtonAppearanceParams createDefaultAppearanceParams() {
		MapButtonsHelper buttonsHelper = app.getMapButtonsHelper();
		int size = buttonsHelper.getDefaultSizePref().get();
		if (size <= 0) {
			size = getDefaultSize();
		}
		float opacity = buttonsHelper.getDefaultOpacityPref().get();
		if (opacity < 0) {
			opacity = getDefaultOpacity();
		}
		int cornerRadius = buttonsHelper.getDefaultCornerRadiusPref().get();
		if (cornerRadius < 0) {
			cornerRadius = getDefaultCornerRadius();
		}
		return new ButtonAppearanceParams(getDefaultIconName(), size, opacity, cornerRadius);
	}

	@LayoutRes
	public abstract int getDefaultLayoutId();

	@NonNull
	public abstract String getDefaultIconName();

	public int getDefaultSize() {
		return BIG_SIZE_DP;
	}

	public float getDefaultOpacity() {
		return TRANSPARENT_ALPHA;
	}

	public int getDefaultCornerRadius() {
		return ROUND_RADIUS_DP;
	}

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
	public ButtonPositionSize getPositionSize() {
		return positionSize;
	}

	@NonNull
	public ButtonPositionSize getDefaultPositionSize() {
		ButtonPositionSize position = setupButtonPosition(defaultPositionSize);
		updatePosition(position);
		return position;
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
	protected abstract ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position);

	@NonNull
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position,
	                                                 int posH, int posV, boolean xMove, boolean yMove) {
		position.setPosH(posH);
		position.setPosV(posV);
		position.setXMove(xMove);
		position.setYMove(yMove);
		position.setMarginX(0);
		position.setMarginY(0);

		return position;
	}

	public void updatePositions(@NonNull @UiContext Context context) {
		this.portrait = AndroidUiHelper.isOrientationPortrait(context);

		updatePosition(positionSize);
		updatePosition(defaultPositionSize);
	}

	public void savePosition() {
		ButtonPositionSize positionSize = getPositionSize();
		CommonPreference<Long> preference = portrait ? portraitPositionPref : landscapePositionPref;
		preference.set(positionSize.toLongValue());
	}

	private void updatePosition(@NonNull ButtonPositionSize position) {
		CommonPreference<Long> preference = portrait ? portraitPositionPref : landscapePositionPref;
		Long value = preference.get();
		if (value != null && value > 0) {
			position.fromLongValue(value);
		}
		int size = createAppearanceParams().getSize();
		size = (size / 8) + 1;
		position.setSize(size, size);
	}

	@Nullable
	public Drawable getIcon(@ColorInt int color, boolean nightMode, boolean mapIcon) {
		int iconId = getIconId();
		return iconId != 0 ? getIcon(iconId, color, nightMode, mapIcon) : null;
	}

	@DrawableRes
	public int getIconId() {
		String iconName = createAppearanceParams().getIconName();
		int iconId = AndroidUtils.getDrawableId(app, iconName);
		return iconId != 0 ? iconId : RenderingIcons.getBigIconResourceId(iconName);
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
		portraitPositionPref.resetModeToDefault(appMode);
		landscapePositionPref.resetModeToDefault(appMode);
		getVisibilityPref().resetModeToDefault(appMode);
	}

	public void copyForMode(@NonNull ApplicationMode fromMode, @NonNull ApplicationMode toMode) {
		iconPref.setModeValue(toMode, iconPref.getModeValue(fromMode));
		sizePref.setModeValue(toMode, sizePref.getModeValue(fromMode));
		opacityPref.setModeValue(toMode, opacityPref.getModeValue(fromMode));
		cornerRadiusPref.setModeValue(toMode, cornerRadiusPref.getModeValue(fromMode));
		portraitPositionPref.setModeValue(toMode, portraitPositionPref.getModeValue(fromMode));
		landscapePositionPref.setModeValue(toMode, landscapePositionPref.getModeValue(fromMode));
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