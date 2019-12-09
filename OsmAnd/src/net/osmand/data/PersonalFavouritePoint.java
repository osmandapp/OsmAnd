package net.osmand.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.R;

public class PersonalFavouritePoint extends FavouritePoint {

	private Context ctx;

	private PointType type;
	public static final String PERSONAL = "personal";

	public enum PointType {
		HOME("home", R.string.home_button, 1),
		WORK("work", R.string.work_button, 2),
		PARKING("parking", R.string.map_widget_parking, 3);

		private String name;
		@StringRes
		private int resId;
		private int order;

		PointType(String name, @StringRes int resId, int order) {
			this.name = name;
			this.resId = resId;
			this.order = order;
		}

		public String getName() {
			return name;
		}

		public int getOrder() {
			return order;
		}

		public String getHumanString(@NonNull Context ctx) {
			return ctx.getString(resId);
		}
	}

	private PersonalFavouritePoint() {
	}

	private PersonalFavouritePoint(@NonNull Context ctx, @NonNull PointType type, double latitude, double longitude) {
		super(latitude, longitude, type.name, PERSONAL);
		this.ctx = ctx;
		this.type = type;
	}

	public PersonalFavouritePoint(@NonNull Context ctx, @NonNull String typeName, double latitude, double longitude) throws IllegalArgumentException {
		this(ctx, PointType.valueOf(typeName), latitude, longitude);
	}

	@Override
	public PointDescription getPointDescription() {
		return new PointDescription(PointDescription.POINT_TYPE_LOCATION, getDescription());
	}

	public PersonalFavouritePoint(PersonalFavouritePoint favouritePoint) {
		super(favouritePoint);
		this.type = favouritePoint.type;
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
		pt.name = type.toString();
		pt.desc = getDescription();
		return pt;
	}
}
