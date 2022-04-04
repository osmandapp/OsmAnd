package net.osmand.plus.myplaces;

import android.content.Context;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;

public class FavoriteGroup {

	public static final String PERSONAL_CATEGORY = "personal";

	private String name;
	private List<FavouritePoint> points = new ArrayList<>();

	private int color;
	private boolean visible = true;

	public FavoriteGroup() {
	}

	public FavoriteGroup(String name, boolean visible, int color) {
		this.name = name;
		this.visible = visible;
		this.color = color;
	}

	public FavoriteGroup(String name, List<FavouritePoint> points, int color, boolean visible) {
		this.name = name;
		this.color = color;
		this.points = points;
		this.visible = visible;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isPersonal() {
		return isPersonal(name);
	}

	private static boolean isPersonal(String name) {
		return PERSONAL_CATEGORY.equals(name);
	}

	public static boolean isPersonalCategoryDisplayName(Context ctx, String name) {
		return name.equals(ctx.getString(R.string.personal_category_name));
	}

	public static String getDisplayName(Context ctx, String name) {
		if (isPersonal(name)) {
			return ctx.getString(R.string.personal_category_name);
		} else if (name.isEmpty()) {
			return ctx.getString(R.string.shared_string_favorites);
		} else {
			return name;
		}
	}

	public List<FavouritePoint> getPoints() {
		return points;
	}

	public int getColor() {
		return color;
	}

	public boolean isVisible() {
		return visible;
	}

	public String getName() {
		return name;
	}

	public String getDisplayName(Context ctx) {
		return getDisplayName(ctx, name);
	}

	public static String convertDisplayNameToGroupIdName(Context context, String name) {
		if (isPersonalCategoryDisplayName(context, name)) {
			return PERSONAL_CATEGORY;
		}
		if (name.equals(context.getString(R.string.shared_string_favorites))) {
			return "";
		}
		return name;
	}
}
