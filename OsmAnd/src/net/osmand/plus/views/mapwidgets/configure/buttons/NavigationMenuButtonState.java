package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.ROUTE_PLANNING_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.BIG_SIZE_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.OPAQUE_ALPHA;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.RECTANGULAR_RADIUS_DP;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;

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
	public ButtonAppearanceParams createDefaultAppearanceParams() {
		String iconName;
		int routePlanningBtnImage = MapActivity.getMapRouteInfoMenu().getRoutePlanningBtnImage();
		if (routePlanningBtnImage != 0) {
			iconName = app.getResources().getResourceEntryName(routePlanningBtnImage);
		} else if (app.getRoutingHelper().isFollowingMode()) {
			iconName = "ic_action_start_navigation";
		} else {
			iconName = "ic_action_gdirections_dark";
		}
		return new ButtonAppearanceParams(iconName, BIG_SIZE_DP, OPAQUE_ALPHA, RECTANGULAR_RADIUS_DP);
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
}