package net.osmand.plus.settings.fragments.profileappearance;

import static net.osmand.plus.utils.ColorUtilities.getColor;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.MarkerDisplayOption;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

class ApplicationProfileObject {
	String stringKey;
	ApplicationMode parent;
	String name;
	ProfileIconColors color;
	Integer customColor;
	int iconRes;
	String routingProfile;
	RouteService routeService;
	String navigationIcon;
	String locationIcon;
	MarkerDisplayOption viewAngleVisibility;
	MarkerDisplayOption locationRadiusVisibility;

	@ColorInt
	public int getActualColor(@NonNull Context context, boolean nightMode) {
		return customColor != null ? customColor : getColor(context, color.getColor(nightMode));
	}

	@Nullable
	public ProfileIconColors getProfileColorByColorValue(@NonNull Context context, int colorValue) {
		for (ProfileIconColors color : ProfileIconColors.values()) {
			if (ColorUtilities.getColor(context, color.getColor(true)) == colorValue
					|| ColorUtilities.getColor(context, color.getColor(false)) == colorValue) {
				return color;
			}
		}
		return null;
	}

	public void copyPropertiesFrom(@NonNull ApplicationMode sourceMode) {
		this.stringKey = sourceMode.getStringKey();
		this.parent = sourceMode.getParent();
		this.name = sourceMode.toHumanString();
		this.color = sourceMode.getIconColorInfo();
		this.customColor = sourceMode.getCustomIconColor();
		this.iconRes = sourceMode.getIconRes();
		this.routingProfile = sourceMode.getRoutingProfile();
		this.routeService = sourceMode.getRouteService();
		this.locationIcon = sourceMode.getLocationIcon();
		this.navigationIcon = sourceMode.getNavigationIcon();
		this.viewAngleVisibility = sourceMode.getViewAngleVisibility();
		this.locationRadiusVisibility = sourceMode.getLocationRadiusVisibility();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ApplicationProfileObject that = (ApplicationProfileObject) o;

		if (iconRes != that.iconRes) return false;
		if (stringKey != null ? !stringKey.equals(that.stringKey) : that.stringKey != null)
			return false;
		if (parent != null ? !parent.equals(that.parent) : that.parent != null) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (color != that.color) return false;
		if (viewAngleVisibility != that.viewAngleVisibility) return false;
		if (locationRadiusVisibility != that.locationRadiusVisibility) return false;
		if (customColor != null ? !customColor.equals(that.customColor) : that.customColor != null)
			return false;
		if (routingProfile != null ? !routingProfile.equals(that.routingProfile) : that.routingProfile != null)
			return false;
		if (routeService != that.routeService) return false;
		if (!Algorithms.objectEquals(navigationIcon, that.navigationIcon)) return false;
		return Algorithms.objectEquals(locationIcon, that.locationIcon);
	}

	@Override
	public int hashCode() {
		int result = stringKey != null ? stringKey.hashCode() : 0;
		result = 31 * result + (parent != null ? parent.hashCode() : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (color != null ? color.hashCode() : 0);
		result = 31 * result + (customColor != null ? customColor.hashCode() : 0);
		result = 31 * result + iconRes;
		result = 31 * result + (routingProfile != null ? routingProfile.hashCode() : 0);
		result = 31 * result + (routeService != null ? routeService.hashCode() : 0);
		result = 31 * result + (navigationIcon != null ? navigationIcon.hashCode() : 0);
		result = 31 * result + (locationIcon != null ? locationIcon.hashCode() : 0);
		result = 31 * result + (viewAngleVisibility != null ? viewAngleVisibility.hashCode() : 0);
		result = 31 * result + (locationRadiusVisibility != null ? locationRadiusVisibility.hashCode() : 0);
		return result;
	}
}
