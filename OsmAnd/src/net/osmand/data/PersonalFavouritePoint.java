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

		private String pointName;
		@StringRes
		private int resId;
		private int order;

		PointType(String pointName, @StringRes int resId, int order) {
			this.pointName = pointName;
			this.resId = resId;
			this.order = order;
		}

		public String getName() {
			return pointName;
		}

		public int getOrder() {
			return order;
		}

		public static PointType valueOfPointName(@NonNull String typeName){

			for (PointType pt:values()) {
				if(pt.pointName.equals(typeName))
						return pt;
			}
			throw new IllegalArgumentException("Illegal PointType pointName");
		}

		public String getHumanString(@NonNull Context ctx) {
			return ctx.getString(resId);
		}
	}

	public PersonalFavouritePoint(@NonNull Context ctx, @NonNull PointType type, double latitude, double longitude) {
		super(latitude, longitude, type.pointName, PERSONAL);
		this.ctx = ctx;
		this.type = type;
	}

	public PersonalFavouritePoint(@NonNull Context ctx, @NonNull String pointName, double latitude, double longitude) throws IllegalArgumentException {
		this(ctx, PointType.valueOfPointName(pointName), latitude, longitude);
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
		pt.name = type.pointName;
		pt.desc = getDescription();
		return pt;
	}
}
