package net.osmand.plus.card.color;

import static net.osmand.router.RouteStatisticsHelper.ROUTE_INFO_PREFIX;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.utils.AndroidUtils;

import java.util.Objects;

public class ColoringInfo {

	private final ColoringType coloringType;
	private final String routeInfoAttribute;

	public ColoringInfo(@NonNull ColoringType coloringType) {
		this(coloringType, null);
	}

	public ColoringInfo(@NonNull ColoringType coloringType, @Nullable String routeInfoAttribute) {
		this.coloringType = coloringType;
		this.routeInfoAttribute = routeInfoAttribute;
	}

	@NonNull
	public ColoringType getColoringType() {
		return coloringType;
	}

	@Nullable
	public String getRouteInfoAttribute() {
		return routeInfoAttribute;
	}

	@NonNull
	public String toHumanString(@NonNull Context context) {
		return coloringType == ColoringType.ATTRIBUTE
				? getRouteInfoAttributeName(context)
				: context.getString(coloringType.getTitleId());
	}

	@NonNull
	private String getRouteInfoAttributeName(@NonNull Context context) {
		if (routeInfoAttribute != null && routeInfoAttribute.startsWith(ROUTE_INFO_PREFIX)) {
			String attr = routeInfoAttribute.replace(ROUTE_INFO_PREFIX, "");
			return AndroidUtils.getStringRouteInfoPropertyValue(context, attr);
		}
		return "";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ColoringInfo)) return false;

		ColoringInfo that = (ColoringInfo) o;
		if (coloringType != that.coloringType) return false;
		return Objects.equals(routeInfoAttribute, that.routeInfoAttribute);
	}

	@Override
	public int hashCode() {
		int result = coloringType != null ? coloringType.hashCode() : 0;
		result = 31 * result + (routeInfoAttribute != null ? routeInfoAttribute.hashCode() : 0);
		return result;
	}
}
