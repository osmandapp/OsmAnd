package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.ROUTE_PLANNING_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.OPAQUE_ALPHA;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.RECTANGULAR_RADIUS_DP;
import static net.osmand.shared.grid.ButtonPositionSize.POS_BOTTOM;
import static net.osmand.shared.grid.ButtonPositionSize.POS_LEFT;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.grid.ButtonPositionSize;

public class NavigationMenuButtonState extends MapButtonState {

	private final CommonPreference<Boolean> visibilityPref;

	public NavigationMenuButtonState(@NonNull OsmandApplication app) {
		super(app, ROUTE_PLANNING_HUD_ID);
		this.visibilityPref = addPreference(settings.registerBooleanPreference(id + "_state", true)).makeProfile();
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.shared_string_navigation);
	}

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.navigation_action_descr);
	}

	@Override
	public boolean isEnabled() {
		return visibilityPref.get();
	}

	@NonNull
	@Override
	public CommonPreference<Boolean> getVisibilityPref() {
		return visibilityPref;
	}

	@Override
	public int getDefaultLayoutId() {
		return R.layout.navigation_menu_button;
	}

	@NonNull
	@Override
	public String getDefaultIconName() {
		int routePlanningBtnImage = app.getRoutingHelper().getAppMode().getIconRes();
		boolean isFollowingMode = app.getRoutingHelper().isFollowingMode();
		if (routePlanningBtnImage != 0 && app.getRoutingHelper().isRoutePlanningMode() && !isFollowingMode) {
			return app.getResources().getResourceEntryName(routePlanningBtnImage);
		}

		return isFollowingMode ? "ic_action_start_navigation" : "ic_action_gdirections_dark";
	}

	@Override
	public float getDefaultOpacity() {
		return OPAQUE_ALPHA;
	}

	@Override
	public int getDefaultCornerRadius() {
		return RECTANGULAR_RADIUS_DP;
	}

	@Nullable
	@Override
	public Drawable getIcon(@DrawableRes int iconId, @ColorInt int color, boolean nightMode, boolean mapIcon) {
		Drawable drawable = super.getIcon(iconId, color, nightMode, mapIcon);
		if (mapIcon && drawable != null) {
			return AndroidUtils.getDrawableForDirection(app, drawable);
		}
		return drawable;
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		return setupButtonPosition(position, POS_LEFT, POS_BOTTOM, true, false);
	}
}