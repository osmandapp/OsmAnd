package net.osmand.data;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.R;

public class PersonalFavouritePoint extends FavouritePoint {

	private Context ctx;

	private PointType type;
	static final String PERSONAL = "personal";

	public enum PointType {
		HOME("home", R.string.home_button, 1, R.drawable.ic_action_home_dark),
		WORK("work", R.string.work_button, 2, R.drawable.ic_action_work),
		PARKING("parking", R.string.map_widget_parking, 3, R.drawable.ic_action_parking_dark);

		private String typeName;
		@StringRes
		private int resId;
		private int order;
		@DrawableRes
		private int iconId;

		PointType(@NonNull String typeName, @StringRes int resId, int order, @DrawableRes int iconId) {
			this.typeName = typeName;
			this.resId = resId;
			this.order = order;
			this.iconId = iconId;
		}

		public String getName() {
			return typeName;
		}

		public int getOrder() {
			return order;
		}

		public static PointType valueOfTypeName(@NonNull String typeName) {
			for (PointType pt : values()) {
				if (pt.typeName.equals(typeName)) {
					return pt;
				}
			}
			throw new IllegalArgumentException("Illegal PointType typeName");
		}

		public int getIconId() {
			return iconId;
		}

		public String getHumanString(@NonNull Context ctx) {
			return ctx.getString(resId);
		}
	}

	public PersonalFavouritePoint(@NonNull Context ctx, @NonNull PointType type, double latitude, double longitude) {
		super(latitude, longitude, type.typeName, PERSONAL);
		this.ctx = ctx;
		this.type = type;
	}

	PersonalFavouritePoint(@NonNull Context ctx, @NonNull String typeName, double latitude, double longitude) throws IllegalArgumentException {
		this(ctx, PointType.valueOfTypeName(typeName), latitude, longitude);
	}

	@Override
	public PointDescription getPointDescription() {
		return new PointDescription(PointDescription.POINT_TYPE_LOCATION, getDescription());
	}

	@Override
	public boolean isPersonal() {
		return true;
	}

	public PointType getType() {
		return type;
	}

	@Override
	public String getName() {
		return type.getHumanString(ctx);
	}

	@Override
	public void setName(String name) {
		throw new IllegalArgumentException("Personal name is readonly");
	}

	@Override
	public String getCategory() {
		return ctx.getString(R.string.personal_category_name);
	}

	@Override
	public void setCategory(String category) {
		throw new IllegalArgumentException("Personal category is readonly");
	}

	@Override
	public WptPt toWpt() {
		WptPt pt = super.toWpt();
		pt.getExtensionsToWrite().put(PERSONAL, "true");
		pt.name = type.typeName;
		pt.desc = getDescription();
		return pt;
	}
}
