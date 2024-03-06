package net.osmand.plus.card.color;

import static net.osmand.router.RouteStatisticsHelper.ROUTE_INFO_PREFIX;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.Objects;

/**
 * The coloring style can be determined by its coloring type.
 * In the cases when we use ATTRIBUTE coloring type, we also take into account
 * the name of the routing info attribute.
 */
public class ColoringStyle {

	private final ColoringType coloringType;
	private final String routeInfoAttribute;

	public ColoringStyle(@NonNull ColoringType coloringType) {
		this(coloringType, null);
	}

	public ColoringStyle(@NonNull String routeInfoAttribute) {
		this(ColoringType.ATTRIBUTE, routeInfoAttribute);
	}

	public ColoringStyle(@NonNull ColoringType coloringType, @Nullable String routeInfoAttribute) {
		this.coloringType = coloringType;
		this.routeInfoAttribute = routeInfoAttribute;
	}

	@Nullable
	public String getId() {
		if (coloringType.isRouteInfoAttribute()) {
			return Algorithms.isEmpty(routeInfoAttribute) ? null : routeInfoAttribute;
		}
		return coloringType.getId();
	}

	@NonNull
	public ColoringType getType() {
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
		if (o instanceof ColoringStyle) {
			ColoringStyle that = (ColoringStyle) o;
			return Objects.equals(getId(), that.getId());
		}
		return false;
	}

	@Override
	public int hashCode() {
		String id = getId();
		return id != null ? id.hashCode() : 0;
	}
}
