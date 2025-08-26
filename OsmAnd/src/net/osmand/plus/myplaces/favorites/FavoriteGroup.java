package net.osmand.plus.myplaces.favorites;

import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.R;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FavoriteGroup {

	public static final String PERSONAL_CATEGORY = "personal";

	private String name;
	private String iconName;
	private BackgroundType backgroundType;
	private List<FavouritePoint> points = new ArrayList<>();

	private int color;
	private boolean visible = true;

	public FavoriteGroup() {
	}

	public FavoriteGroup(@NonNull FavouritePoint point) {
		name = point.getCategory();
		color = point.getColor();
		visible = point.isVisible();
		iconName = point.getIconName();
		backgroundType = point.getBackgroundType();
	}

	public FavoriteGroup(String name, List<FavouritePoint> points, int color, boolean visible) {
		this.name = name;
		this.color = color;
		this.points = points;
		this.visible = visible;
	}

	public FavoriteGroup(@NonNull FavoriteGroup group) {
		name = group.name;
		color = group.color;
		visible = group.visible;
		iconName = group.iconName;
		backgroundType = group.backgroundType;
		points.addAll(group.getPoints());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public String getIconName() {
		return iconName;
	}

	public void setIconName(String iconName) {
		this.iconName = iconName;
	}

	public BackgroundType getBackgroundType() {
		return backgroundType == null ? DEFAULT_BACKGROUND_TYPE : backgroundType;
	}

	public void setBackgroundType(BackgroundType backgroundType) {
		this.backgroundType = backgroundType;
	}

	public List<FavouritePoint> getPoints() {
		return points;
	}

	public void setPoints(List<FavouritePoint> points) {
		this.points = points;
	}

	public boolean isPersonal() {
		return isPersonal(name);
	}

	public String getDisplayName(@NonNull Context ctx) {
		return getDisplayName(ctx, name);
	}

	public static String getDisplayName(@NonNull Context ctx, String name) {
		if (isPersonal(name)) {
			return ctx.getString(R.string.personal_category_name);
		} else if (name.isEmpty()) {
			return ctx.getString(R.string.shared_string_favorites);
		} else {
			return name;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FavoriteGroup that = (FavoriteGroup) o;
		return Algorithms.stringsEqual(name, that.name)
				&& appearanceEquals(that)
				&& points.equals(that.points);
	}

	public boolean appearanceEquals(@NonNull FavoriteGroup group) {
		return (color == group.color)
				&& (backgroundType == group.backgroundType)
				&& Algorithms.stringsEqual(iconName, group.iconName)
				&& visible == group.isVisible();
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, iconName, backgroundType, points.size(), color, visible);
	}

	public void copyAppearance(@NonNull FavoriteGroup group) {
		setColor(group.getColor());
		setIconName(group.getIconName());
		setBackgroundType(group.getBackgroundType());
		setVisible(group.isVisible());
	}

	private static boolean isPersonal(@NonNull String name) {
		return PERSONAL_CATEGORY.equals(name);
	}

	public static boolean isPersonalCategoryDisplayName(@NonNull Context ctx, @NonNull String name) {
		return name.equals(ctx.getString(R.string.personal_category_name));
	}

	public static String convertDisplayNameToGroupIdName(@NonNull Context context, @NonNull String name) {
		if (isPersonalCategoryDisplayName(context, name)) {
			return PERSONAL_CATEGORY;
		}
		if (name.equals(context.getString(R.string.shared_string_favorites))) {
			return "";
		}
		return name;
	}

	public PointsGroup toPointsGroup(@NonNull Context ctx) {
		PointsGroup pointsGroup = new PointsGroup(getName(), getIconName(), getBackgroundType().getTypeName(), getColor(), !isVisible());
		List<FavouritePoint> points = new ArrayList<>(this.points);
		for (FavouritePoint point : points) {
			pointsGroup.getPoints().add(point.toWpt(ctx));
		}
		return pointsGroup;
	}

	public static FavoriteGroup fromPointsGroup(@NonNull PointsGroup pointsGroup) {
		FavoriteGroup favoriteGroup = new FavoriteGroup();
		favoriteGroup.name = pointsGroup.getName();
		favoriteGroup.color = pointsGroup.getColor();
		favoriteGroup.iconName = pointsGroup.getIconName();
		favoriteGroup.backgroundType = BackgroundType.getByTypeName(pointsGroup.getBackgroundType(), DEFAULT_BACKGROUND_TYPE);

		for (WptPt point : pointsGroup.getPoints()) {
			favoriteGroup.points.add(FavouritePoint.fromWpt(point));
		}
		if (!Algorithms.isEmpty(favoriteGroup.points)) {
			favoriteGroup.visible = favoriteGroup.points.get(0).isVisible();
		}
		return favoriteGroup;
	}

	public boolean containsPointByName(@NonNull String name) {
		if (!name.isEmpty()) {
			for (FavouritePoint p : points) {
				if (Objects.equals(name, p.getName())) {
					return true;
				}
			}
		}
		return false;
	}
}
